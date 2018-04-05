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
package org.jboss.pnc.causeway.inject;

import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.jboss.pnc.causeway.config.CausewayConfig;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import java.io.Closeable;
import java.util.logging.Logger;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.config.KojiConfig;
import com.redhat.red.build.koji.config.SimpleKojiConfigBuilder;

/**
 * Created by jdcasey on 11/10/15.
 */
@ApplicationScoped
public class CausewayProducer
        implements Closeable
{

    private final Logger logger = Logger.getLogger(CausewayProducer.class.getName());

    private final CausewayConfig config;

    @Resource
    private ManagedExecutorService executorService;

    private KojiClient koji;

    private final PasswordManager passwords = new MemoryPasswordManager();

    @Inject
    public CausewayProducer( CausewayConfig config )
    {
        this.config = config;
        passwords.bind(config.getKojiClientCertificatePassword(), CausewayConfig.KOJI_SITE_ID, PasswordType.KEY);
    }

    private synchronized void setupKoji(PasswordManager passwords) {
        if (koji != null) {
            return;
        }

        SimpleKojiConfigBuilder builder = new SimpleKojiConfigBuilder();
        builder.withKojiSiteId("koji")
                .withKojiURL(config.getKojiURL())
                .withClientKeyCertificateFile(config.getKojiClientKeyCertificateFile())
                .withKojiClientCertificatePassword(config.getKojiClientCertificatePassword())
                .withServerCertificateFile(config.getKojiServerCertificateFile())
                .withTrustSelfSigned(config.getKojiTrustSelfSigned())
                .withTimeout(config.getKojiTimeout())
                .withConnectionPoolTimeout(config.getKojiConnectionPoolTimeout())
                .withMaxConnections(config.getKojiConnections());

        KojiConfig kc = builder.build();

        try {
            koji = new KojiClient(kc, passwords, executorService);
        } catch (KojiClientException ex) {
            throw new RuntimeException("Couldn't connect to Koji.", ex);
        }
    }

    @PreDestroy
    public void close()
    {
        if(koji != null){
            koji.close();
        }
    }

    @Produces
    @Default
    public KojiClient getKojiClient()
    {
        if (koji == null) {
            setupKoji(passwords);
        }
        return koji;
    }

}
