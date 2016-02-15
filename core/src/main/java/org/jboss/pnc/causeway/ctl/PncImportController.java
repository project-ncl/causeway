package org.jboss.pnc.causeway.ctl;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.model.BuildImportResult;
import org.jboss.pnc.causeway.model.BrewBuild;
import org.jboss.pnc.causeway.model.BrewNVR;
import org.jboss.pnc.causeway.model.ProductReleaseImportResult;
import org.jboss.pnc.causeway.pncclient.PncBuild;
import org.jboss.pnc.causeway.pncclient.PncClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class PncImportController
{
    private final PncClient pncClient;
    private final BrewClient brewClient;

    @Inject
    public PncImportController(PncClient pnclClient, BrewClient brewClient)
    {
        this.pncClient = pnclClient;
        this.brewClient = brewClient;
    }

    public ProductReleaseImportResult importProductRelease(long releaseId)
            throws CausewayException
    {
        Set<Long> buildIds = findAndAssertBuildIds(releaseId);

        ProductReleaseImportResult productReleaseImportResult = new ProductReleaseImportResult();
        for (Long buildId : buildIds) {
            BuildImportResult importResult = importBuild(buildId);
            productReleaseImportResult.addResult(buildId, importResult);
        }

        return productReleaseImportResult;
    }

    private Set<Long> findAndAssertBuildIds(long releaseId) throws CausewayException {
        Set<Long> buildIds;
        try {
            buildIds = pncClient.findBuildIdsOfRelease(releaseId);
        } catch (Exception e) {
            throw new CausewayException(messagePncReleaseNotFound(releaseId, e), e);
        }
        if (buildIds == null || buildIds.size() == 0) {
            throw new CausewayException(messageReleaseWithoutBuildConfigurations(releaseId));
        }
        return buildIds;
    }

    private BuildImportResult importBuild(Long buildId) {
        PncBuild build = pncClient.findBuild(buildId);
        if (build == null) {
            return new BuildImportResult(null, messageBuildNotFound(buildId));
        }
        BrewNVR nvr = build.createNVR();
        BrewBuild brewBuild = brewClient.findBrewBuildOfNVR(nvr);
        if ( brewBuild != null ) {
            // FIXME clarify behavior - if the build already exists in brew log as successful import ?
            return new BuildImportResult(brewBuild, null);
        }
        return brewClient.importBuild(nvr, build);
    }

    static String messagePncReleaseNotFound(long releaseId, Exception e) {
        return "Can not find PNC release " + releaseId + " - " + e.getMessage();
    }

    static String messageReleaseWithoutBuildConfigurations(long releaseId) {
        return "Release " + releaseId + " does not contain any build configurations";
    }

    static String messageBuildNotFound(Long buildId) {
        return "PNC build id " + buildId + " not found";
    }

}
