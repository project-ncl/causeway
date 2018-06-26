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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = BuildRoot.BuildRootBuilder.class)
public class BuildRoot {

    @NonNull
    private final String container;
    @NonNull
    private final String containerArchitecture;
    @NonNull
    private final String host;
    @NonNull
    private final String hostArchitecture;
    @NonNull
    private final Map<String, String> tools;

    @JsonPOJOBuilder(withPrefix = "")
    public static class BuildRootBuilder {

    }
}
