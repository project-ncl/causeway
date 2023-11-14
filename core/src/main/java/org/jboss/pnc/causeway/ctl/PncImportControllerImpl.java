/**
 * Copyright (C) 2015 Red Hat, Inc. (jbrazdil@redhat.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.ctl;

import static org.jboss.pnc.causeway.ctl.ImportControllerImpl.METRICS_ARTIFACTS_NUMBER_KEY;
import static org.jboss.pnc.causeway.ctl.ImportControllerImpl.METRICS_ARTIFACTS_SIZE_KEY;
import static org.jboss.pnc.causeway.ctl.ImportControllerImpl.METRICS_LOGS_NUMBER_KEY;
import static org.jboss.pnc.causeway.ctl.ImportControllerImpl.METRICS_LOGS_SIZE_KEY;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import com.redhat.red.build.koji.model.json.KojiImport;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;

import org.jboss.pnc.api.constants.MDCKeys;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.ErrorMessages;
import org.jboss.pnc.causeway.bpmclient.BPMClient;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BuildTranslator;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.source.RenamedSources;
import org.jboss.pnc.causeway.source.SourceRenamer;
import org.jboss.pnc.common.otel.OtelUtils;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.pncmetrics.MetricsConfiguration;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.pnc.BuildImportResultRest;
import org.jboss.pnc.causeway.rest.pnc.BuildImportStatus;
import org.jboss.pnc.causeway.rest.pnc.MilestoneReleaseResultRest;
import org.jboss.pnc.causeway.rest.pnc.ReleaseStatus;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.ArtifactQuality;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.jboss.pnc.causeway.CausewayFailure;

import static org.jboss.pnc.constants.Attributes.BUILD_BREW_NAME;
import static org.jboss.pnc.constants.Attributes.BUILD_BREW_VERSION;

import org.slf4j.MDC;

@Deprecated
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Slf4j
public class PncImportControllerImpl implements PncImportController {

    private static final String METRICS_BASE = "causeway.import.milestone";
    private static final String METRICS_TIMER = ".timer";
    private static final String METRICS_METER = ".meter";
    private static final String METRICS_ERRORS = ".errors";

    private final PncClient pncClient;
    private final BrewClient brewClient;
    private final BPMClient bpmClient;
    private final BuildTranslator translator;
    private final CausewayConfig config;
    private final SourceRenamer renamer;

    private final MetricsConfiguration metricsConfiguration;

    @Inject
    public PncImportControllerImpl(
            PncClient pnclClient,
            BrewClient brewClient,
            BPMClient bpmClient,
            BuildTranslator translator,
            CausewayConfig config,
            SourceRenamer renamer,
            MetricsConfiguration metricConfiguration) {
        this.pncClient = pnclClient;
        this.brewClient = brewClient;
        this.bpmClient = bpmClient;
        this.translator = translator;
        this.config = config;
        this.renamer = renamer;
        this.metricsConfiguration = metricConfiguration;
    }

    @Override
    @Asynchronous
    public void importMilestone(
            int milestoneId,
            Request positiveCallback,
            Request negativeCallback,
            Request callback,
            String callbackId,
            String username) {
        log.info("Importing PNC milestone {}.", milestoneId);

        // Create a parent child span with values from MDC
        SpanBuilder spanBuilder = OtelUtils.buildChildSpan(
                GlobalOpenTelemetry.get().getTracer(""),
                "PncImportControllerImpl.importMilestone",
                SpanKind.CLIENT,
                MDC.get(MDCKeys.SLF4J_TRACE_ID_KEY),
                MDC.get(MDCKeys.SLF4J_SPAN_ID_KEY),
                MDC.get(MDCKeys.SLF4J_TRACE_FLAGS_KEY),
                MDC.get(MDCKeys.SLF4J_TRACE_STATE_KEY),
                Span.current().getSpanContext(),
                Map.of("milestoneId", String.valueOf(milestoneId), "callbackId", callbackId, "username", username));
        Span span = spanBuilder.startSpan();
        log.debug("Started a new span :{}", span);

        MetricRegistry registry = metricsConfiguration.getMetricRegistry();
        Meter meter = registry.meter(METRICS_BASE + METRICS_METER);
        meter.mark();

        Timer timer = registry.timer(METRICS_BASE + METRICS_TIMER);
        Timer.Context context = timer.time();

        Meter errors = registry.meter(METRICS_BASE + METRICS_ERRORS);

        Request successCallback = positiveCallback != null ? positiveCallback : callback;
        Request failedCallback = negativeCallback != null ? negativeCallback : callback;

        // put the span into the current Context
        try (Scope scope = span.makeCurrent()) {
            MilestoneReleaseResultRest result = new MilestoneReleaseResultRest();
            result.setMilestoneId(milestoneId);
            try {
                List<BuildImportResultRest> results = importProductMilestone(milestoneId, username);
                result.setBuilds(results);

                if (results.stream().anyMatch(r -> r.getStatus() == BuildImportStatus.ERROR)) {
                    result.setReleaseStatus(ReleaseStatus.SET_UP_ERROR);
                    bpmClient.failure(failedCallback, callbackId, result);
                    errors.mark();
                } else if (results.stream().anyMatch(r -> r.getStatus() == BuildImportStatus.FAILED)) {
                    result.setReleaseStatus(ReleaseStatus.IMPORT_ERROR);
                    bpmClient.failure(failedCallback, callbackId, result);
                    errors.mark();
                } else {
                    result.setReleaseStatus(ReleaseStatus.SUCCESS);
                    bpmClient.success(successCallback, callbackId, result);
                }
            } catch (CausewayFailure ex) {
                log.warn(ErrorMessages.failedToImportMilestone(milestoneId, ex), ex);
                result.setErrorMessage(ex.getMessage());
                result.setReleaseStatus(ReleaseStatus.FAILURE);
                bpmClient.failure(failedCallback, callbackId, result);
                errors.mark();
            } catch (CausewayException | RuntimeException ex) {
                log.error(ErrorMessages.errorImportingMilestone(milestoneId, ex), ex);
                result.setErrorMessage(ex.getMessage());
                result.setReleaseStatus(ReleaseStatus.SET_UP_ERROR);
                bpmClient.error(failedCallback, callbackId, result);
                errors.mark();
            }

            // stop the timer
            context.stop();
        } finally {
            span.end(); // closing the scope does not end the span, this has to be done manually
        }
    }

    private List<BuildImportResultRest> importProductMilestone(int milestoneId, String username)
            throws CausewayException {
        String tagPrefix = pncClient.getTagForMilestone(milestoneId);
        if (!brewClient.tagsExists(tagPrefix)) {
            throw new CausewayFailure(ErrorMessages.tagsAreMissingInKoji(tagPrefix, config.getKojiURL()));
        }

        Collection<Build> builds = findAndAssertBuilds(milestoneId);

        List<BuildImportResultRest> results = new ArrayList<>();
        for (Build build : builds) {
            BuildImportResultRest importResult;
            try (MDC.MDCCloseable mdcClose = MDC.putCloseable(MDCKeys.BUILD_ID_KEY, build.getId())) {
                BuildArtifacts artifacts = pncClient.findBuildArtifacts(build.getId());
                importResult = importBuild(build, username, artifacts);
                if (importResult.getStatus() == BuildImportStatus.SUCCESSFUL && importResult.getBrewBuildId() != null) {
                    brewClient.tagBuild(
                            tagPrefix,
                            new BrewBuild(importResult.getBrewBuildId(), getNVR(build, artifacts)));
                }
            } catch (CausewayException ex) {
                importResult = new BuildImportResultRest();
                if (ex instanceof CausewayFailure) {
                    log.warn(ErrorMessages.failedToImportBuild(build.getId(), (CausewayFailure) ex));
                    importResult.setStatus(BuildImportStatus.FAILED);
                } else {
                    log.error(ErrorMessages.errorImportingBuild(build.getId(), ex));
                    importResult.setStatus(BuildImportStatus.ERROR);
                }
                importResult.setBuildRecordId(build.getId());
                importResult.setErrorMessage(ex.getMessage());
            }
            results.add(importResult);
        }

        return results;
    }

    private Collection<Build> findAndAssertBuilds(int milestoneId) throws CausewayException {
        Collection<Build> builds = pncClient.findBuildsOfProductMilestone(milestoneId);
        if (builds == null || builds.isEmpty()) {
            throw new CausewayFailure(ErrorMessages.messageMilestoneWithoutBuilds(milestoneId));
        }
        return builds;
    }

    private BuildImportResultRest importBuild(Build build, String username, BuildArtifacts artifacts)
            throws CausewayException {
        BrewNVR nvr = getNVR(build, artifacts);
        log.info("Processing PNC build {} as {}.", build.getId(), nvr.getNVR());
        BrewBuild brewBuild = brewClient.findBrewBuildOfNVR(nvr);
        if (brewBuild != null) {
            // FIXME clarify behavior - if the build already exists in brew log as successful import ?
            BuildImportResultRest ret = new BuildImportResultRest();
            ret.setBrewBuildId(brewBuild.getId());
            ret.setBrewBuildUrl(brewClient.getBuildUrl(brewBuild.getId()));
            ret.setBuildRecordId(build.getId());
            ret.setStatus(BuildImportStatus.SUCCESSFUL); // TODO: replace with EXISTING?

            log.info("Build {} was already imported with id {}.", nvr.getNVR(), brewBuild.getId());
            return ret;
        }

        List<BuildArtifacts.PncArtifact> badArtifacts = new ArrayList<>();
        for (Iterator<BuildArtifacts.PncArtifact> it = artifacts.buildArtifacts.iterator(); it.hasNext();) {
            BuildArtifacts.PncArtifact artifact = it.next();
            if (artifact.artifactQuality == ArtifactQuality.BLACKLISTED
                    || artifact.artifactQuality == ArtifactQuality.DELETED) {
                badArtifacts.add(artifact);
                it.remove();
            }
        }

        final BuildImportResultRest buildResult;
        if (artifacts.buildArtifacts.isEmpty()) {
            buildResult = new BuildImportResultRest();
            buildResult.setBuildRecordId(build.getId());
            buildResult.setStatus(BuildImportStatus.SUCCESSFUL);
            buildResult.setErrorMessage("Build doesn't contain any artifacts to import, skipping.");
            log.info("PNC build {} doesn't contain any artifacts to import, skipping.", build.getId());
        } else {
            String buildLog = pncClient.getBuildLog(build.getId());

            BuildType buildType = build.getBuildConfigRevision().getBuildType();
            String sourcesDeployPath = getSourcesDeployPath(build, artifacts);

            Optional<BuildArtifacts.PncArtifact> any = artifacts.buildArtifacts.stream()
                    .filter(a -> a.deployPath.equals(sourcesDeployPath))
                    .findAny();

            RenamedSources sources = null;
            if (!any.isPresent()) {
                log.info("Sources at '{}' not present, generating sources file.", sourcesDeployPath);
                try (InputStream sourcesStream = pncClient.getSources(build.getId())) {
                    sources = translator.getSources(build, artifacts, sourcesStream);
                } catch (IOException ex) {
                    throw new CausewayException(ErrorMessages.failedToDownloadSources(ex), ex);
                }
            }
            KojiImport kojiImport = translator.translate(nvr, build, artifacts, sources, buildLog, username);
            ImportFileGenerator importFiles = translator.getImportFiles(artifacts, sources, buildLog);
            buildResult = brewClient.importBuild(nvr, build.getId(), kojiImport, importFiles);

            long artifactSize = artifacts.buildArtifacts.stream().mapToLong(pncArtifact -> pncArtifact.size).sum();
            int artifactNumber = artifacts.buildArtifacts.size();
            int logLenght = buildLog.length();
            try {
                logLenght = buildLog.getBytes("UTF-8").length;
            } catch (UnsupportedEncodingException e) {
            }

            updateHistogram(metricsConfiguration, METRICS_ARTIFACTS_SIZE_KEY, artifactSize);
            updateHistogram(metricsConfiguration, METRICS_ARTIFACTS_NUMBER_KEY, artifactNumber);
            updateHistogram(metricsConfiguration, METRICS_LOGS_SIZE_KEY, logLenght);
            updateHistogram(metricsConfiguration, METRICS_LOGS_NUMBER_KEY, 1);
        }

        for (BuildArtifacts.PncArtifact artifact : badArtifacts) {
            log.warn(ErrorMessages.badArtifactNotImported(artifact.id));
        }
        return buildResult;
    }

    private String getSourcesDeployPath(Build build, BuildArtifacts artifacts) throws CausewayException {
        String sourcesDeployPath = translator.getSourcesDeployPath(build, artifacts);
        if (sourcesDeployPath.startsWith("/")) {
            sourcesDeployPath = sourcesDeployPath.substring(1); // PncClientImpl.toPncArtifact strips leading "/"
        }
        return sourcesDeployPath;
    }

    private void updateHistogram(MetricsConfiguration metricsConfiguration, String name, long value) {
        Histogram histogram = null;
        if (metricsConfiguration != null) {
            MetricRegistry registry = metricsConfiguration.getMetricRegistry();
            try {
                histogram = registry.register(name, new Histogram(new UniformReservoir()));
            } catch (IllegalArgumentException e) {
                histogram = registry.histogram(name);
            }
        }
        if (histogram != null) {
            histogram.update(value);
        }
    }

    BrewNVR getNVR(Build build, BuildArtifacts artifacts) throws CausewayException {
        if (!build.getAttributes().containsKey(BUILD_BREW_NAME)) {
            throw new CausewayFailure(ErrorMessages.missingBrewNameAttributeInBuild());
        }
        String version = build.getAttributes().get(BUILD_BREW_VERSION);
        if (version == null) {
            version = BuildTranslator.guessVersion(build, artifacts);
        }
        return new BrewNVR(build.getAttributes().get(BUILD_BREW_NAME), version, "1");
    }

}
