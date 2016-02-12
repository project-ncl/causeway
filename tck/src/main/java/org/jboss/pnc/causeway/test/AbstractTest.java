package org.jboss.pnc.causeway.test;

import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.jboss.pnc.causeway.test.spi.CausewayDriver;
import org.jboss.pnc.causeway.test.util.HttpCommands;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ServiceLoader;

/**
 * Created by jdcasey on 2/11/16.
 */
public class AbstractTest
{
    private static CausewayDriver driver;

    @BeforeClass
    public static void startup()
            throws Exception
    {
        ServiceLoader<CausewayDriver> loader = ServiceLoader.load( CausewayDriver.class );
        driver = loader.iterator().next();

        driver.start();
    }

    @AfterClass
    public static void shutdown()
            throws Exception
    {
        if ( driver != null )
        {
            driver.stop();
        }
    }

    protected int getPort()
    {
        return driver.getPort();
    }

    protected String formatUrl( String... pathParts )
            throws MalformedURLException
    {
        return driver.formatUrl( pathParts );
    }

    protected HttpFactory getHttpFactory()
            throws Exception
    {
        return driver.getHttpFactory();
    }

    protected SiteConfig getSiteConfig()
            throws Exception
    {
        return driver.getSiteConfig();
    }

    protected PasswordManager getPasswordManager()
            throws Exception
    {
        return driver.getPasswordManager();
    }

    protected void withNewHttpClient( HttpCommands commands )
            throws Exception
    {
        driver.withNewHttpClient( commands );
    }
}
