/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.impl;

import java.util.concurrent.ScheduledExecutorService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.pnc.causeway.CausewayConfig;
import org.jboss.pnc.common.concurrent.HeartbeatScheduler;
import org.jboss.pnc.common.http.PNCHttpClient;
import org.jboss.pnc.quarkus.client.auth.runtime.PNCClientAuth;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class Producers {
    private final HeartbeatScheduler heartbeatScheduler;
    private final PNCHttpClient httpClient;

    @Inject
    public Producers(
            ScheduledExecutorService executorService,
            PNCClientAuth pncClientAuth,
            CausewayConfig causewayConfig,
            ObjectMapper objectMapper) {
        httpClient = new PNCHttpClient(objectMapper, causewayConfig.httpClientConfig());
        httpClient.setAuthValueSupplier(pncClientAuth::getHttpAuthorizationHeaderValue);
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
