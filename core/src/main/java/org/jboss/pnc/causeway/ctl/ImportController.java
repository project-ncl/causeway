package org.jboss.pnc.causeway.ctl;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.model.ProductReleaseImportResult;
import org.jboss.pnc.causeway.pncl.ProjectNewcastleClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

/**
 * Created by jdcasey on 2/9/16.
 */
@ApplicationScoped
public class ImportController
{
    private ProjectNewcastleClient pnclClient;

    @Inject
    public ImportController( ProjectNewcastleClient pnclClient )
    {
        this.pnclClient = pnclClient;
    }

    public ProductReleaseImportResult importProductRelease( int releaseId )
            throws CausewayException
    {
//        Set<BuildRecord> records = pnclClient.getBuildRecordIdsForRelease( releaseId );

        // 1. Retrieve the build records for the given product release id, from PNC
        // 2. For each, retrieve the build metadata and list of output artifacts from PNC
        // 2a.    Generate a Brew NVR from the build metadata / artifacts
        // 2b.    Check if the build already exists in brew...if so, log as successful import (?)
        // 2c.    If not, run a Koji build import using the artifacts + metadata, and collect the result (error or
        //        resulting brew build id)
        // 3. Assemble a ProductReleaseImportResult from the collected information and return it
        return null;
    }
}
