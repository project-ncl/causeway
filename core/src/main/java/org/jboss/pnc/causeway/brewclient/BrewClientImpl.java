package org.jboss.pnc.causeway.brewclient;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.rest.restmodel.causeway.ArtifactImportError;
import org.jboss.pnc.rest.restmodel.causeway.BuildImportResultRest;
import org.jboss.pnc.rest.restmodel.causeway.BuildImportStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.KojiImportResult;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiNVR;
import com.redhat.red.build.koji.model.xmlrpc.KojiSessionInfo;

@ApplicationScoped
public class BrewClientImpl implements BrewClient {

    private final KojiClient koji;

    @Inject
    public BrewClientImpl(KojiClient koji) {
        this.koji = koji;
    }

    @Override
    public BrewBuild findBrewBuildOfNVR(BrewNVR nvr) throws CausewayException {
        return null;/*
        try {
            KojiSessionInfo session = koji.login();

            KojiNVR knvr = new KojiNVR(nvr.getName(), nvr.getVersion(), nvr.getRelease());
            KojiBuildInfo bi = koji.getBuildInfo(knvr, session); // returns null if missing

            return toBrewBuild(bi, nvr);
        } catch (KojiClientException ex) {
            throw new CausewayException("Failure while comunicating with Koji", ex);
        }*/
    }

    private static BrewBuild toBrewBuild(KojiBuildInfo bi, BrewNVR nvr) {
        return new BrewBuild(bi.getId(), nvr);
    }

    @Override
    public BuildImportResultRest importBuild(BrewNVR nvr, int buildRecordId, KojiImport kojiImport, ImportFileGenerator importFiles) throws CausewayException {
        BuildImportResultRest ret = new BuildImportResultRest();
        ret.setBuildRecordId(buildRecordId);
        ret.setStatus(BuildImportStatus.SUCCESSFUL);
        try {
            KojiSessionInfo session = koji.login();

            KojiImportResult result = koji.importBuild(kojiImport, () -> importFiles, session);

            Map<String, KojiClientException> errors = result.getUploadErrors();

            if(errors != null && !errors.isEmpty()){
                List<ArtifactImportError> importErrors = new ArrayList<>();
                for(Map.Entry<String, KojiClientException> e : errors.entrySet()){
                    ArtifactImportError importError = new ArtifactImportError();
                    importError.setArtifactId(importFiles.getId(e.getValue()));
                    importError.setErrorMessage(e.getValue().getMessage());
                }
                ret.setErrors(importErrors);
                ret.setStatus(BuildImportStatus.FAILED);
            }

            KojiBuildInfo bi = result.getBuildInfo(); // TODO: handle error

            if(bi == null){
                ret.setErrorMessage("Import to koji failed");
                ret.setStatus(BuildImportStatus.ERROR);
            }else{
                ret.setBrewBuildId(bi.getId());
                ret.setBrewBuildUrl(getBuildUrl(bi.getId()));
            }

            return ret;
        } catch (KojiClientException ex) {
            throw new CausewayException("Failure while comunicating with Koji", ex);
        }
    }

    @Override
    public String getBuildUrl(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
