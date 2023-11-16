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
package org.jboss.pnc.causeway.bpmclient;

import org.jboss.pnc.api.constants.HttpHeaders;
import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.causeway.authentication.KeycloakClient;
import org.jboss.pnc.causeway.authentication.KeycloakClientException;
import org.jboss.pnc.causeway.rest.BrewPushMilestoneResult;
import org.jboss.pnc.causeway.rest.Callback;
import org.jboss.pnc.causeway.rest.pnc.MilestoneReleaseResultRest;
import org.jboss.pnc.common.log.MDCUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.MDC;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@ApplicationScoped
@Deprecated
@Slf4j
public class BPMClientImpl implements BPMClient {
    private final ResteasyClient client;

    @Inject
    private KeycloakClient keycloakClient;

    public BPMClientImpl() {
        client = new ResteasyClientBuilder().connectionPoolSize(4).build();
    }

    private synchronized void send(Request callback, BrewPushMilestoneResult result) {
        log.info("Will send callback to {} using http method: {}", callback.getUri(), callback.getMethod());

        ResteasyWebTarget target = client.target(callback.getUri());
        Invocation.Builder requestBuilder = target.request(MediaType.APPLICATION_JSON);
        callback.getHeaders().forEach(h -> requestBuilder.header(h.getName(), h.getValue()));

        try {
            requestBuilder.header(HttpHeaders.AUTHORIZATION_STRING, "Bearer " + keycloakClient.getAccessToken());
        } catch (KeycloakClientException e) {
            log.error("Couldn't obtain the access token from the OIDC server", e);
        }

        // Add OTEL headers from MDC context
        addOtelMDCHeaders(requestBuilder);

        Response response = requestBuilder
                .method(callback.getMethod().toString(), Entity.entity(result, MediaType.APPLICATION_JSON_TYPE));
        log.info(
                "Callback sent to {} using http method: {}, received status: {} from location: {}",
                callback.getUri(),
                callback.getMethod(),
                response.getStatus(),
                response.getLocation());
    }

    private void addOtelMDCHeaders(Invocation.Builder request) {
        headersFromMdc(request, MDCHeaderKeys.SLF4J_TRACE_ID);
        headersFromMdc(request, MDCHeaderKeys.SLF4J_SPAN_ID);
        Map<String, String> otelHeaders = MDCUtils.getOtelHeadersFromMDC();
        otelHeaders.forEach((k, v) -> request.header(k, v));
    }

    private void headersFromMdc(Invocation.Builder request, MDCHeaderKeys headerKey) {
        String mdcValue = MDC.get(headerKey.getMdcKey());
        if (mdcValue != null && mdcValue.isEmpty()) {
            request.header(headerKey.getHeaderName(), mdcValue.trim());
        }
    }

    @Override
    public void success(Request callbackTarget, String callbackId, MilestoneReleaseResultRest result) {
        log.info("Import of milestone {} ended with success.", result.getMilestoneId());
        Callback callback = new Callback(callbackId, 200);
        send(callbackTarget, new BrewPushMilestoneResult(result, callback));
    }

    @Override
    public void error(Request callbackTarget, String callbackId, MilestoneReleaseResultRest result) {
        log.info("Import of milestone {} ended with error.", result.getMilestoneId());
        Callback callback = new Callback(callbackId, 500);
        send(callbackTarget, new BrewPushMilestoneResult(result, callback));
    }

    @Override
    public void failure(Request callbackTarget, String callbackId, MilestoneReleaseResultRest result) {
        log.info("Import of milestone {} ended with failure.", result.getMilestoneId());
        Callback callback = new Callback(callbackId, 418);
        send(callbackTarget, new BrewPushMilestoneResult(result, callback));
    }

}
