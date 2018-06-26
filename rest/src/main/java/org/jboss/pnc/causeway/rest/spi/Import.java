package org.jboss.pnc.causeway.rest.spi;

import org.jboss.pnc.causeway.rest.model.BuildImportRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */

@Path( "/import" )
public interface Import {
    
    @POST
    @Path( "/build" )
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importBuild(BuildImportRequest request);

    @GET
    @Path("/test/{variable}")
    public Response testResponse(@PathParam("variable") String var);
}
