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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.redhat.red.build.koji.model.json.KojiImport;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.bpmclient.BPMClient;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BrewClientImpl;
import org.jboss.pnc.causeway.brewclient.BuildTranslator;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.metrics.MetricsConfiguration;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.CallbackTarget;
import org.jboss.pnc.causeway.rest.pnc.BuildImportResultRest;
import org.jboss.pnc.causeway.rest.pnc.BuildImportStatus;
import org.jboss.pnc.causeway.rest.pnc.MilestoneReleaseResultRest;
import org.jboss.pnc.causeway.rest.pnc.ReleaseStatus;
import org.jboss.pnc.dto.ArtifactImportError;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.ArtifactQuality;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jboss.pnc.constants.Attributes.BUILD_BREW_NAME;
import static org.jboss.pnc.constants.Attributes.BUILD_BREW_VERSION;

@Deprecated
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PncImportControllerImpl implements PncImportController {

    private static final String METRICS_BASE = "causeway.import.milestone";
    private static final String METRICS_TIMER = ".timer";
    private static final String METRICS_METER = ".meter";

    private final PncClient pncClient;
    private final BrewClient brewClient;
    private final BPMClient bpmClient;
    private final BuildTranslator translator;
    private final CausewayConfig config;

    private final MetricsConfiguration metricsConfiguration;

    @Inject
    public PncImportControllerImpl(PncClient pnclClient, BrewClient brewClient, BPMClient bpmClient, BuildTranslator translator, CausewayConfig config, MetricsConfiguration metricConfiguration) {
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
        Logger.getLogger(PncImportControllerImpl.class.getName()).log(Level.INFO, "Entering importMilestone.");

        MetricRegistry registry = metricsConfiguration.getMetricRegistry();
        Meter meter = registry.meter(METRICS_BASE + METRICS_METER);
        meter.mark();

        Timer timer = registry.timer(METRICS_BASE + METRICS_TIMER);
        Timer.Context context = timer.time();

        MilestoneReleaseResultRest result = new MilestoneReleaseResultRest();
        result.setMilestoneId(milestoneId);
        try {
            List<BuildImportResultRest> results = importProductMilestone(milestoneId, username);
            result.setBuilds(results);

            if (results.stream().anyMatch(r -> r.getStatus() == BuildImportStatus.ERROR)) {
                result.setReleaseStatus(ReleaseStatus.SET_UP_ERROR);
                bpmClient.failure(callback.getUrl(), callbackId, result);
            } else if (results.stream().anyMatch(r -> r.getStatus() == BuildImportStatus.FAILED)) {
                result.setReleaseStatus(ReleaseStatus.IMPORT_ERROR);
                bpmClient.failure(callback.getUrl(), callbackId, result);
            }else{
                result.setReleaseStatus(ReleaseStatus.SUCCESS);
                bpmClient.success(callback.getUrl(), callbackId, result);
            }

        } catch (CausewayException ex) {
            Logger.getLogger(PncImportControllerImpl.class.getName()).log(Level.SEVERE, "Failed to import milestone.", ex);
            result.setErrorMessage(ex.getMessage());
            result.setReleaseStatus(ReleaseStatus.SET_UP_ERROR);
            bpmClient.failure(callback.getUrl(), callbackId, result);
        } catch (RuntimeException ex) {
            Logger.getLogger(PncImportControllerImpl.class.getName()).log(Level.SEVERE, "Failed to import milestone.", ex);
            result.setErrorMessage(ex.getMessage());
            result.setReleaseStatus(ReleaseStatus.SET_UP_ERROR);
            bpmClient.error(callback.getUrl(), callbackId, result);
        }

        // stop the timer
        context.stop();
    }

    private List<BuildImportResultRest> importProductMilestone(int milestoneId, String username) throws CausewayException {
        String tagPrefix = pncClient.getTagForMilestone(milestoneId);
        if (!brewClient.tagsExists(tagPrefix)) {
            throw new CausewayException(messageMissingTag(tagPrefix, config.getKojiURL()));
        }

        Collection<Build> builds = findAndAssertBuilds(milestoneId);

        List<BuildImportResultRest> results = new ArrayList<>();
        for (Build build : builds) {
            BuildImportResultRest importResult;
            try{
                BuildArtifacts artifacts = pncClient.findBuildArtifacts(build.getId());
                importResult = importBuild(build, username, artifacts);
                if(importResult.getStatus() == BuildImportStatus.SUCCESSFUL){
                    brewClient.tagBuild(tagPrefix, getNVR(build, artifacts));
                }
            }catch(CausewayException ex){
                Logger.getLogger(PncImportControllerImpl.class.getName()).log(Level.SEVERE, "Failed to import build.", ex);
                importResult = new BuildImportResultRest();
                importResult.setBuildRecordId(build.getId());
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
    
    private BuildImportResultRest importBuild(Build build, String username, BuildArtifacts artifacts) throws CausewayException {
        BrewNVR nvr = getNVR(build, artifacts);
        BrewBuild brewBuild = brewClient.findBrewBuildOfNVR(nvr);
        if (brewBuild != null) {
            // FIXME clarify behavior - if the build already exists in brew log as successful import ?
            BuildImportResultRest ret = new BuildImportResultRest();
            ret.setBrewBuildId(brewBuild.getId());
            ret.setBrewBuildUrl(brewClient.getBuildUrl(brewBuild.getId()));
            ret.setBuildRecordId(build.getId());
            ret.setStatus(BuildImportStatus.SUCCESSFUL); // TODO: replace with EXISTING?

            return ret;
        }

        List<BuildArtifacts.PncArtifact> blackArtifacts = new ArrayList<>();
        for (Iterator<BuildArtifacts.PncArtifact> it = artifacts.buildArtifacts.iterator(); it.hasNext();) {
            BuildArtifacts.PncArtifact artifact = it.next();
            if(artifact.artifactQuality == ArtifactQuality.BLACKLISTED){
                blackArtifacts.add(artifact);
                it.remove();
            }
        }

        final BuildImportResultRest buildResult;
        if (artifacts.buildArtifacts.isEmpty()) {
            buildResult = new BuildImportResultRest();
            buildResult.setBuildRecordId(build.getId());
            buildResult.setStatus(BuildImportStatus.SUCCESSFUL);
            buildResult.setErrorMessage("Build doesn't contain any artifacts to import, skipping.");
        } else {
            String log = pncClient.getBuildLog(build.getId());
            KojiImport kojiImport = translator.translate(nvr, build, artifacts, log, username);
            ImportFileGenerator importFiles = translator.getImportFiles(artifacts, log);
            buildResult = brewClient.importBuild(nvr, build.getId(), kojiImport, importFiles);
        }

        for (BuildArtifacts.PncArtifact artifact : blackArtifacts) {
            ArtifactImportError error = ArtifactImportError.builder()
                    .artifactId(artifact.id)
                    .errorMessage("This artifact is blacklisted, so it was not imported.")
                    .build();
            buildResult.getErrors().add(error);
        }
        return buildResult;
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
        return "Proper brew tags don't exist. Create them before importing builds.\n"
              + "Tag prefix: " + tagPrefix+ "\n"
              + "You should ask RCM to create at least following tags:\n"
              + " * " + child + "\n"
              + "   * " + parent + "\n"
              + "in " + kojiURL + "\n"
              + "(Note that tag " + child + " should inherit from tag " + parent + ")";
    }

    BrewNVR getNVR(Build build, BuildArtifacts artifacts) throws CausewayException {
        if(!build.getAttributes().containsKey(BUILD_BREW_NAME)){
            throw new CausewayException("Build attribute " + BUILD_BREW_NAME + " can't be missing");
        }
        String version = build.getAttributes().get(BUILD_BREW_VERSION);
        if(version == null){
            version = BuildTranslator.guessVersion(build, artifacts);
        }
        return new BrewNVR(build.getAttributes().get(BUILD_BREW_NAME), version, "1");
    }

    private boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

}
