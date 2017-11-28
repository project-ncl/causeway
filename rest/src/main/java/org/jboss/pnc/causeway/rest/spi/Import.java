package org.jboss.pnc.causeway.rest.spi;

import org.jboss.pnc.causeway.rest.model.BuildImportRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Honza Brázdil <janinko.g@gmail.com>
 */

@Path( "/import" )
public interface Import {
    
    @POST
    @Path( "/build" )
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importBuild(BuildImportRequest request);
    
}
