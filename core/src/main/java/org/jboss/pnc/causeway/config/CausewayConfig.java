/**
 * Copyright (C) 2015 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.config;

import org.apache.commons.io.FileUtils;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

import javax.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

/**
 * Created by jdcasey on 11/10/15.
 */
@ApplicationScoped
@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
public class CausewayConfig
{
    public static final String CAUSEWAY_CONFIG_DIR_SYSPROP = "causeway.config.dir";

    public static final String CLIENT_CERT_PEM_PASSWORD_OPTION = "koji.client.pem.password";

    public static final String PNCL_URL_OPTION = "pncl.url";

    public static final String KOJI_URL_OPTION = "koji.url";

    public static final String INDY_URL_OPTION = "indy.url";

    public static final String DEFAULT_CAUSEWAY_CONFIG = "/etc/causeway/main.conf";

    public static final String DEFAULT_SSL_SERVER_PEM_FILE = "server.pem";

    public static final String DEFAULT_SSL_CLIENT_PEM_FILE = "client.pem";

    public static final Boolean DEFAULT_SSL_TRUST_SELF_SIGNED = Boolean.FALSE;

    public static final String KOJI_SITE_ID = "koji";

    private static final String PNCL_SITE_ID = "pncl";

    private static final String SSL_SUBDIR = "ssl";

    private static final Integer DEFAULT_HTTP_TIMEOUT_SECS = Integer.valueOf( 10 );

    private boolean configured;

    private String kojiClientKeyCertificateFile;

    private String kojiClientCertificatePassword;

    private String kojiServerCertificateFile;

    private Boolean kojiTrustSelfSigned;

    private String kojiURL;

    private String indyURL;

    private String pnclURL;

    private File configDir;

    private Integer httpTimeout;

    private Integer kojiTimeout;

    private Integer pnclTimeout;

    private SiteConfig kojiSiteConfig;

    private SiteConfig pnclSiteConfig;

    public Boolean getKojiTrustSelfSigned()
    {
        return kojiTrustSelfSigned == null ? false : kojiTrustSelfSigned;
    }

    @ConfigName( "koji.trust.self-signed" )
    public void setKojiTrustSelfSigned( Boolean kojiTrustSelfSigned )
    {
        this.kojiTrustSelfSigned = kojiTrustSelfSigned == null ? DEFAULT_SSL_TRUST_SELF_SIGNED : kojiTrustSelfSigned;
    }

    public String getKojiClientCertificatePassword()
    {
        return kojiClientCertificatePassword;
    }

    @ConfigName( CausewayConfig.CLIENT_CERT_PEM_PASSWORD_OPTION )
    public void setKojiClientCertificatePassword( String kojiClientCertificatePassword )
    {
        this.kojiClientCertificatePassword = kojiClientCertificatePassword;
    }

    public String getKojiClientKeyCertificateFile()
    {
        checkConfigured();
        return kojiClientKeyCertificateFile == null ?
                Paths.get( getConfigDir().getPath(), SSL_SUBDIR,
                           DEFAULT_SSL_CLIENT_PEM_FILE ).toString() :
                kojiClientKeyCertificateFile;
    }

    @ConfigName( "koji.client.pem.file" )
    public void setKojiClientKeyCertificateFile( String kojiClientKeyCertificateFile )
    {
        this.kojiClientKeyCertificateFile = kojiClientKeyCertificateFile;
    }

    public String getKojiServerCertificateFile()
    {
        checkConfigured();
        return kojiServerCertificateFile == null ?
                Paths.get( getConfigDir().getPath(), SSL_SUBDIR,
                           DEFAULT_SSL_SERVER_PEM_FILE ).toString() :
                kojiServerCertificateFile;
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

    @ConfigName( CausewayConfig.KOJI_URL_OPTION )
    public void setKojiURL( String kojiURL )
    {
        this.kojiURL = kojiURL;
    }

    public String getPnclURL()
    {
        checkConfigured();
        return pnclURL;
    }

    @ConfigName( CausewayConfig.INDY_URL_OPTION )
    public void setIndyURL( String indyURL )
    {
        this.indyURL = indyURL;
    }

    public String getIndyURL()
    {
        checkConfigured();
        return indyURL;
    }

    @ConfigName( CausewayConfig.PNCL_URL_OPTION )
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

            builder.withRequestTimeoutSeconds( getKojiTimeout() );

            kojiSiteConfig = builder.build();
        }

        return kojiSiteConfig;
    }

    public synchronized SiteConfig getPnclSiteConfig()
    {
        if ( pnclSiteConfig == null )
        {
            SiteConfigBuilder builder = new SiteConfigBuilder( PNCL_SITE_ID, getPnclURL() );

            // TODO: SSL?

            builder.withRequestTimeoutSeconds( getPnclTimeout() );

            pnclSiteConfig = builder.build();
        }

        return pnclSiteConfig;
    }

    public void setConfigDir( File configDir )
    {
        this.configDir = configDir;
    }

    public File getConfigDir()
    {
        return configDir;
    }

    public Integer getHttpTimeout()
    {
        return httpTimeout == null ? DEFAULT_HTTP_TIMEOUT_SECS : httpTimeout;
    }

    @ConfigName( "http.timeout.secs" )
    public void setHttpTimeout( Integer httpTimeout )
    {
        this.httpTimeout = httpTimeout;
    }

    @ConfigName( "koji.timeout.secs" )
    public void setKojiTimeout( Integer kojiTimeout )
    {
        this.kojiTimeout = kojiTimeout;
    }

    public Integer getKojiTimeout()
    {
        return kojiTimeout == null ? getHttpTimeout() : kojiTimeout;
    }

    public Integer getPnclTimeout()
    {
        return pnclTimeout == null ? getHttpTimeout() : pnclTimeout;
    }

    @ConfigName( "pncl.timeout.secs" )
    public void setPnclTimeout( Integer pnclTimeout )
    {
        this.pnclTimeout = pnclTimeout;
    }

    public String getValidationErrors()
    {
        List<String> errors = new ArrayList<>();
        if ( isEmpty( getKojiClientCertificatePassword() ) )
        {
            errors.add( String.format( "Koji SSL password '%s' is required.", CLIENT_CERT_PEM_PASSWORD_OPTION ) );
        }

        if ( isEmpty( getPnclURL() ) )
        {
            errors.add( String.format( "Project Newcastle URL '%s' is required.", PNCL_URL_OPTION ) );
        }

        if ( isEmpty( getKojiURL() ) )
        {
            errors.add( String.format( "Koji URL '%s' is required.", KOJI_URL_OPTION ) );
        }

        if ( !errors.isEmpty() )
        {
            return join( errors, "\n" );
        }

        return null;
    }
}
