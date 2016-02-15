package org.jboss.pnc.causeway.brewclient;

import org.jboss.pnc.causeway.model.BrewBuild;
import org.jboss.pnc.causeway.model.BrewNVR;
import org.jboss.pnc.causeway.model.BuildImportResult;
import org.jboss.pnc.causeway.pncclient.PncBuild;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BrewClient {
    public BrewBuild findBrewBuildOfNVR(BrewNVR nvr) {
        return null;
    }

    public BuildImportResult importBuild(BrewNVR nvr, PncBuild build) {
        return null;
    }

}
