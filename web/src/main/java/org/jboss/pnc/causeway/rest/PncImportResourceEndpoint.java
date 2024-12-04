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
package org.jboss.pnc.causeway.rest;

import org.jboss.pnc.causeway.ctl.PncImportController;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import java.util.UUID;

@RequestScoped
@Deprecated
public class PncImportResourceEndpoint implements PncImportResource {

    @Inject
    private PncImportController pncController;

    @Inject
    private UserService userSerivce;

    @Override
    @WithSpan
    public BrewPushMilestoneResponse importProductMilestone(
            @SpanAttribute(value = "request") BrewPushMilestone request) {

        if ((request.getPositiveCallback() == null || request.getNegativeCallback() == null)
                && request.getCallback() == null) {
            throw new IllegalStateException("Callbacks not specified properly");
        }

        String id = UUID.randomUUID().toString();

        // Use the DTO userInitiator as the user who started the push. If this is absent, use the SSO user who did the
        // REST request instead. They are different since the SSO user is typically just the PNC-Orch service account
        String user = request.getContent().getUserInitiator();
        if (user == null) {
            user = userSerivce.getUsername();
        }

        pncController.importMilestone(
                request.getContent().getMilestoneId(),
                request.getPositiveCallback(),
                request.getNegativeCallback(),
                request.getCallback(),
                id,
                user);

        return new BrewPushMilestoneResponse(new Callback(id));
    }

}
