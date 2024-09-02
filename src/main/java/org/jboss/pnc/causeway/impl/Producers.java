/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.oidc.client.OidcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jboss.pnc.causeway.CausewayConfig;
import org.jboss.pnc.common.concurrent.HeartbeatScheduler;
import org.jboss.pnc.common.http.PNCHttpClient;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

@ApplicationScoped
public class Producers {
    private final HeartbeatScheduler heartbeatScheduler;
    private final PNCHttpClient httpClient;

    @Inject
    public Producers(
            ScheduledExecutorService executorService,
            OidcClient oidcClient,
            CausewayConfig causewayConfig,
            ObjectMapper objectMapper) {
        httpClient = new PNCHttpClient(objectMapper, causewayConfig.httpClientConfig());
        httpClient
                .setTokenSupplier(() -> oidcClient.getTokens().await().atMost(Duration.ofMinutes(1)).getAccessToken());
        heartbeatScheduler = new HeartbeatScheduler(executorService, httpClient);
    }

    @Produces
    public HeartbeatScheduler heartbeatScheduler() {
        return heartbeatScheduler;
    }

    @Produces
    public PNCHttpClient pncHttpClient() {
        return httpClient;
    }
}
