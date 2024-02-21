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

import org.jboss.pnc.api.dto.ComponentVersion;
import org.jboss.pnc.causeway.config.constants.BuildInformationConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.ZonedDateTime;

/**
 *
 * @author Jozef Mrazek &lt;jmrazek@redhat.com&gt;
 *
 */
@Path("/")
public class Root {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getDescription() {
        return "<h1>Causeway REST API</h1>" + "\n";
    }

    @GET
    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentVersion getVersion() {
        return ComponentVersion.builder()
                .name("Causeway")
                .version(BuildInformationConstants.VERSION)
                .commit(BuildInformationConstants.COMMIT_HASH)
                .builtOn(ZonedDateTime.parse(BuildInformationConstants.BUILD_TIME))
                .build();
    }

}
