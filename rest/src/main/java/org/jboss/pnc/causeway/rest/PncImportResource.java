package org.jboss.pnc.causeway.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path( "/import" )
public interface PncImportResource {

    @POST
    @Path( "/product/milestone" )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BrewPushMilestoneResponse importProductMilestone(BrewPushMilestone request);

    @GET
    @Path( "/test/{variable}" )
    public Response testResponse(@PathParam( "variable" ) String var );
}
