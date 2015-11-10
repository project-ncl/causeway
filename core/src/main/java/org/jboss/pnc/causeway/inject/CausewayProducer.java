package org.jboss.pnc.causeway.inject;

import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.jboss.pnc.causeway.config.CausewayConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;

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
    public HttpFactory getHttpFactory()
    {
        return httpFactory;
    }
}
