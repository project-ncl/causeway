package org.jboss.pnc.causeway.rest;

import static org.jboss.pnc.causeway.rest.Constants.IMPORT_PATH;

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

/**
 * Created by jdcasey on 2/9/16.
 */
@RequestScoped
@Path( IMPORT_PATH)
public class ImportResource
        implements RestResources
{

    @Inject
    private PncImportController controller;

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
