package org.jboss.pnc.causeway.rest;

import org.jboss.pnc.causeway.ctl.PncImportController;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.UUID;


@RequestScoped
public class PncImportResourceEndpoint implements PncImportResource {

    @Inject
    private PncImportController controller;

    @Override
    public Response testResponse( String var )
    {
        return Response.ok( var ).build();
    }

    @Override
    public BrewPushMilestoneResponse importProductMilestone(BrewPushMilestone request)
    {
        String id = UUID.randomUUID().toString();
        
        controller.importMilestone(request.getContent().getMilestoneId(), request.getCallback(), id);
        
        return new BrewPushMilestoneResponse(new Callback(id));
    }


}
