package org.jboss.pnc.causeway.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.propulsor.boot.BootStatus;
import org.commonjava.propulsor.boot.Booter;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.util.UrlUtils;
import org.jboss.pnc.causeway.boot.CausewayBootOptions;
import org.jboss.pnc.causeway.test.spi.CausewayDriver;
import org.jboss.pnc.causeway.test.util.HttpCommands;
import org.junit.AfterClass;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 2/11/16.
 */
public class LocalTestDriver
        implements CausewayDriver
{
    private CausewayBootOptions options;

    private BootStatus bootStatus;

    private Booter booter;

    private PasswordManager passwordManager;

    private HttpFactory httpFactory;

    private SiteConfig siteConfig;

    private TemporaryFolder temp = new TemporaryFolder();

    private File configDir;

    @Override
    public void start()
            throws Exception
    {
        temp.create();

        configDir = temp.newFolder( "local-causeway-etc" );

        File mainConf = new File( configDir, "etc/main.conf" );
        mainConf.getParentFile().mkdirs();

        FileUtils.write( mainConf, "koji.url=https://koji.myco.com/kojihub\n"
                + "pncl.url=https://pncl.myco.com/\n"
                + "koji.client.pem.password = mypassword" );

        System.out.println(
                "Wrote configuration: " + mainConf + " with configuration:\n\n" + FileUtils.readFileToString(
                        mainConf ) );

        options = new CausewayBootOptions();
        options.setPort( -1 );
        options.setHomeDir( configDir.getAbsolutePath() );

        booter = new Booter();
        bootStatus = booter.start( options );

        if ( bootStatus == null )
        {
            fail( "No boot status" );
        }

        Throwable t = bootStatus.getError();
        if ( t != null )
        {
            throw new RuntimeException( "Failed to start Causeway test server.", t );
        }

        assertThat( bootStatus.isSuccess(), equalTo( true ) );

        passwordManager = new MemoryPasswordManager();
        siteConfig = new SiteConfigBuilder( "local-test", formatUrl().toString() ).build();
        httpFactory = new HttpFactory( passwordManager );
    }

    @Override
    public void stop()
            throws Exception
    {
        if ( booter != null && bootStatus != null && bootStatus.isSuccess() )
        {
            booter.stop();
        }

        temp.delete();
    }

    private void checkStarted()
    {
        if ( booter == null || bootStatus == null || !bootStatus.isSuccess() )
        {
            throw new RuntimeException( "Cannot execute; Causeway test server is not running." );
        }
    }

    @Override
    public int getPort()
    {
        checkStarted();
        return options.getPort();
    }

    @Override
    public String formatUrl( String... pathParts )
            throws MalformedURLException
    {
        checkStarted();
        return UrlUtils.buildUrl( String.format( "http://localhost:%d", getPort() ), pathParts );
    }

    @Override
    public HttpFactory getHttpFactory()
            throws Exception
    {
        return httpFactory;
    }

    @Override
    public SiteConfig getSiteConfig()
            throws Exception
    {
        return siteConfig;
    }

    @Override
    public PasswordManager getPasswordManager()
            throws Exception
    {
        return passwordManager;
    }

    @Override
    public void withNewHttpClient( HttpCommands commands )
            throws Exception
    {
        try (CloseableHttpClient client = httpFactory.createClient( siteConfig ))
        {
            commands.execute( this, client ).throwError();
        }
    }
}
