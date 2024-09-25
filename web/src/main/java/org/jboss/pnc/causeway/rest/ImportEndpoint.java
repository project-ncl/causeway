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

import org.jboss.pnc.api.causeway.dto.push.BuildImportRequest;
import org.jboss.pnc.causeway.ctl.ImportController;
import org.jboss.pnc.causeway.rest.spi.Import;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@RequestScoped
public class ImportEndpoint implements Import {

    @Inject
    private ImportController controller;

    @Inject
    private UserService userSerivce;

    @Override
    public Response testResponse(String var) {
        return Response.ok(var).build();
    }

    @Override
    @WithSpan
    public Response importBuild(@SpanAttribute(value = "request") BuildImportRequest request) {
        // Use the DTO userInitiator as the user who started the push. If this is absent, use the SSO user who did the
        // REST request instead. They are different since the SSO user is typically just the PNC-Orch service account
        String user = request.getUserInitiator();
        if (user == null) {
            user = userSerivce.getUsername();
        }

        controller.importBuild(request.getBuild(), request.getCallback(), user, request.isReimport());
        return Response.accepted().build();
    }

}
