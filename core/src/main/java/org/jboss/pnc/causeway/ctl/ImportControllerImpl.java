package org.jboss.pnc.causeway.ctl;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.CausewayFailure;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BuildTranslator;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import static org.jboss.pnc.causeway.ctl.PncImportControllerImpl.messageMissingTag;

import org.jboss.pnc.causeway.metrics.MetricsConfiguration;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.CallbackTarget;
import org.jboss.pnc.causeway.rest.model.Build;
import org.jboss.pnc.causeway.rest.model.BuiltArtifact;
import org.jboss.pnc.causeway.rest.model.MavenBuiltArtifact;
import org.jboss.pnc.causeway.rest.model.TaggedBuild;
import org.jboss.pnc.causeway.rest.model.response.BuildRecordPushResultRest;
import org.jboss.pnc.causeway.rest.model.response.BuildRecordPushResultRest.BuildRecordPushResultRestBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.red.build.koji.model.json.KojiImport;

import lombok.Data;

import org.jboss.pnc.causeway.rest.model.response.OperationStatus;
import org.jboss.pnc.causeway.rest.model.response.UntagResultRest;
import org.jboss.pnc.causeway.rest.model.response.UntagResultRest.UntagResultRestBuilder;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ImportControllerImpl implements ImportController {

    private final Logger logger = Logger.getLogger(ImportControllerImpl.class.getName());

    private static final String METRICS_BASE = "causeway.import.build";
    private static final String METRICS_TIMER = ".timer";
    private static final String METRICS_METER = ".meter";

    private static final String BUILD_NOT_TAGGED = " but not previously tagged. Tagged now.";
    private static final String BUILD_ALREADY_IMPORTED = "Build was already imported with id ";

    @Inject
    private BrewClient brewClient;
    @Inject
    private BuildTranslator translator;
    @Inject
    private CausewayConfig config;
    private ResteasyClient restClient;

    @Inject
    private MetricsConfiguration metricsConfiguration;

    @Inject
    public ImportControllerImpl() {
        restClient = new ResteasyClientBuilder().connectionPoolSize(4).build();
    }

    @Override
    @Asynchronous
    public void importBuild(Build build, CallbackTarget callback, String username, boolean reimport) {
        logger.log(Level.INFO, "Importing external build {0} to tag {1}", new Object[]{build.getExternalBuildID(), build.getTagPrefix()});

        MetricRegistry registry = metricsConfiguration.getMetricRegistry();
        Meter meter = registry.meter(METRICS_BASE + METRICS_METER);
        meter.mark();

        Timer timer = registry.timer(METRICS_BASE + METRICS_TIMER);
        Timer.Context context = timer.time();

        BuildRecordPushResultRestBuilder response = BuildRecordPushResultRest.builder();
        response.buildRecordId(build.getExternalBuildID());
        try {
            BuildResult result = importBuild(build, build.getTagPrefix(), username, reimport);
            response.brewBuildId(result.getBrewID());
            response.brewBuildUrl(result.getBrewURL());
            response.status(OperationStatus.SUCCESS);
            response.log(result.getMessage());
        } catch (CausewayFailure ex) {
            logger.log(Level.SEVERE, "Failed to import build.", ex);
            response.status(OperationStatus.FAILED);
            response.artifactImportErrors(ex.getArtifactErrors());
            response.log(getMessageOrStacktrace(ex));
        } catch (CausewayException | RuntimeException ex) {
            logger.log(Level.SEVERE, "Error while importing build.", ex);
            response.status(OperationStatus.SYSTEM_ERROR);
            response.log(getMessageOrStacktrace(ex));
        }
        respond(callback, response.build());

        // stop the timer
        context.stop();
    }

    @Override
    @Asynchronous
    public void untagBuild(TaggedBuild build, CallbackTarget callback) {
        logger.log(Level.INFO, "Untaging build {0} from tag {1}", new Object[]{build.getBrewBuildId(), build.getTagPrefix()});

        UntagResultRestBuilder response = UntagResultRest.builder();
        response.brewBuildId(build.getBrewBuildId());
        try {
            untagBuild(build.getBrewBuildId(), build.getTagPrefix());
            response.log("Brew build " + build.getBrewBuildId() + " untaged from tag " + build.getTagPrefix());
            response.status(OperationStatus.SUCCESS);
        } catch (CausewayFailure ex) {
            logger.log(Level.SEVERE, "Failed to untag build.", ex);
            response.status(OperationStatus.FAILED);
            response.log(getMessageOrStacktrace(ex));
        } catch (CausewayException | RuntimeException ex) {
            logger.log(Level.SEVERE, "Error while untaging build.", ex);
            response.status(OperationStatus.SYSTEM_ERROR);
            response.log(getMessageOrStacktrace(ex));
        }
        respond(callback, response.build());
    }

    private BuildResult importBuild(Build build, String tagPrefix, String username, boolean reimport) throws CausewayException {
        if (build.getBuiltArtifacts().isEmpty()) {
            throw new CausewayFailure("Build doesn't contain any artifacts");
        }
        if (!brewClient.tagsExists(tagPrefix)) {
            throw new CausewayFailure(messageMissingTag(tagPrefix, config.getKojiURL()));
        }

        BrewNVR nvr = getNVR(build);

        BrewBuild brewBuild = brewClient.findBrewBuildOfNVR(nvr);
        String message;
        if (brewBuild == null) {
            brewBuild = translateAndImport(nvr, build, username);
            message = "Build imported with id " + brewBuild.getId() + ".";
        } else {
            if(reimport){
                int revision = 1;
                while (brewBuild != null && brewClient.isBuildTagged(tagPrefix, brewBuild)) {
                    nvr = getNVR(build, ++revision);
                    brewBuild = brewClient.findBrewBuildOfNVR(nvr);
                }
                if (brewBuild == null) {
                    brewBuild = translateAndImport(nvr, build, username);
                    message = "Build was previously imported. Reimported again with revision " + revision
                            + " and with id " + brewBuild.getId() + ".";
                } else {
                    message = BUILD_ALREADY_IMPORTED + brewBuild.getId() + BUILD_NOT_TAGGED;
                }
            } else {
                message = BUILD_ALREADY_IMPORTED + brewBuild.getId();
                if (!brewClient.isBuildTagged(tagPrefix, brewBuild)) {
                    message += BUILD_NOT_TAGGED;
                }
            }
        }
        brewClient.tagBuild(tagPrefix, brewBuild);

        return new BuildResult(brewBuild.getId(), brewClient.getBuildUrl(brewBuild.getId()),
                message);
    }

    private BrewBuild translateAndImport(BrewNVR nvr, Build build, String username) throws CausewayException {
        KojiImport kojiImport = translator.translate(nvr, build, username);
        ImportFileGenerator importFiles = translator.getImportFiles(build);
        return brewClient.importBuild(nvr, kojiImport, importFiles);
    }

    BrewNVR getNVR(Build build) throws CausewayException{
        String buildVersion = build.getBuildVersion();
        if(buildVersion == null){
            buildVersion = BuildTranslator.guessVersion(build);
        }

        return new BrewNVR(build.getBuildName(), buildVersion, "1");
    }

    BrewNVR getNVR(Build build, int revision) throws CausewayException{
        if(revision <= 0) throw new IllegalArgumentException("Revison must be positive, is " + revision);
        String buildVersion = build.getBuildVersion();
        if(buildVersion == null){
            buildVersion = BuildTranslator.guessVersion(build);
        }

        return new BrewNVR(build.getBuildName(), buildVersion, Integer.toString(revision));
    }

    private <T> void respond(CallbackTarget callback, T responseEntity) {
        if (callback == null) {
            logger.log(Level.INFO, "Not sending callback.");
            return;
        }
        logger.log(Level.INFO, "Will send callback to {0}.", callback.getUrl());
        ResteasyWebTarget target = restClient.target(callback.getUrl());
        Invocation.Builder request = target.request(MediaType.APPLICATION_JSON);
        callback.getHeaders().forEach(request::header);
        Response response = request.post(Entity.entity(responseEntity, MediaType.APPLICATION_JSON_TYPE));
        logger.log(Level.INFO, "Callback response: {0} - {1}", new Object[]{response.getStatusInfo(), response.readEntity(String.class)});
    }

    private void untagBuild(int brewBuildId, String tagPrefix) throws CausewayException {
        BrewBuild build = brewClient.findBrewBuild(brewBuildId);
        if (build == null) {
            throw new CausewayFailure("Build with given id (" + brewBuildId + ") not found");
        }
        brewClient.untagBuild(tagPrefix, build);
    }

    /**
     * If the exception.getMessage() is null, return the stacktrace instead
     * If exception.getMessage() is not-null, just return the message
     *
     * @param e exception
     * @return reason for exception
     */
    private String getMessageOrStacktrace(Exception e) {

        // get the message
        String message = e.getMessage();

        // it can be null for NullPointerException for example!
        if (message != null) {
            return message;
        }

        // if message is null, return the stacktrace instead
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        return sw.toString();
    }

    @Data
    public static class BuildResult {

        private final int brewID;
        private final String brewURL;
        private final String message;
    }
}
