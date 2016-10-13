package org.jboss.pnc.causeway.brewclient;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.KojiImportResult;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiNVR;
import com.redhat.red.build.koji.model.xmlrpc.KojiSessionInfo;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.ctl.PncImportControllerImpl;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class BrewClientImpl implements BrewClient {

    private final KojiClient koji;

    private final String brewUrl;

    @Inject
    public BrewClientImpl(KojiClient koji, CausewayConfig config) {
        this.koji = koji;
        brewUrl = config.getKojiWebURL();
    }

    @Override
    public BrewBuild findBrewBuildOfNVR(BrewNVR nvr) throws CausewayException {
        try {
            KojiSessionInfo session = koji.login();

            KojiNVR knvr = new KojiNVR(nvr.getKojiName(), nvr.getVersion(), nvr.getRelease());
            KojiBuildInfo bi = koji.getBuildInfo(knvr, session); // returns null if missing

            koji.logout(session);
            return bi == null? null : toBrewBuild(bi, nvr);
        } catch (KojiClientException ex) {
            throw new CausewayException("Failure while comunicating with Koji: " + ex.getMessage(), ex);
        }
    }

    private static BrewBuild toBrewBuild(KojiBuildInfo bi, BrewNVR nvr) {
        return new BrewBuild(bi.getId(), nvr);
    }

    @Override
    public void tagBuild(String tag, BrewNVR nvr) throws CausewayException{
        try {
            KojiSessionInfo session = koji.login();

            koji.addPackageToTag(tag, nvr.getKojiName(), session);
            koji.tagBuild(tag, nvr.getNVR(), session);

            koji.logout(session);
        } catch (KojiClientException ex) {
            throw new CausewayException("Failure while comunicating with Koji: " + ex.getMessage(), ex);
        }
    }

    @Override
    public BuildImportResultRest importBuild(BrewNVR nvr, int buildRecordId, KojiImport kojiImport, ImportFileGenerator importFiles) throws CausewayException {
        BuildImportResultRest ret = new BuildImportResultRest();
        ret.setBuildRecordId(buildRecordId);
        ret.setStatus(BuildImportStatus.SUCCESSFUL);
        try {
            KojiSessionInfo session = koji.login();

            KojiImportResult result = koji.importBuild(kojiImport, importFiles, session);
            koji.logout(session);

            List<ArtifactImportError> importErrors = new ArrayList<>();

            Map<String, KojiClientException> kojiErrors = result.getUploadErrors();
            if(kojiErrors != null){
                for(Map.Entry<String, KojiClientException> e : kojiErrors.entrySet()){
                    ArtifactImportError importError = new ArtifactImportError();
                    importError.setArtifactId(importFiles.getId(e.getKey()));
                    importError.setErrorMessage(e.getValue().getMessage());
                    importErrors.add(importError);
                    Logger.getLogger(PncImportControllerImpl.class.getName()).log(Level.WARNING, "Failed to import.", e.getValue());
                }
            }

            Map<Integer, String> importerErrors = importFiles.getErrors();
            if(!importerErrors.isEmpty()){
                for(Map.Entry<Integer, String> e : importerErrors.entrySet()){
                    ArtifactImportError importError = new ArtifactImportError();
                    importError.setArtifactId(e.getKey());
                    importError.setErrorMessage(e.getValue());
                    importErrors.add(importError);
                    Logger.getLogger(PncImportControllerImpl.class.getName()).log(Level.WARNING, "Failed to import: {0}", e.getValue());
                }
            }
            if(!importErrors.isEmpty()){
                ret.setErrors(importErrors);
                ret.setStatus(BuildImportStatus.FAILED);
            }

            KojiBuildInfo bi = result.getBuildInfo();

            if(bi == null){
                ret.setErrorMessage("Import to koji failed");
                ret.setStatus(BuildImportStatus.ERROR);
            }else{
                ret.setBrewBuildId(bi.getId());
                ret.setBrewBuildUrl(getBuildUrl(bi.getId()));
            }

            return ret;
        } catch (KojiClientException ex) {
            throw new CausewayException("Failure while comunicating with Koji: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getBuildUrl(int id) {
        return brewUrl + id;
    }

}
