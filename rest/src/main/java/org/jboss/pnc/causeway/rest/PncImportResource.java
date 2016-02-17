package org.jboss.pnc.causeway.rest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path( "/import" )
public interface PncImportResource {

    @GET
    @Path( "/product/release/{releaseId}" )
    public ProductReleaseImportResult importProductRelease(@PathParam( "releaseId" ) int releaseId, @DefaultValue("false")@QueryParam("dryRun") boolean dryRun);
}
