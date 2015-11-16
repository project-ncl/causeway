package org.jboss.pnc.causeway.koji;

import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jdcasey on 11/13/15.
 */
public class KojiClientTest
{

    private KojiClient client;

    @Before
    public void setup()
            throws Exception
    {
        CausewayConfig config = new CausewayConfig();
//        config.setKojiURL( "https://brew-test.devel.redhat.com/kojihub/ssllogin" );
        config.setKojiURL( "https://brewhub.stage.engineering.redhat.com/brewhub/ssllogin" );
        config.setKojiClientCertificatePassword( "foobar" );
        config.setKojiClientKeyCertificateFile( "/home/jdcasey/.koji/brew-stage/jcasey-http-test.intermed.pem" );
        config.setKojiClientCertificatePassword( "password" );
        config.setKojiServerCertificateFile( "/home/jdcasey/.koji/brew-stage/server.crt" );
//        config.setKojiClientKeyCertificateFile( "/home/jdcasey/.koji/newcastle.passwd.pem" );
//        config.setKojiServerCertificateFile( "/home/jdcasey/.koji/brew-test_ca.crt" );
        config.setKojiTimeout(30);
        config.configurationDone();

        PasswordManager passwords = new MemoryPasswordManager();
        passwords.bind( config.getKojiClientCertificatePassword(), CausewayConfig.KOJI_SITE_ID, PasswordType.KEY );

        HttpFactory httpFactory = new HttpFactory( passwords );

        client = new KojiClient( config, new KojiTransportFactory( config, httpFactory ) );
    }

    @Test
    public void login()
            throws Exception
    {
        client.login();
    }
}
