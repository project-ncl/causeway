/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import static org.jboss.pnc.api.constants.Attributes.BUILD_BREW_NAME;
import static org.jboss.pnc.api.constants.Attributes.BUILD_BREW_VERSION;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;
import org.jboss.pnc.causeway.CausewayConfig;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.CausewayFailure;
import org.jboss.pnc.causeway.ErrorMessages;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.source.RenamedSources;
import org.jboss.pnc.causeway.source.SourceRenamer;
import org.jboss.pnc.dto.ArtifactRef;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.restclient.util.ArtifactUtil;

import com.github.zafarkhaja.semver.Version;
import com.redhat.red.build.koji.model.json.BuildContainer;
import com.redhat.red.build.koji.model.json.BuildDescription;
import com.redhat.red.build.koji.model.json.BuildOutput;
import com.redhat.red.build.koji.model.json.BuildRoot;
import com.redhat.red.build.koji.model.json.BuildTool;
import com.redhat.red.build.koji.model.json.FileBuildComponent;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.StandardArchitecture;
import com.redhat.red.build.koji.model.json.StandardOutputType;
import com.redhat.red.build.koji.model.json.VerificationException;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@ApplicationScoped
public class BuildTranslatorImpl implements BuildTranslator {
    private static final String CONTENT_GENERATOR_NAME = "Project Newcastle";
    public static final String PNC = "PNC";
    private static final String MD5 = "md5";

    @Inject
    CausewayConfig config;

    @Inject
    SourceRenamer renamer;

    @Override
    public KojiImport translate(
            BrewNVR nvr,
            org.jboss.pnc.dto.Build build,
            BuildArtifacts artifacts,
            RenamedSources sources,
            BurnAfterReadingFile buildLog,
            BurnAfterReadingFile alignLlog,
            String username) throws CausewayException {
        String externalBuildId = String.valueOf(build.getId());
        String externalBuildUrl = null;
        String externalBuildsUrl = config.pnc().buildsURL();
        if (externalBuildsUrl != null) {
            externalBuildUrl = externalBuildsUrl + externalBuildId;
        }
        KojiImport.Builder builder = new KojiImport.Builder();
        BuildDescription.Builder descriptionBuilder = builder
                .withNewBuildDescription(nvr.getKojiName(), nvr.getVersion(), nvr.getRelease())
                .withStartTime(Date.from(build.getStartTime()))
                .withEndTime(Date.from(build.getEndTime()))
                .withBuildSource(normalizeScmUrl(build.getScmUrl()), build.getScmRevision())
                .withExternalBuildId(externalBuildId)
                .withExternalBuildUrl(externalBuildUrl)
                .withBuildSystem(PNC);
        setBuildType(descriptionBuilder, build, artifacts);
        if (build.getScmTag() != null) {
            descriptionBuilder.withSCMTag(build.getScmTag());
        }

        int buildRootId = 42;
        BuildRoot.Builder buildRootBuilder = builder.withNewBuildRoot(buildRootId)
                .withContentGenerator(CONTENT_GENERATOR_NAME, config.pnc().systemVersion())
                .withContainer(getContainer(build))
                .withHost(build.getEnvironment().getAttributes().get("OS"), StandardArchitecture.noarch);

        Map<String, String> tools = new HashMap<>(build.getEnvironment().getAttributes());
        addTool(buildRootBuilder, build.getBuildConfigRevision().getBuildType(), tools, build.getEnvironment().getId());
        addTools(buildRootBuilder, tools);
        addDependencies(artifacts.getDependencies(), buildRootBuilder, build.getBuildConfigRevision().getBuildType());
        addBuiltArtifacts(
                artifacts.getBuildArtifacts(),
                builder,
                buildRootId,
                build.getBuildConfigRevision().getBuildType());
        addLog(buildLog, builder, buildRootId);
        addLog(alignLlog, builder, buildRootId);
        addSources(sources, builder, buildRootId);

        KojiImport translatedBuild = buildTranslatedBuild(builder);
        translatedBuild.getBuild().getExtraInfo().setImportInitiator(username);
        return translatedBuild;
    }

    private String normalizeScmUrl(final String url) {
        if (url.startsWith("http")) {
            return "git+" + url;
        }
        return url;
    }

    private void addLog(BurnAfterReadingFile log, KojiImport.Builder builder, int buildRootId) {
        builder.withNewOutput(buildRootId, log.getName())
                .withOutputType(StandardOutputType.log)
                .withFileSize(log.getSize())
                .withArch(StandardArchitecture.noarch)
                .withChecksum(MD5, log.getMd5());
    }

    private void addSources(RenamedSources sources, KojiImport.Builder builder, int buildRootId) {
        if (sources != null) {
            BuildOutput.Builder outputBuilder = builder.withNewOutput(buildRootId, sources.getName())
                    .withFileSize(sources.getSize())
                    .withArch(StandardArchitecture.noarch)
                    .withChecksum(MD5, sources.getMd5());
            RenamedSources.ArtifactType artifactType = sources.getArtifactType();
            if (artifactType.isMavenType()) {
                outputBuilder.withMavenInfoAndType(artifactType.getMavenInfoAndType());
            } else if (artifactType.isNPMType()) {
                outputBuilder.withNpmInfoAndType(artifactType.getNpmInfoAndType());
            } else {
                throw new IllegalArgumentException(ErrorMessages.unknownArtifactType());
            }
        }
    }

    private void addDependencies(
            List<ArtifactRef> dependencies,
            BuildRoot.Builder buildRootBuilder,
            BuildType buildType) throws CausewayException {
        for (ArtifactRef artifact : dependencies) {
            FileBuildComponent.Builder componentBuilder = buildRootBuilder
                    .withFileComponent(stripLeadingSlash(artifact.getDeployPath()));
            componentBuilder.withChecksum(MD5, artifact.getMd5());

            switch (buildType) {
                case GRADLE:
                case NPM:
                case SBT:
                case MVN: {
                    componentBuilder.withFileSize(artifact.getSize());
                    break;
                }
                default: {
                    throw new IllegalArgumentException(ErrorMessages.unknownArtifactType());
                }
            }
        }
    }

    public static String stripLeadingSlash(String deployPath) {
        if (deployPath.startsWith("/"))
            return deployPath.substring(1);
        return deployPath;
    }

    private void addBuiltArtifacts(
            List<ArtifactRef> buildArtifacts,
            KojiImport.Builder builder,
            int buildRootId,
            BuildType buildType) throws CausewayException {
        for (ArtifactRef artifact : buildArtifacts) {
            BuildOutput.Builder outputBuilder = builder
                    .withNewOutput(buildRootId, stripLeadingSlash(artifact.getDeployPath()))
                    .withArch(StandardArchitecture.noarch)
                    .withChecksum(MD5, artifact.getMd5());

            switch (buildType) {
                case GRADLE:
                case SBT:
                case MVN: {
                    SimpleArtifactRef ref = ArtifactUtil.parseMavenCoordinates(artifact);
                    outputBuilder.withFileSize(artifact.getSize());
                    outputBuilder.withMavenInfoAndType(ref);
                    break;
                }
                case NPM: {
                    NpmPackageRef ref = ArtifactUtil.parseNPMCoordinates(artifact);
                    outputBuilder.withFileSize(artifact.getSize());
                    outputBuilder.withNpmInfoAndType(ref);
                    break;
                }
                default: {
                    throw new IllegalArgumentException(ErrorMessages.unknownArtifactType());
                }
            }
        }
    }

    private BuildContainer getContainer(org.jboss.pnc.dto.Build buildRecord) {
        return switch (buildRecord.getEnvironment().getSystemImageType()) {
            case DOCKER_IMAGE -> new BuildContainer("docker", "noarch");
            default -> throw new IllegalArgumentException(ErrorMessages.unknownSystemImageType());
        };
    }

    @Override
    public ImportFileGenerator getImportFiles(
            BuildArtifacts artifacts,
            BurnAfterReadingFile sources,
            BurnAfterReadingFile buildLog,
            BurnAfterReadingFile alignLog) throws CausewayException {
        try {
            ImportFileGenerator ret = new ImportFileGenerator(sources, buildLog, alignLog);
            for (ArtifactRef artifact : artifacts.getBuildArtifacts()) {
                ret.addUrl(
                        artifact.getId(),
                        artifact.getDeployUrl(),
                        stripLeadingSlash(artifact.getDeployPath()),
                        artifact.getSize());
            }
            return ret;
        } catch (MalformedURLException ex) {
            throw new CausewayException(ErrorMessages.failedToParseArtifactURL(ex), ex);
        }
    }

    @Override
    public RenamedSources getSources(org.jboss.pnc.dto.Build build, BuildArtifacts artifacts, InputStream sources)
            throws CausewayException {
        return buildTypeSwitch(
                build,
                artifacts,
                (gav) -> renamer.repackMaven(sources, gav.getGroupId(), gav.getArtifactId(), gav.getVersionString()),
                (npmPackage) -> renamer
                        .repackNPM(sources, npmPackage.getName(), npmPackage.getVersion().getNormalVersion()));
    }

    @Override
    public String getSourcesDeployPath(org.jboss.pnc.dto.Build build, BuildArtifacts artifacts)
            throws CausewayException {
        return buildTypeSwitch(
                build,
                artifacts,
                (gav) -> renamer.getMavenDeployPath(gav.getGroupId(), gav.getArtifactId(), gav.getVersionString()),
                (npmPackage) -> renamer
                        .getNPMDeployPath(npmPackage.getName(), npmPackage.getVersion().getNormalVersion()));
    }

    private KojiImport buildTranslatedBuild(KojiImport.Builder builder) throws CausewayException {
        final KojiImport translatedBuild;
        try {
            translatedBuild = builder.build();
        } catch (VerificationException ex) {
            throw new CausewayException(ErrorMessages.failureWhileBuildingKojiImport(ex), ex);
        }
        return translatedBuild;
    }

    private ProjectVersionRef buildRootToGAV(org.jboss.pnc.dto.Build build, BuildArtifacts artifacts)
            throws CausewayException {
        String brewName = build.getAttributes().get(BUILD_BREW_NAME);
        if (brewName == null) {
            throw new CausewayFailure(ErrorMessages.missingBrewNameAttributeInMavenBuild());
        }
        String[] splittedName = brewName.split(":");
        if (splittedName.length != 2) {
            throw new IllegalArgumentException(ErrorMessages.illegalMavenBrewName(brewName));
        }
        String version = build.getAttributes().get(BUILD_BREW_VERSION);
        if (version == null) {
            version = BuildTranslator.guessVersion(build, artifacts);
        }
        return new SimpleProjectVersionRef(splittedName[0], splittedName[1], version);
    }

    private NpmPackageRef buildRootToNV(org.jboss.pnc.dto.Build build, BuildArtifacts artifacts)
            throws CausewayException {
        if (!build.getAttributes().containsKey(BUILD_BREW_NAME)) {
            throw new CausewayFailure(ErrorMessages.missingBrewNameAttributeInBuild());
        }
        String name = build.getAttributes().get(BUILD_BREW_NAME);
        String version = build.getAttributes().get(BUILD_BREW_VERSION);
        if (version == null) {
            version = BuildTranslator.guessVersion(build, artifacts);
        }
        return new NpmPackageRef(name, Version.valueOf(version));
    }

    private void addTools(BuildRoot.Builder buildRootBuilder, Map<String, String> tools) {
        for (Map.Entry<String, String> e : tools.entrySet()) {
            if (!config.pnc().ignoredTools().contains(e.getKey())) {
                buildRootBuilder.withTool(e.getKey(), e.getValue());
            }
        }
    }

    private void setBuildType(
            BuildDescription.Builder buildDescription,
            org.jboss.pnc.dto.Build build,
            BuildArtifacts artifacts) throws CausewayException {
        buildTypeSwitch(build, artifacts, buildDescription::withMavenInfoAndType, buildDescription::withNpmInfoAndType);
    }

    private void addTool(
            BuildRoot.Builder buildRootBuilder,
            BuildType buildType,
            Map<String, String> tools,
            String envID) throws CausewayException {
        switch (buildType) {
            case MVN:
                buildRootBuilder.withTool(getTool("JDK", tools, envID));
                buildRootBuilder.withTool(getTool("MAVEN", tools, envID));
                break;
            case SBT:
                buildRootBuilder.withTool(getTool("SBT", tools, envID));
                break;
            case GRADLE:
                buildRootBuilder.withTool(getTool("GRADLE", tools, envID));
                break;
            case NPM:
                buildRootBuilder.withTool(getTool("NPM", tools, envID));
                break;
            default:
                throw new IllegalArgumentException(ErrorMessages.unsupportedBuildType(buildType));
        }
    }

    private BuildTool getTool(String name, Map<String, String> tools, String envID) {
        String version = tools.remove(name);
        if (version == null) {
            for (Iterator<Map.Entry<String, String>> it = tools.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, String> e = it.next();
                if (e.getKey().equalsIgnoreCase(name)) {
                    if (version != null) {
                        throw new IllegalArgumentException(ErrorMessages.environmentWithMultipleVersions(envID, name));
                    }
                    version = e.getValue();
                    it.remove();
                }
            }
            if (version == null) {
                throw new IllegalArgumentException(ErrorMessages.environmentWithoutVersion(envID, name));
            }
        }
        return new BuildTool(name, version);
    }

    private <T> T buildTypeSwitch(
            org.jboss.pnc.dto.Build build,
            BuildArtifacts artifacts,
            CausewayFunction<ProjectVersionRef, T> mavenConsumer,
            CausewayFunction<NpmPackageRef, T> npmConsumer) throws CausewayException {
        BuildType buildType = build.getBuildConfigRevision().getBuildType();
        return switch (buildType) {
            case MVN, SBT, GRADLE -> {
                ProjectVersionRef gav = buildRootToGAV(build, artifacts);
                yield mavenConsumer.apply(gav);
            }
            case NPM -> {
                NpmPackageRef npmPackage = buildRootToNV(build, artifacts);
                yield npmConsumer.apply(npmPackage);
            }
        };
    }

    @FunctionalInterface
    private interface CausewayFunction<T, R> {
        R apply(T o) throws CausewayException;
    }
}
