package org.jboss.pnc.causeway.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.propulsor.deploy.resteasy.ResteasyAppConfig;

import static java.util.Arrays.asList;

@ApplicationScoped
public class CausewayResteasyAppConfig implements ResteasyAppConfig {

    @Override
    public List<String> getJaxRsMappings() {
        return asList();//FIXME
    }
}
