package org.jboss.pnc.causeway.koji;

import org.commonjava.rwx.error.XmlRpcException;
import org.commonjava.rwx.http.RequestModifier;
import org.commonjava.rwx.http.UrlBuilder;
import org.commonjava.rwx.http.httpclient4.HC4SyncObjectClient;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.util.UrlUtils;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.koji.model.KojiBindery;
import org.jboss.pnc.causeway.koji.model.KojiSessionInfo;
import org.jboss.pnc.causeway.koji.model.KojiUserInfo;
import org.jboss.pnc.causeway.koji.model.LoggedInUserRequest;
import org.jboss.pnc.causeway.koji.model.LoggedInUserResponse;
import org.jboss.pnc.causeway.koji.model.LoginRequest;
import org.jboss.pnc.causeway.koji.model.LoginResponse;
import org.jboss.pnc.causeway.koji.model.LogoutRequest;
import org.jboss.pnc.causeway.koji.model.LogoutResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jdcasey on 12/3/15.
 */
@ApplicationScoped
public class KojiClient
{
    private static final String SSL_LOGIN_PATH = "ssllogin";

    private static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";

    private static final String IDENTITY_ENCODING_VALUE = "identity";

    private static final String SESSION_ID_PARAM = "session-id";

    private static final String SESSION_KEY_PARAM = "session-key";

    private static final String CALL_NUMBER_PARAM = "callnum";

    private HC4SyncObjectClient xmlrpcClient;

    @Inject
    private HttpFactory httpFactory;

    @Inject
    private CausewayConfig config;

    @Inject
    private KojiBindery bindery;

    private AtomicInteger callCount = new AtomicInteger( 0 );

    private static final RequestModifier STANDARD_REQUEST_MODIFIER = (request)->{
        request.setHeader( ACCEPT_ENCODING_HEADER, IDENTITY_ENCODING_VALUE );
    };

    private UrlBuilder standardUrlBuilder( KojiSessionInfo session )
    {
        return (url)-> {
            Map<String, String> params = new HashMap<>();
            params.put( SESSION_ID_PARAM, Integer.toString( session.getSessionId() ) );
            params.put( SESSION_KEY_PARAM, session.getSessionKey() );
            params.put( CALL_NUMBER_PARAM, Integer.toString( callCount.getAndIncrement() ) );

            return UrlUtils.buildUrl( url, params );
        };
    }

    protected KojiClient()
    {
    }

    public KojiClient( CausewayConfig config, KojiBindery bindery, HttpFactory httpFactory )
    {
        this.config = config;
        this.bindery = bindery;
        this.httpFactory = httpFactory;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        try
        {
            xmlrpcClient = new HC4SyncObjectClient( httpFactory, bindery, config.getKojiSiteConfig() );
        }
        catch ( IOException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Cannot construct koji HTTP site-config: " + e.getMessage(), e );
        }
    }

    public KojiSessionInfo login()
            throws KojiClientException
    {
        try
        {
            LoginResponse loginResponse = xmlrpcClient.call( new LoginRequest(), LoginResponse.class, ( url ) -> {
                return UrlUtils.buildUrl( url, SSL_LOGIN_PATH );
            }, ( request ) -> request.setHeader( ACCEPT_ENCODING_HEADER, IDENTITY_ENCODING_VALUE ) );

            return loginResponse == null ? null : loginResponse.getSessionInfo();
        }
        catch ( XmlRpcException e )
        {
            throw new KojiClientException( "Failed to login: %s", e, e.getMessage() );
        }
    }

    public KojiUserInfo getUserInfo( KojiSessionInfo session )
            throws KojiClientException
    {
        try
        {
            LoggedInUserResponse response = xmlrpcClient.call( new LoggedInUserRequest(), LoggedInUserResponse.class,
                                                               standardUrlBuilder( session ), STANDARD_REQUEST_MODIFIER );

            return response == null ? null : response.getUserInfo();
        }
        catch ( XmlRpcException e )
        {
            throw new KojiClientException( "Failed to retrieve current user info: %s", e, e.getMessage() );
        }
    }

    public void logout( KojiSessionInfo session )
            throws KojiClientException
    {
        try
        {
            LogoutResponse response = xmlrpcClient.call( new LogoutRequest(), LogoutResponse.class,
                                                               standardUrlBuilder( session ), STANDARD_REQUEST_MODIFIER );
        }
        catch ( XmlRpcException e )
        {
            throw new KojiClientException( "Failed to logout: %s", e, e.getMessage() );
        }
    }

}
