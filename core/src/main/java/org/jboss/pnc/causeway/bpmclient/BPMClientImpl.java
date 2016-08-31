/*
 * Copyright 2016 Honza Brázdil <jbrazdil@redhat.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.jboss.pnc.rest.restmodel.causeway.MilestoneReleaseResultRest;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
@ApplicationScoped
public class BPMClientImpl implements BPMClient {
    private final ResteasyClient client;

    public BPMClientImpl() {
        client = new ResteasyClientBuilder().build();
    }

    private void send(String url, BrewPushMilestoneResult result){
        ResteasyWebTarget target = client.target(url);
        target.request(MediaType.APPLICATION_JSON).post(Entity.entity(result, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public void success(String url, String callbackId, MilestoneReleaseResultRest result) {
        Callback callback = new Callback(callbackId, 200);
        send(url, new BrewPushMilestoneResult(result, callback));
    }
    
    @Override
    public void error(String url, String callbackId, MilestoneReleaseResultRest result) {
        Callback callback = new Callback(callbackId, 418);
        send(url, new BrewPushMilestoneResult(result, callback));
    }

    @Override
    public void failure(String url, String callbackId, MilestoneReleaseResultRest result) {
        Callback callback = new Callback(callbackId, 500);
        send(url, new BrewPushMilestoneResult(result, callback));
    }
}
