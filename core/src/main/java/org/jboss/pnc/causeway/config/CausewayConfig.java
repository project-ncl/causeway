package org.jboss.pnc.causeway.config;

import org.apache.commons.io.FileUtils;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

import java.io.File;
import java.io.IOException;

/**
 * Created by jdcasey on 11/10/15.
 */
@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
public class CausewayConfig
{

    public static final String KOJI_SITE_ID = "koji";

    private boolean configured;

    private String kojiClientKeyCertificateFile;

    private String kojiClientCertificatePassword;

    private String kojiServerCertificateFile;

    private Boolean kojiTrustSelfSigned;

    private String kojiURL;

    private String pnclURL;

    private File configDir;

    private SiteConfig kojiSiteConfig;

    public Boolean getKojiTrustSelfSigned()
    {
        return kojiTrustSelfSigned;
    }

    @ConfigName( "koji.trust.self-signed" )
    public void setKojiTrustSelfSigned( Boolean kojiTrustSelfSigned )
    {
        this.kojiTrustSelfSigned = kojiTrustSelfSigned;
    }

    public String getKojiClientCertificatePassword()
    {
        return kojiClientCertificatePassword;
    }

    @ConfigName( "koji.client.pem.password" )
    public void setKojiClientCertificatePassword( String kojiClientCertificatePassword )
    {
        this.kojiClientCertificatePassword = kojiClientCertificatePassword;
    }

    public String getKojiClientKeyCertificateFile()
    {
        checkConfigured();
        return kojiClientKeyCertificateFile;
    }

    @ConfigName( "koji.client.pem.file" )
    public void setKojiClientKeyCertificateFile( String kojiClientKeyCertificateFile )
    {
        this.kojiClientKeyCertificateFile = kojiClientKeyCertificateFile;
    }

    public String getKojiServerCertificateFile()
    {
        checkConfigured();
        return kojiServerCertificateFile;
    }

    @ConfigName( "koji.server.pem.file" )
    public void setKojiServerCertificateFile( String kojiServerCertificateFile )
    {
        this.kojiServerCertificateFile = kojiServerCertificateFile;
    }

    public String getKojiURL()
    {
        checkConfigured();
        return kojiURL;
    }

    @ConfigName( "koji.url" )
    public void setKojiURL( String kojiURL )
    {
        this.kojiURL = kojiURL;
    }

    public String getPnclURL()
    {
        checkConfigured();
        return pnclURL;
    }

    @ConfigName( "pncl.url" )
    public void setPnclURL( String pnclURL )
    {
        this.pnclURL = pnclURL;
    }

    public void configurationDone()
    {
        configured = true;
    }

    private void checkConfigured()
    {
        if ( !configured )
        {
            throw new IllegalStateException( "The causeway system has not been configured! "
                                                     + "This is a sign that something is in the wrong order in the boot sequence!!" );
        }
    }

    public synchronized SiteConfig getKojiSiteConfig()
            throws IOException
    {
        if ( kojiSiteConfig == null )
        {
            SiteConfigBuilder builder = new SiteConfigBuilder( KOJI_SITE_ID, getKojiURL() );
            File keyCert = new File( getKojiClientKeyCertificateFile() );
            if ( keyCert.exists() )
            {
                builder.withKeyCertPem( FileUtils.readFileToString( keyCert ) );
            }

            File serverCert = new File( getKojiServerCertificateFile() );
            if ( serverCert.exists() )
            {
                builder.withServerCertPem( FileUtils.readFileToString( serverCert ) );
            }

            if ( getKojiTrustSelfSigned() )
            {
                builder.withTrustType( SiteTrustType.TRUST_SELF_SIGNED );
            }

            kojiSiteConfig = builder.build();
        }

        return kojiSiteConfig;
    }

    public void setConfigDir( File configDir )
    {
        this.configDir = configDir;
    }

    public File getConfigDir()
    {
        return configDir;
    }
}
