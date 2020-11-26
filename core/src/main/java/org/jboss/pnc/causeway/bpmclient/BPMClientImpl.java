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

import org.jboss.pnc.causeway.rest.BrewPushMilestoneResult;
import org.jboss.pnc.causeway.rest.Callback;
import org.jboss.pnc.causeway.rest.pnc.MilestoneReleaseResultRest;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@ApplicationScoped
@Deprecated
@Slf4j
public class BPMClientImpl implements BPMClient {
    private final ResteasyClient client;

    public BPMClientImpl() {
        client = new ResteasyClientBuilder().connectionPoolSize(4).build();
    }

    private synchronized void send(
            String url,
            Map<String, String> headers,
            String httpMethod,
            BrewPushMilestoneResult result) {
        log.info("Will send callback to {} using http method: {}", url, httpMethod);

        ResteasyWebTarget target = client.target(url);
        target.request(MediaType.APPLICATION_JSON)
                .headers(toMultivaluedMap(headers))
                .method(httpMethod, Entity.entity(result, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public void success(String url, String callbackId, MilestoneReleaseResultRest result) {
        success(url, Collections.EMPTY_MAP, "POST", callbackId, result);
    }

    @Override
    public void success(
            String url,
            Map<String, String> headers,
            String httpMethod,
            String callbackId,
            MilestoneReleaseResultRest result) {
        log.info("Import of milestone {} ended with success.", result.getMilestoneId());
        Callback callback = new Callback(callbackId, 200);
        send(url, headers, httpMethod, new BrewPushMilestoneResult(result, callback));
    }

    @Override
    public void error(String url, String callbackId, MilestoneReleaseResultRest result) {
        error(url, Collections.EMPTY_MAP, "POST", callbackId, result);
    }

    @Override
    public void error(
            String url,
            Map<String, String> headers,
            String httpMethod,
            String callbackId,
            MilestoneReleaseResultRest result) {
        log.info("Import of milestone {} ended with error.", result.getMilestoneId());
        Callback callback = new Callback(callbackId, 500);
        send(url, headers, httpMethod, new BrewPushMilestoneResult(result, callback));
    }

    @Override
    public void failure(String url, String callbackId, MilestoneReleaseResultRest result) {
        failure(url, Collections.EMPTY_MAP, "POST", callbackId, result);
    }

    @Override
    public void failure(
            String url,
            Map<String, String> headers,
            String httpMethod,
            String callbackId,
            MilestoneReleaseResultRest result) {
        log.info("Import of milestone {} ended with failure.", result.getMilestoneId());
        Callback callback = new Callback(callbackId, 418);
        send(url, headers, httpMethod, new BrewPushMilestoneResult(result, callback));
    }

    private static MultivaluedMap<String, Object> toMultivaluedMap(Map<String, String> map) {
        MultivaluedMap<String, Object> toReturn = new MultivaluedHashMap<>();
        map.forEach(toReturn::add);
        return toReturn;
    }
}
