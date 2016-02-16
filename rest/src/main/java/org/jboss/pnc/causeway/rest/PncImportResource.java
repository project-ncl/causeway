package org.jboss.pnc.causeway.rest;

import org.commonjava.propulsor.deploy.resteasy.RestResources;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.ctl.PncImportController;
import org.jboss.pnc.causeway.model.ProductReleaseImportResult;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@RequestScoped
@Path( "/import" )
public class PncImportResource
        implements RestResources
{

    @Inject
    private PncImportController controller;

    @GET
    @Path( "test/{variable}" )
    public Response testResponse( @PathParam( "variable" ) String var )
    {
        return Response.ok( var ).build();
    }

    @GET
    @Path( "/product/release/{releaseId}" )
    public ProductReleaseImportResult importProductRelease(@PathParam( "releaseId" ) int releaseId )
    {
        try
        {
            return controller.importProductRelease( releaseId );
        }
        catch ( CausewayException e )
        {
            throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
        }
    }
}
