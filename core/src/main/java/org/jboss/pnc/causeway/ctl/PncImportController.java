package org.jboss.pnc.causeway.ctl;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.rest.BuildImportResult;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.ProductReleaseImportResult;
import org.jboss.pnc.causeway.pncclient.PncBuild;
import org.jboss.pnc.causeway.pncclient.PncClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
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

    public ProductReleaseImportResult importProductRelease(long releaseId, boolean dryRun)
            throws CausewayException
    {
        Collection<Integer> buildIds = findAndAssertBuildIds(releaseId);

        ProductReleaseImportResult productReleaseImportResult = new ProductReleaseImportResult();
        for (Integer buildId : buildIds) {
            BuildImportResult importResult = importBuild(buildId, dryRun);
            productReleaseImportResult.addResult(buildId.longValue(), importResult);
        }

        return productReleaseImportResult;
    }

    private Collection<Integer> findAndAssertBuildIds(long releaseId) throws CausewayException {
        Collection<Integer> buildIds;
        try {
            buildIds = pncClient.findBuildIdsOfProductRelease(new Long(releaseId).intValue());
        } catch (Exception e) {
            throw new CausewayException(messagePncReleaseNotFound(releaseId, e), e);
        }
        if (buildIds == null || buildIds.size() == 0) {
            throw new CausewayException(messageReleaseWithoutBuildConfigurations(releaseId));
        }
        return buildIds;
    }

    private BuildImportResult importBuild(Integer buildId, boolean dryRun) {
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

    static String messageBuildNotFound(Integer buildId) {
        return "PNC build id " + buildId + " not found";
    }

}
