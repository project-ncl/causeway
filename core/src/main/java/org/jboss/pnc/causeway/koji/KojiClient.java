package org.jboss.pnc.causeway.koji;

import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.JHttpCException;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.config.CausewayConfig;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Created by jdcasey on 11/10/15.
 */
public class KojiClient
{
    @Inject
    private HttpFactory httpFactory;

    @Inject
    private CausewayConfig config;

    public void login()
            throws CausewayException
    {
        CloseableHttpClient client = getClient();
    }

    private CloseableHttpClient getClient()
            throws CausewayException
    {
        SiteConfig siteConfig;
        try
        {
            siteConfig = config.getKojiSiteConfig();
        }
        catch ( IOException e )
        {
            throw new CausewayException( "Failed to initialize Koji client configuration: %s", e, e.getMessage() );
        }

        try
        {
            return httpFactory.createClient( siteConfig );
        }
        catch ( JHttpCException e )
        {
            throw new CausewayException( "Failed to setup HTTP client for use with Koji: %s", e, e.getMessage() );
        }
    }
}
