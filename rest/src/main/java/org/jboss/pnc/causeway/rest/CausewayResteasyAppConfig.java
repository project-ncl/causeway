package org.jboss.pnc.causeway.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.propulsor.deploy.resteasy.ResteasyAppConfig;

import static java.util.Arrays.asList;

@ApplicationScoped
public class CausewayResteasyAppConfig implements ResteasyAppConfig {

    @Override
    public List<String> getJaxRsMappings() {
        // Just map everything for now...until we have some static content to serve that needs the UI servlet from the
        // propulsor-undertow module.
        return asList( "/*" );
    }
}
