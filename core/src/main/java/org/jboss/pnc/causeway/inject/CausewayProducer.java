package org.jboss.pnc.causeway.inject;

import org.commonjava.rwx.binding.error.BindException;
import org.commonjava.rwx.binding.internal.reflect.ReflectionMapper;
import org.commonjava.rwx.binding.internal.xbr.XBRCompositionBindery;
import org.commonjava.rwx.binding.mapping.Mapping;
import org.commonjava.rwx.binding.spi.Bindery;
import org.commonjava.rwx.http.httpclient4.HC4SyncObjectClient;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.koji.model.KojiSessionInfo;
import org.jboss.pnc.causeway.koji.model.LoginRequest;
import org.jboss.pnc.causeway.koji.model.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Created by jdcasey on 11/10/15.
 */
@ApplicationScoped
public class CausewayProducer
    implements Closeable
{
    @Inject
    private CausewayConfig config;

    private HttpFactory httpFactory;

    private HC4SyncObjectClient xmlrpcClient;

    protected CausewayProducer(){}

    public CausewayProducer(CausewayConfig config){
        this.config = config;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        PasswordManager passwords = new MemoryPasswordManager();
        passwords.bind( config.getKojiClientCertificatePassword(), CausewayConfig.KOJI_SITE_ID, PasswordType.KEY );

        httpFactory = new HttpFactory( passwords );

    }

    @PreDestroy
    public void close()
            throws IOException
    {
        if ( httpFactory != null )
        {
            httpFactory.close();
        }
    }

    @Produces
    @Default
    public HC4SyncObjectClient getXmlrpcClient()
    {
        return xmlrpcClient;
    }

    @Produces
    @Default
    public HttpFactory getHttpFactory()
    {
        return httpFactory;
    }
}
