package org.jboss.pnc.causeway.koji;

import org.apache.xmlrpc.XmlRpcException;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.config.CausewayConfig;

import javax.inject.Inject;
import java.util.Properties;

/**
 * Created by jdcasey on 11/10/15.
 */
public class KojiClient
{
    private static final String DEBUG_SSL = "javax.net.debug";

    private static final String DEBUG_SSL_VALUE = "ssl:handshake:verbose";

    @Inject
    private KojiTransportFactory transportFactory;

    @Inject
    private CausewayConfig config;

    protected KojiClient(){}

    public KojiClient( CausewayConfig config, KojiTransportFactory transportFactory )
    {
        this.config = config;
        this.transportFactory = transportFactory;
    }

    public void login()
            throws CausewayException
    {
        // javax.net.debug=ssl:handshake:verbose
        Properties sysprops = System.getProperties();
        String old = sysprops.getProperty( DEBUG_SSL );
        try
        {
            sysprops.setProperty( DEBUG_SSL, DEBUG_SSL_VALUE );
            System.setProperties( sysprops );
            transportFactory.getClient().execute( "sslLogin", new Object[] {null} );
        }
        catch ( XmlRpcException | KojiTransportRuntimeException e )
        {
            throw new CausewayException( "Failed to execute sslLogin method: %s", e, e.getMessage() );
        }
        finally
        {
            if ( old != null )
            {
                sysprops.setProperty( DEBUG_SSL, old );
                System.setProperties( sysprops );
            }
        }
    }
}
