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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BrewNVR {

    private final String name;
    private final String version;
    private final String release;

    public BrewNVR(String name, String version, String release) {
        this.name = name;
        this.version = version;
        this.release = release;
    }

    public String getName() {
        return name;
    }

    public String getKojiName() {
        return name.replaceAll(":", "-");
    }

    public String getVersion() {
        return version;
    }

    public String getRelease() {
        return release;
    }

    @JsonIgnore
    public String getNVR(){
        return getKojiName() + "-" + version + "-" + release;
    }
}
