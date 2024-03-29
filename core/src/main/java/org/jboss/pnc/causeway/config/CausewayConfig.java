/**
 * Copyright (C) 2015 Red Hat, Inc. (jbrazdil@redhat.com)
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
import org.jboss.pnc.causeway.ErrorMessages;
import org.jboss.pnc.client.Configuration;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Created by jdcasey on 11/10/15.
 */
@ApplicationScoped
@SectionName(ConfigurationSectionListener.DEFAULT_SECTION)
public class CausewayConfig {
    public static final String CAUSEWAY_CONFIG_DIR_SYSPROP = "causeway.config.dir";

    public static final String CLIENT_CERT_PEM_PASSWORD_OPTION = "koji.client.pem.password";

    public static final String PNCL_URL_OPTION = "pncl.url";

    public static final String PNCL_BUILDS_URL_OPTION = "pncl.builds.url";

    public static final String OIDC_CLIENT_URL = "oidc-client.url";
    public static final String OIDC_CLIENT_REALM = "oidc-client.realm";
    public static final String OIDC_CLIENT_CLIENT_ID = "oidc-client.client-id";
    public static final String OIDC_CLIENT_SECRET_FILE = "oidc-client.secret-file";

    public static final String PNC_SYSTEM_VERSION = "pnc.system.version";

    public static final String ARTIFACT_STORAGE = "artifact-storage";

    public static final String LOG_STORAGE = "log-storage";

    public static final String KOJI_URL_OPTION = "koji.url";

    public static final String KOJI_WEBURL_OPTION = "koji.weburl";

    public static final String DEFAULT_CAUSEWAY_CONFIG = "/etc/causeway/main.conf";

    public static final String DEFAULT_SSL_SERVER_PEM_FILE = "server.pem";

    public static final String DEFAULT_SSL_CLIENT_PEM_FILE = "client.pem";

    public static final Boolean DEFAULT_SSL_TRUST_SELF_SIGNED = Boolean.FALSE;

    public static final String KOJI_SITE_ID = "koji";

    private static final String PNCL_SITE_ID = "pncl";

    private static final String SSL_SUBDIR = "ssl";

    private static final Integer DEFAULT_HTTP_TIMEOUT_SECS = Integer.valueOf(10);

    private static final Integer DEFAULT_CONNECTIONS = 10;

    private static final Integer DEFAULT_PAGE_SIZE = 50;

    private boolean configured;

    private String kojiClientKeyCertificateFile;

    private String kojiClientCertificatePassword;

    private String kojiServerCertificateFile;

    private Boolean kojiTrustSelfSigned;

    private String kojiURL;

    private String kojiWebURL;

    private String artifactStorage;

    private String logStorage;

    private String pnclURL;

    private String pnclBuildsURL;

    private String pncSystemVersion;

    private File configDir;

    private Integer httpTimeout;

    private Integer kojiTimeout;

    private Integer kojiConnectionPoolTimeout;

    private Integer kojiConnections;

    private Set<String> ignoredTools;

    private Integer pnclTimeout;

    private String oidcClientUrl;

    private String oidcClientRealm;

    private String oidcClientClientId;

    private String oidcClientSecretFile;

    /**
     * Cache the content of the oidcClientSecretFile
     */
    private String cachedSecretFileContent;

    private SiteConfig kojiSiteConfig;

    private SiteConfig pnclSiteConfig;

    private Configuration configuration;

    public Boolean getKojiTrustSelfSigned() {
        return kojiTrustSelfSigned == null ? false : kojiTrustSelfSigned;
    }

    @ConfigName("koji.trust.self-signed")
    public void setKojiTrustSelfSigned(Boolean kojiTrustSelfSigned) {
        this.kojiTrustSelfSigned = kojiTrustSelfSigned == null ? DEFAULT_SSL_TRUST_SELF_SIGNED : kojiTrustSelfSigned;
    }

    public String getKojiClientCertificatePassword() {
        return kojiClientCertificatePassword;
    }

    @ConfigName(CausewayConfig.CLIENT_CERT_PEM_PASSWORD_OPTION)
    public void setKojiClientCertificatePassword(String kojiClientCertificatePassword) {
        this.kojiClientCertificatePassword = kojiClientCertificatePassword;
    }

    public String getKojiClientKeyCertificateFile() {
        checkConfigured();
        return kojiClientKeyCertificateFile == null
                ? Paths.get(getConfigDir().getPath(), SSL_SUBDIR, DEFAULT_SSL_CLIENT_PEM_FILE).toString()
                : kojiClientKeyCertificateFile;
    }

    @ConfigName("koji.client.pem.file")
    public void setKojiClientKeyCertificateFile(String kojiClientKeyCertificateFile) {
        this.kojiClientKeyCertificateFile = kojiClientKeyCertificateFile;
    }

    public String getKojiServerCertificateFile() {
        checkConfigured();
        return kojiServerCertificateFile == null
                ? Paths.get(getConfigDir().getPath(), SSL_SUBDIR, DEFAULT_SSL_SERVER_PEM_FILE).toString()
                : kojiServerCertificateFile;
    }

    @ConfigName("koji.server.pem.file")
    public void setKojiServerCertificateFile(String kojiServerCertificateFile) {
        this.kojiServerCertificateFile = kojiServerCertificateFile;
    }

    public String getKojiURL() {
        checkConfigured();
        return kojiURL;
    }

    @ConfigName(CausewayConfig.KOJI_URL_OPTION)
    public void setKojiURL(String kojiURL) {
        this.kojiURL = kojiURL;
    }

    public String getKojiWebURL() {
        checkConfigured();
        return kojiWebURL;
    }

    @ConfigName(CausewayConfig.KOJI_WEBURL_OPTION)
    public void setKojiWebURL(String kojiWebURL) {
        this.kojiWebURL = kojiWebURL;
    }

    public String getArtifactStorage() {
        checkConfigured();
        return artifactStorage;
    }

    @ConfigName(CausewayConfig.ARTIFACT_STORAGE)
    public void setArtifactStorage(String artifactStorage) {
        this.artifactStorage = artifactStorage;
    }

    public String getLogStorage() {
        checkConfigured();
        return logStorage;
    }

    @ConfigName(CausewayConfig.LOG_STORAGE)
    public void setLogStorage(String logStorage) {
        this.logStorage = logStorage;
    }

    public String getPnclURL() {
        checkConfigured();
        return pnclURL;
    }

    @ConfigName(CausewayConfig.PNCL_URL_OPTION)
    public void setPnclURL(String pnclURL) {
        this.pnclURL = pnclURL;
    }

    public String getPnclBuildsURL() {
        checkConfigured();
        return pnclBuildsURL;
    }

    @ConfigName(CausewayConfig.PNCL_BUILDS_URL_OPTION)
    public void setPnclBuildsURL(String pnclURL) {
        this.pnclBuildsURL = pnclURL;
    }

    public String getPNCSystemVersion() {
        checkConfigured();
        return pncSystemVersion;
    }

    @ConfigName(CausewayConfig.PNC_SYSTEM_VERSION)
    public void setPNCSystemVersion(String pncSystemVersion) {
        this.pncSystemVersion = pncSystemVersion;
    }

    public void configurationDone() {
        configured = true;
    }

    private void checkConfigured() {
        if (!configured) {
            throw new IllegalStateException(ErrorMessages.causewayNotConfigured());
        }
    }

    public synchronized SiteConfig getKojiSiteConfig() throws IOException {
        if (kojiSiteConfig == null) {
            SiteConfigBuilder builder = new SiteConfigBuilder(KOJI_SITE_ID, getKojiURL());
            File keyCert = new File(getKojiClientKeyCertificateFile());
            if (keyCert.exists()) {
                builder.withKeyCertPem(FileUtils.readFileToString(keyCert));
            }

            File serverCert = new File(getKojiServerCertificateFile());
            if (serverCert.exists()) {
                builder.withServerCertPem(FileUtils.readFileToString(serverCert));
            }

            if (getKojiTrustSelfSigned()) {
                builder.withTrustType(SiteTrustType.TRUST_SELF_SIGNED);
            }

            builder.withRequestTimeoutSeconds(getKojiTimeout());

            kojiSiteConfig = builder.build();
        }

        return kojiSiteConfig;
    }

    public synchronized SiteConfig getPnclSiteConfig() {
        if (pnclSiteConfig == null) {
            SiteConfigBuilder builder = new SiteConfigBuilder(PNCL_SITE_ID, getPnclURL());

            // TODO: SSL?

            builder.withRequestTimeoutSeconds(getPnclTimeout());

            pnclSiteConfig = builder.build();
        }

        return pnclSiteConfig;
    }

    public synchronized Configuration getPncClientConfig() {
        if (configuration == null) {
            Configuration.ConfigurationBuilder builder = Configuration.builder();

            try {
                URL url = new URL(getPnclURL());

                builder.host(url.getHost())
                        .protocol(url.getProtocol())
                        .pageSize(DEFAULT_PAGE_SIZE)
                        .addDefaultMdcToHeadersMappings()
                        .port((url.getPort() != -1) ? url.getPort() : url.getDefaultPort());
            } catch (MalformedURLException e) {
                throw new IllegalStateException(ErrorMessages.configurationValueIsNotURL(PNCL_URL_OPTION), e);
            }

            configuration = builder.build();
        }

        return configuration;
    }

    public void setConfigDir(File configDir) {
        this.configDir = configDir;
    }

    public File getConfigDir() {
        return configDir;
    }

    public Integer getHttpTimeout() {
        return httpTimeout == null ? DEFAULT_HTTP_TIMEOUT_SECS : httpTimeout;
    }

    @ConfigName("http.timeout.secs")
    public void setHttpTimeout(Integer httpTimeout) {
        this.httpTimeout = httpTimeout;
    }

    @ConfigName("koji.timeout.secs")
    public void setKojiTimeout(Integer kojiTimeout) {
        this.kojiTimeout = kojiTimeout;
    }

    public Integer getKojiTimeout() {
        return kojiTimeout == null ? getHttpTimeout() : kojiTimeout;
    }

    @ConfigName("koji.connectionPoolTimeout.secs")
    public void setKojiConnectionPoolTimeout(Integer kojiConnectionPoolTimeout) {
        this.kojiConnectionPoolTimeout = kojiConnectionPoolTimeout;
    }

    public Integer getKojiConnectionPoolTimeout() {
        return kojiConnectionPoolTimeout == null ? getHttpTimeout() : kojiConnectionPoolTimeout;
    }

    @ConfigName("koji.connections")
    public void setKojiConnections(Integer kojiConnections) {
        this.kojiConnections = kojiConnections;
    }

    public Integer getKojiConnections() {
        return kojiConnections == null ? DEFAULT_CONNECTIONS : kojiConnections;
    }

    public Integer getPnclTimeout() {
        return pnclTimeout == null ? getHttpTimeout() : pnclTimeout;
    }

    @ConfigName("pncl.timeout.secs")
    public void setPnclTimeout(Integer pnclTimeout) {
        this.pnclTimeout = pnclTimeout;
    }

    public Set<String> getIgnoredTools() {
        return ignoredTools == null ? Collections.emptySet() : ignoredTools;
    }

    @ConfigName("pnc.tools.ignored")
    public void setIgnoredTools(String ignoredTools) {
        this.ignoredTools = ignoredTools == null ? null : new HashSet<>(Arrays.asList(ignoredTools.split(",")));
    }

    public String getOidcClientUrl() {
        return oidcClientUrl;
    }

    @ConfigName(CausewayConfig.OIDC_CLIENT_URL)
    public void setOidcClientUrl(String clientUrl) {
        this.oidcClientUrl = clientUrl;
    }

    public String getOidcClientRealm() {
        return oidcClientRealm;
    }

    @ConfigName(CausewayConfig.OIDC_CLIENT_REALM)
    public void setOidcClientRealm(String clientRealm) {
        this.oidcClientRealm = clientRealm;
    }

    public String getOidcClientClientId() {
        return oidcClientClientId;
    }

    @ConfigName(CausewayConfig.OIDC_CLIENT_CLIENT_ID)
    public void setOidcClientClientId(String clientId) {
        this.oidcClientClientId = clientId;
    }

    public String getOidcClientSecretFile() {
        return oidcClientSecretFile;
    }

    @ConfigName(CausewayConfig.OIDC_CLIENT_SECRET_FILE)
    public void setOidcClientSecretFile(String secretFile) {
        this.oidcClientSecretFile = secretFile;
    }

    public String getOidcClientSecret() throws IOException {
        if (cachedSecretFileContent == null) {
            List<String> lines = Files.readAllLines(Paths.get(oidcClientSecretFile));
            cachedSecretFileContent = String.join("\n", lines).trim();
        }

        return cachedSecretFileContent;
    }

    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        if (isEmpty(getKojiClientCertificatePassword())) {
            errors.add(String.format("Koji SSL password '%s' is required.", CLIENT_CERT_PEM_PASSWORD_OPTION));
        }

        if (isEmpty(getPnclURL())) {
            errors.add(String.format("Project Newcastle URL '%s' is required.", PNCL_URL_OPTION));
        }

        if (isEmpty(getKojiURL())) {
            errors.add(String.format("Koji URL '%s' is required.", KOJI_URL_OPTION));
        }

        if (isEmpty(getKojiWebURL())) {
            errors.add(String.format("Koji Web URL '%s' is required.", KOJI_WEBURL_OPTION));
        }
        return errors;
    }
}
