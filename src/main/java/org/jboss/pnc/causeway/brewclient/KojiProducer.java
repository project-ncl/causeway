/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import java.io.Closeable;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.causeway.CausewayConfig;
import org.jboss.pnc.causeway.ErrorMessages;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.config.KojiConfig;
import com.redhat.red.build.koji.config.SimpleKojiConfigBuilder;

import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class KojiProducer implements Closeable {

    private static final String KOJI_SITE_ID = "koji";

    @Inject
    CausewayConfig config;

    @Inject
    ManagedExecutor executorService;

    private KojiClient koji;

    private final PasswordManager passwords = new MemoryPasswordManager();

    private synchronized void setupKoji() {
        if (koji != null) {
            return;
        }
        passwords.bind(config.koji().clientCertificatePassword(), KOJI_SITE_ID, PasswordType.KEY);

        log.warn("config.koji().clientKeyCertificateFile(): " + config.koji().clientKeyCertificateFile());

        SimpleKojiConfigBuilder builder = new SimpleKojiConfigBuilder();
        builder.withKojiSiteId(KOJI_SITE_ID)
                .withKojiURL(config.koji().url())
                .withClientKeyCertificateFile(config.koji().clientKeyCertificateFile())
                .withKojiClientCertificatePassword(config.koji().clientCertificatePassword())
                .withServerCertificateFile("conf/ssl/server.pem")// config.koji().serverCertificateFile())
                .withTrustSelfSigned(config.koji().trustSelfSigned())
                .withTimeout(config.koji().timeout())
                .withConnectionPoolTimeout(config.koji().connectionPoolTimeout())
                .withMaxConnections(config.koji().connections());
        KojiConfig kc = builder.build();

        try {
            koji = new KojiClient(kc, passwords, executorService);
        } catch (KojiClientException ex) {
            throw new RuntimeException(ErrorMessages.canNotConnectToKoji(ex), ex);
        }
    }

    @PreDestroy
    public void close() {
        if (koji != null) {
            koji.close();
        }
    }

    @Produces
    public KojiClient getKojiClient() {
        if (koji == null) {
            setupKoji();
        }
        return koji;
    }

}
