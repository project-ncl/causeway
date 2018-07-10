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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BrewBuild extends BrewNVR {

    private final Integer id;

    @JsonCreator
    public BrewBuild(@JsonProperty("id") Integer id, @JsonProperty("name") String name, @JsonProperty("version") String version, @JsonProperty("release") String release) {
        super(name, version, release);
        this.id = id;
    }

    public BrewBuild(Integer id, BrewNVR nvr) {
        this(id, nvr.getName(), nvr.getVersion(), nvr.getRelease());
    }

    public Integer getId() {
        return id;
    }
}
