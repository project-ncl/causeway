/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.ctl;

import static org.jboss.pnc.api.constants.Attributes.BUILD_BREW_NAME;
import static org.jboss.pnc.api.constants.Attributes.BUILD_BREW_VERSION;
import static org.jboss.pnc.causeway.impl.Meters.METRICS_IMPORT;
import static org.jboss.pnc.causeway.impl.Meters.METRICS_UNTAG;
import static org.jboss.pnc.enums.ArtifactQuality.BLACKLISTED;
import static org.jboss.pnc.enums.ArtifactQuality.DELETED;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.pnc.api.causeway.dto.push.PushResult;
import org.jboss.pnc.api.constants.MDCKeys;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.causeway.CausewayConfig;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.CausewayFailure;
import org.jboss.pnc.causeway.ErrorMessages;
import org.jboss.pnc.causeway.brewclient.BrewBuild;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BrewNVR;
import org.jboss.pnc.causeway.brewclient.BuildTranslator;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;
import org.jboss.pnc.causeway.impl.Meters;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.source.RenamedSources;
import org.jboss.pnc.dto.ArtifactRef;
import org.jboss.pnc.dto.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.xmlrpc.KojiNVRA;

import io.micrometer.core.annotation.Timed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ImportControllerImpl implements ImportController {
    private static final Logger userLog = LoggerFactory.getLogger("org.jboss.pnc._userlog_.brew-push");
    private static final String BUILD_NOT_TAGGED = " but not previously tagged. Tagging now.";
    private static final String BUILD_ALREADY_IMPORTED = "Build was already imported with id ";
    @Inject
    BrewClient brewClient;

    @Inject
    PncClient pncClient;

    @Inject
    CausewayConfig config;

    @Inject
    BuildTranslator translator;

    @Inject
    Meters meters;

    @Timed(value = METRICS_IMPORT)
    @WithSpan(kind = SpanKind.CLIENT)
    public PushResult importBuild(
            @SpanAttribute(value = MDCKeys.BUILD_ID_KEY) String buildId,
            @SpanAttribute(value = "tag") String tagPrefix,
            @SpanAttribute(value = "reimport") boolean reimport,
            @SpanAttribute(value = "username") String username) throws CausewayException {

        Build build = pncClient.findBuild(buildId);
        BuildArtifacts artifacts = filterArtifacts(pncClient.findBuildArtifacts(buildId));
        if (artifacts.getBuildArtifacts().isEmpty()) {
            userLog.info("PNC build {} doesn't contain any artifacts to import, skipping.", build.getId());
            return PushResult.builder().buildId(build.getId()).result(ResultStatus.SUCCESS).build();
        }

        if (!brewClient.tagsExists(tagPrefix)) {
            throw new CausewayFailure(ErrorMessages.tagsAreMissingInKoji(tagPrefix, config.koji().url()));
        }

        BrewBuild brewBuild = checkAndImport(tagPrefix, reimport, username, build, artifacts);
        brewClient.tagBuild(tagPrefix, brewBuild);

        log.debug(brewClient.getBuildUrl(brewBuild.getId()));
        return PushResult.builder()
                .buildId(buildId)
                .brewBuildId(brewBuild.getId())
                .brewBuildUrl(brewClient.getBuildUrl(brewBuild.getId()))
                .result(ResultStatus.SUCCESS)
                .build();
    }

    private BrewBuild checkAndImport(
            String tagPrefix,
            boolean reimport,
            String username,
            Build build,
            BuildArtifacts artifacts) {
        BrewNVR nvr = getNVR(build, artifacts);
        userLog.info("Processing PNC build {} as {}.", build.getId(), nvr.getNVR());
        BrewBuild brewBuild = brewClient.findBrewBuildOfNVR(nvr);

        if (brewBuild == null) {
            brewBuild = translateAndImport(build, artifacts, nvr, username);
            userLog.info("Build imported with id " + brewBuild.getId() + ".");
        } else {
            if (reimport) {
                int revision = 1;
                while (brewBuild != null && brewClient.isBuildTagged(tagPrefix, brewBuild)) {
                    nvr = getNVR(nvr, ++revision);
                    brewBuild = brewClient.findBrewBuildOfNVR(nvr);
                }
                if (brewBuild == null) {
                    brewBuild = translateAndImport(build, artifacts, nvr, username);
                    userLog.info(
                            "Build was previously imported. Re-imported again with revision " + revision
                                    + " and with id " + brewBuild.getId() + ".");
                } else {
                    userLog.info(BUILD_ALREADY_IMPORTED + brewBuild.getId() + BUILD_NOT_TAGGED);
                }
            } else {
                String message = BUILD_ALREADY_IMPORTED + brewBuild.getId();
                if (!brewClient.isBuildTagged(tagPrefix, brewBuild)) {
                    message += BUILD_NOT_TAGGED;
                }
                userLog.info(message);
            }
        }
        return brewBuild;
    }

    private BuildArtifacts filterArtifacts(BuildArtifacts artifacts) {
        for (Iterator<ArtifactRef> it = artifacts.getBuildArtifacts().iterator(); it.hasNext();) {
            ArtifactRef artifact = it.next();
            if (artifact.getArtifactQuality() == BLACKLISTED || artifact.getArtifactQuality() == DELETED) {
                it.remove();
                userLog.warn(ErrorMessages.badArtifactNotImported(artifact.getId()));
            }
        }
        return artifacts;
    }

    private BrewBuild translateAndImport(Build build, BuildArtifacts artifacts, BrewNVR nvr, String username) {
        BurnAfterReadingFile buildLog = pncClient.getBuildLog(build.getId());
        BurnAfterReadingFile alignLog = pncClient.getAlignLog(build.getId());

        RenamedSources sources = getSources(build, artifacts);
        KojiImport kojiImport = translator.translate(nvr, build, artifacts, sources, buildLog, alignLog, username);
        ImportFileGenerator importFiles = translator.getImportFiles(artifacts, sources, buildLog, alignLog);
        BrewBuild brewBuild = brewClient.importBuild(nvr, kojiImport, importFiles);

        long artifactSize = artifacts.getBuildArtifacts().stream().mapToLong(ArtifactRef::getSize).sum();
        meters.recordArtifactsSize(artifactSize);
        meters.recordArtifactsNumber(artifacts.getBuildArtifacts().size());
        meters.recordLogsSize(buildLog.getSize() + alignLog.getSize());
        meters.recordLogsNumber(2);

        return brewBuild;
    }

    RenamedSources getSources(Build build, BuildArtifacts artifacts) {
        String sourcesDeployPath = translator.getSourcesDeployPath(build, artifacts);
        Optional<ArtifactRef> sourceJar = artifacts.getBuildArtifacts()
                .stream()
                .filter(a -> a.getDeployPath().equals(sourcesDeployPath))
                .findAny();
        if (sourceJar.isEmpty()) {
            userLog.info("Sources at '{}' not present, generating sources file.", sourcesDeployPath);
            try (InputStream sourcesStream = pncClient.getSources(build.getId())) {
                return translator.getSources(build, artifacts, sourcesStream);
            } catch (IOException ex) {
                userLog.error("Unable to download sources with buildId " + build.getId(), ex);
                throw new CausewayException(ErrorMessages.failedToDownloadSources(ex), ex);
            }
        }
        return null;
    }

    @Override
    @Timed(value = METRICS_UNTAG)
    @WithSpan(kind = SpanKind.CLIENT)
    public void untagBuild(@SpanAttribute("build") int brewBuildId, @SpanAttribute("tag") String tagPrefix) {
        BrewBuild build = brewClient.findBrewBuild(brewBuildId);
        if (build == null) {
            throw new CausewayFailure(ErrorMessages.brewBuildNotFound(brewBuildId));
        }
        brewClient.untagBuild(tagPrefix, build);
    }

    BrewNVR getNVR(Build build, BuildArtifacts artifacts) throws CausewayException {
        return switch (build.getBuildConfigRevision().getBuildType()) {
            case MVN_RPM -> {
                // If we are importing an RPM then extract the NVR from the RPM name rather than the
                // Maven GAV.
                ArtifactRef artifactRef = artifacts.getBuildArtifacts()
                        .stream()
                        .filter(a -> a.getFilename().endsWith(".rpm"))
                        .findAny()
                        .orElseThrow(() -> new CausewayFailure("Unable to find RPM to derive NVR from"));
                // The 'arch' (e.g. noarch/src) doesn't matter for extracting the NVR.
                try {
                    KojiNVRA nvra = KojiNVRA.parseNVRA(artifactRef.getFilename());
                    yield new BrewNVR(nvra.getName(), nvra.getVersion(), nvra.getRelease());
                } catch (KojiClientException e) {
                    throw new CausewayFailure(e.toString());
                }
            }
            case GRADLE, SBT, MVN, NPM -> {
                if (!build.getAttributes().containsKey(BUILD_BREW_NAME)) {
                    throw new CausewayFailure(ErrorMessages.missingBrewNameAttributeInBuild());
                }
                String version = build.getAttributes().get(BUILD_BREW_VERSION);
                if (version == null) {
                    version = BuildTranslator.guessVersion(build, artifacts);
                }
                yield new BrewNVR(build.getAttributes().get(BUILD_BREW_NAME), version, "1");
            }
        };
    }

    BrewNVR getNVR(BrewNVR nvr, int revision) {
        return new BrewNVR(nvr.getName(), nvr.getVersion(), Integer.toString(revision));
    }
}
