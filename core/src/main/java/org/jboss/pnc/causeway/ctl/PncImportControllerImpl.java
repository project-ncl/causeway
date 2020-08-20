/**
 * Copyright (C) 2015 Red Hat, Inc. (jbrazdil@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
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
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.bpmclient.BPMClient;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BrewClientImpl;
import org.jboss.pnc.causeway.brewclient.BuildTranslator;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.pncmetrics.MetricsConfiguration;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.CallbackTarget;
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.causeway.CausewayFailure;

import static org.jboss.pnc.constants.Attributes.BUILD_BREW_NAME;
import static org.jboss.pnc.constants.Attributes.BUILD_BREW_VERSION;
import org.jboss.pnc.constants.MDCKeys;
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

    private final MetricsConfiguration metricsConfiguration;

    @Inject
    public PncImportControllerImpl(
            PncClient pnclClient,
            BrewClient brewClient,
            BPMClient bpmClient,
            BuildTranslator translator,
            CausewayConfig config,
            MetricsConfiguration metricConfiguration) {
        this.pncClient = pnclClient;
        this.brewClient = brewClient;
        this.bpmClient = bpmClient;
        this.translator = translator;
        this.config = config;
        this.metricsConfiguration = metricConfiguration;
    }

    @Override
    @Asynchronous
    public void importMilestone(int milestoneId, CallbackTarget callback, String callbackId, String username) {
        log.info("Importing PNC milestone {}.", milestoneId);

        MetricRegistry registry = metricsConfiguration.getMetricRegistry();
        Meter meter = registry.meter(METRICS_BASE + METRICS_METER);
        meter.mark();

        Timer timer = registry.timer(METRICS_BASE + METRICS_TIMER);
        Timer.Context context = timer.time();

        Meter errors = registry.meter(METRICS_BASE + METRICS_ERRORS);

        MilestoneReleaseResultRest result = new MilestoneReleaseResultRest();
        result.setMilestoneId(milestoneId);
        try {
            List<BuildImportResultRest> results = importProductMilestone(milestoneId, username);
            result.setBuilds(results);

            if (results.stream().anyMatch(r -> r.getStatus() == BuildImportStatus.ERROR)) {
                result.setReleaseStatus(ReleaseStatus.SET_UP_ERROR);
                bpmClient.failure(callback.getUrl(), callbackId, result);
                errors.mark();
            } else if (results.stream().anyMatch(r -> r.getStatus() == BuildImportStatus.FAILED)) {
                result.setReleaseStatus(ReleaseStatus.IMPORT_ERROR);
                bpmClient.failure(callback.getUrl(), callbackId, result);
                errors.mark();
            } else {
                result.setReleaseStatus(ReleaseStatus.SUCCESS);
                bpmClient.success(callback.getUrl(), callbackId, result);
            }
        } catch (CausewayFailure ex) {
            log.error("Failed to import milestone. " + ex.getMessage(), ex);
            result.setErrorMessage(ex.getMessage());
            result.setReleaseStatus(ReleaseStatus.FAILURE);
            bpmClient.failure(callback.getUrl(), callbackId, result);
            errors.mark();
        } catch (CausewayException | RuntimeException ex) {
            log.error("Failed to import milestone. " + ex.getMessage(), ex);
            result.setErrorMessage(ex.getMessage());
            result.setReleaseStatus(ReleaseStatus.SET_UP_ERROR);
            bpmClient.error(callback.getUrl(), callbackId, result);
            errors.mark();
        }

        // stop the timer
        context.stop();
    }

    private List<BuildImportResultRest> importProductMilestone(int milestoneId, String username)
            throws CausewayException {
        String tagPrefix = pncClient.getTagForMilestone(milestoneId);
        if (!brewClient.tagsExists(tagPrefix)) {
            throw new CausewayFailure(messageMissingTag(tagPrefix, config.getKojiURL()));
        }

        Collection<Build> builds = findAndAssertBuilds(milestoneId);

        List<BuildImportResultRest> results = new ArrayList<>();
        for (Build build : builds) {
            BuildImportResultRest importResult;
            try (MDC.MDCCloseable mdcClose = MDC.putCloseable(MDCKeys.BUILD_ID_KEY, build.getId())) {
                BuildArtifacts artifacts = pncClient.findBuildArtifacts(Integer.valueOf(build.getId()));
                importResult = importBuild(build, username, artifacts);
                if (importResult.getStatus() == BuildImportStatus.SUCCESSFUL && importResult.getBrewBuildId() != null) {
                    brewClient.tagBuild(tagPrefix, getNVR(build, artifacts));
                }
            } catch (CausewayException ex) {
                log.error("Failed to import build " + build.getId() + ".", ex);
                importResult = new BuildImportResultRest();
                importResult.setBuildRecordId(Integer.valueOf(build.getId()));
                importResult.setErrorMessage(ex.getMessage());
                importResult.setStatus(BuildImportStatus.ERROR);
            }
            results.add(importResult);
        }

        return results;
    }

    private Collection<Build> findAndAssertBuilds(int milestoneId) throws CausewayException {
        Collection<Build> builds;
        try {
            builds = pncClient.findBuildsOfProductMilestone(milestoneId);
        } catch (Exception e) {
            throw new CausewayException(messagePncReleaseNotFound(milestoneId, e), e);
        }
        if (builds == null || builds.isEmpty()) {
            throw new CausewayException(messageMilestoneWithoutBuilds(milestoneId));
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
            ret.setBuildRecordId(Integer.valueOf(build.getId()));
            ret.setStatus(BuildImportStatus.SUCCESSFUL); // TODO: replace with EXISTING?

            log.info("Build {} was already imported with id {}.", nvr.getNVR(), brewBuild.getId());
            return ret;
        }

        List<BuildArtifacts.PncArtifact> blackArtifacts = new ArrayList<>();
        for (Iterator<BuildArtifacts.PncArtifact> it = artifacts.buildArtifacts.iterator(); it.hasNext();) {
            BuildArtifacts.PncArtifact artifact = it.next();
            if (artifact.artifactQuality == ArtifactQuality.BLACKLISTED) {
                blackArtifacts.add(artifact);
                it.remove();
            }
        }

        final BuildImportResultRest buildResult;
        if (artifacts.buildArtifacts.isEmpty()) {
            buildResult = new BuildImportResultRest();
            buildResult.setBuildRecordId(Integer.valueOf(build.getId()));
            buildResult.setStatus(BuildImportStatus.SUCCESSFUL);
            buildResult.setErrorMessage("Build doesn't contain any artifacts to import, skipping.");
            log.info("PNC build {} doesn't contain any artifacts to import, skipping.", build.getId());
        } else {
            String log = pncClient.getBuildLog(Integer.valueOf(build.getId()));
            KojiImport kojiImport = translator.translate(nvr, build, artifacts, log, username);
            ImportFileGenerator importFiles = translator.getImportFiles(artifacts, log);
            buildResult = brewClient.importBuild(nvr, Integer.valueOf(build.getId()), kojiImport, importFiles);

            long artifactSize = artifacts.buildArtifacts.stream().mapToLong(pncArtifact -> pncArtifact.size).sum();
            int artifactNumber = artifacts.buildArtifacts.size();
            int logLenght = log.length();
            try {
                logLenght = log.getBytes("UTF-8").length;
            } catch (UnsupportedEncodingException e) {
            }

            updateHistogram(metricsConfiguration, METRICS_ARTIFACTS_SIZE_KEY, artifactSize);
            updateHistogram(metricsConfiguration, METRICS_ARTIFACTS_NUMBER_KEY, artifactNumber);
            updateHistogram(metricsConfiguration, METRICS_LOGS_SIZE_KEY, logLenght);
            updateHistogram(metricsConfiguration, METRICS_LOGS_NUMBER_KEY, 1);
        }

        for (BuildArtifacts.PncArtifact artifact : blackArtifacts) {
            log.warn(
                    "Failed to import artifact {}: {}",
                    artifact.id,
                    "This artifact is blacklisted, so it was not imported.");
        }
        return buildResult;
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

    static String messagePncReleaseNotFound(long releaseId, Exception e) {
        return "Can not find PNC release " + releaseId + " - " + e.getMessage();
    }

    private static String messageMilestoneWithoutBuilds(long milestoneId) {
        return "Milestone " + milestoneId + " does not contain any build";
    }

    public static String messageMissingTag(String tagPrefix, String kojiURL) {
        final String parent = tagPrefix;
        final String child = tagPrefix + BrewClientImpl.BUILD_TAG_SUFIX;
        return "Proper brew tags don't exist. Create them before importing builds.\n" + "Tag prefix: " + tagPrefix
                + "\n" + "You should ask RCM to create at least following tags:\n" + " * " + child + "\n" + "   * "
                + parent + "\n" + "in " + kojiURL + "\n" + "(Note that tag " + child + " should inherit from tag "
                + parent + ")";
    }

    BrewNVR getNVR(Build build, BuildArtifacts artifacts) throws CausewayException {
        if (!build.getAttributes().containsKey(BUILD_BREW_NAME)) {
            throw new CausewayException("Build attribute " + BUILD_BREW_NAME + " can't be missing");
        }
        String version = build.getAttributes().get(BUILD_BREW_VERSION);
        if (version == null) {
            version = BuildTranslator.guessVersion(build, artifacts);
        }
        return new BrewNVR(build.getAttributes().get(BUILD_BREW_NAME), version, "1");
    }

    private boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

}
