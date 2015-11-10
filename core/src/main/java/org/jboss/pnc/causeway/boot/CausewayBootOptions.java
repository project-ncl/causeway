package org.jboss.pnc.causeway.boot;

import org.commonjava.propulsor.boot.BootOptions;

/**
 * Created by jdcasey on 11/10/15.
 */
public class CausewayBootOptions
    extends BootOptions
{
    public static final String DEFAULT_CAUSEWAY_CONFIG = "/etc/causeway/main.conf";

    @Override
    public String getHomeSystemProperty()
    {
        return "causeway.home";
    }

    @Override
    public String getConfigSystemProperty()
    {
        return "causeway.config";
    }

    @Override
    public String getHomeEnvar()
    {
        return "CAUSEWAY_HOME";
    }
}
