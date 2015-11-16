package org.jboss.pnc.causeway.koji;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.JHttpCException;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.config.CausewayConfig;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jdcasey on 11/13/15.
 */
public class KojiTransportFactory
    implements XmlRpcTransportFactory
{
    @Inject
    private CausewayConfig config;

    @Inject
    private HttpFactory httpFactory;

    private XmlRpcClient client;

    protected KojiTransportFactory(){}

    public KojiTransportFactory( CausewayConfig config, HttpFactory factory )
    {
        this.config = config;
        this.httpFactory = factory;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        client = new XmlRpcClient();
        XmlRpcClientConfigImpl xConfig = new XmlRpcClientConfigImpl();

        URL url;
        try
        {
            url = new URL( config.getKojiURL() );
        }
        catch ( MalformedURLException e )
        {
            throw new KojiTransportRuntimeException( "Invalid Koji URL: " + config.getKojiURL() +". Reason: " + e.getMessage(), e );
        }

        xConfig.setServerURL( url );
        xConfig.setEnabledForExtensions( true );
        client.setConfig( xConfig );
        client.setTransportFactory( this );
    }

    @Override
    public XmlRpcTransport getTransport()
    {
        SiteConfig site;
        try
        {
            site = config.getKojiSiteConfig();
        }
        catch ( IOException e )
        {
            throw new KojiTransportRuntimeException( "Failed to construct site configuration instance: " + e.getMessage(), e );
        }

        CloseableHttpClient httpClient;
        try
        {
            httpClient = httpFactory.createClient( site );
        }
        catch ( JHttpCException e )
        {
            throw new IllegalStateException( "Failed to create HTTP client: " + e.getMessage(), e );
        }

        return new JHttpCTransport( client, site, httpClient );
    }

    public XmlRpcClient getClient()
    {
        return client;
    }
}
