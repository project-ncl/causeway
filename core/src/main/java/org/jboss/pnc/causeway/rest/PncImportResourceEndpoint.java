package org.jboss.pnc.causeway.rest;

import org.commonjava.propulsor.deploy.resteasy.RestResources;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.ctl.PncImportController;
import org.jboss.pnc.causeway.rest.ProductReleaseImportResult;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@RequestScoped
public class PncImportResourceEndpoint
        implements RestResources, PncImportResource {

    @Inject
    private PncImportController controller;

    public Response testResponse( @PathParam( "variable" ) String var )
    {
        return Response.ok( var ).build();
    }

    public ProductReleaseImportResult importProductRelease(int releaseId, boolean dryRun)
    {
        try
        {
            return controller.importProductRelease(releaseId, dryRun);
        }
        catch ( CausewayException e )
        {
            throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
        }
    }
}
