package org.jboss.pnc.causeway.rest;

import static java.util.Arrays.asList;
import static org.jboss.pnc.causeway.rest.Constants.IMPORT_PATH;

import org.commonjava.propulsor.deploy.resteasy.ResteasyAppConfig;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class CausewayResteasyAppConfig implements ResteasyAppConfig {

    @Override
    public List<String> getJaxRsMappings() {
        return asList(IMPORT_PATH + "/*");
    }
}
