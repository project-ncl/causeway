package org.jboss.pnc.causeway.rest.spi;

import org.jboss.pnc.causeway.rest.model.UntagRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@Path("/untag")
public interface Untag {

    @POST
    @Path("/build")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response untagBuild(UntagRequest request);

}
