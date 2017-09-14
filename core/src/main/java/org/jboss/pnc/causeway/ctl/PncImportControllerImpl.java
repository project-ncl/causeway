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

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.bpmclient.BPMClient;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BrewClientImpl;
import org.jboss.pnc.causeway.brewclient.BuildTranslator;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.rest.CallbackTarget;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.causeway.MilestoneReleaseResultRest;
import org.jboss.pnc.rest.restmodel.causeway.BuildImportResultRest;
import org.jboss.pnc.rest.restmodel.causeway.BuildImportStatus;
import org.jboss.pnc.rest.restmodel.causeway.ReleaseStatus;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.red.build.koji.model.json.KojiImport;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PncImportControllerImpl implements PncImportController {

    private final PncClient pncClient;
    private final BrewClient brewClient;
    private final BPMClient bpmClient;
    private final BuildTranslator translator;
    private final CausewayConfig config;

    @Inject
    public PncImportControllerImpl(PncClient pnclClient, BrewClient brewClient, BPMClient bpmClient, BuildTranslator translator, CausewayConfig config) {
        this.pncClient = pnclClient;
        this.brewClient = brewClient;
        this.bpmClient = bpmClient;
        this.translator = translator;
        this.config = config;
    }

    @Override
    @Asynchronous
    public void importMilestone(int milestoneId, CallbackTarget callback, String callbackId) {
        Logger.getLogger(PncImportControllerImpl.class.getName()).log(Level.INFO, "Entering importMilestone.");
        MilestoneReleaseResultRest result = new MilestoneReleaseResultRest();
        result.setMilestoneId(milestoneId);
        try {
            List<BuildImportResultRest> results = importProductMilestone(milestoneId, false);
            result.setBuilds(results);

            if( results.stream().anyMatch(r -> r.getErrorMessage() != null)){
                result.setReleaseStatus(ReleaseStatus.SET_UP_ERROR);
                bpmClient.failure(callback.getUrl(), callbackId, result);
            }else if( results.stream().anyMatch(r -> isNotEmpty(r.getErrors()))){
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
    }

    private List<BuildImportResultRest> importProductMilestone(int milestoneId, boolean dryRun) throws CausewayException {
        String tagPrefix = pncClient.getTagForMilestone(milestoneId);
        if (!brewClient.tagsExists(tagPrefix)) {
            throw new CausewayException(messageMissingTag(tagPrefix));
        }

        Collection<BuildRecordRest> builds = findAndAssertBuilds(milestoneId);

        List<BuildImportResultRest> results = new ArrayList<>();
        for (BuildRecordRest build : builds) {
            BuildImportResultRest importResult;
            try{
                importResult = importBuild(build, dryRun);
                if(importResult.getStatus() == BuildImportStatus.SUCCESSFUL){
                    brewClient.tagBuild(tagPrefix, getNVR(build));
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

    private Collection<BuildRecordRest> findAndAssertBuilds(int milestoneId) throws CausewayException {
        Collection<BuildRecordRest> builds;
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
    
    private BuildImportResultRest importBuild(BuildRecordRest build, boolean dryRun) throws CausewayException {
        BrewNVR nvr = getNVR(build);
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

        BuildArtifacts artifacts = pncClient.findBuildArtifacts(build.getId());
        String log = pncClient.getBuildLog(build.getId());

        KojiImport kojiImport = translator.translate(nvr, build, artifacts, log);
        ImportFileGenerator importFiles = translator.getImportFiles(artifacts, log);

        return brewClient.importBuild(nvr, build.getId(), kojiImport, importFiles);
    }

    static String messagePncReleaseNotFound(long releaseId, Exception e) {
        return "Can not find PNC release " + releaseId + " - " + e.getMessage();
    }

    static String messageMilestoneWithoutBuilds(long milestoneId) {
        return "Milestone " + milestoneId + " does not contain any build";
    }

    static String messageBuildNotFound(Integer buildId) {
        return "PNC build id " + buildId + " not found";
    }

    private String messageMissingTag(String tagPrefix) {
        final String parent = tagPrefix;
        final String child = tagPrefix + BrewClientImpl.BUILD_TAG_SUFIX;
        return "Proper brew tags don't exist. Create them before importing builds.\n"
              + "Tag prefix: " + tagPrefix+ "\n"
              + "You should ask RCM to create at least following tags:\n"
              + " * " + child + "\n"
              + "   * " + parent + "\n"
              + "in " + config.getKojiURL() + "\n"
              + "(Note that tag " + child + " should inherit from tag " + parent + ")";
    }

    private BrewNVR getNVR(BuildRecordRest build) {
        return new BrewNVR(build.getExecutionRootName(), build.getExecutionRootVersion(), "1");
    }

    private boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

}
