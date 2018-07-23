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
package org.jboss.pnc.causeway.rest.model;

import org.jboss.pnc.causeway.rest.model.*;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

import org.jboss.pnc.causeway.rest.CallbackTarget;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UntagRequest {

    private final CallbackTarget callback;
    @NonNull
    private final TaggedBuild build;

    @JsonCreator
    public UntagRequest(@JsonProperty("callback") CallbackTarget callback,
            @JsonProperty("build") TaggedBuild build) {
        this.callback = callback;
        this.build = Objects.requireNonNull(build, "Build information must be specified.");
    }
}