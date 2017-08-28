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
package org.jboss.pnc.causeway.pncclient;

import java.util.ArrayList;
import java.util.List;

public final class BuildArtifacts {

    public final List<PncArtifact> buildArtifacts;
    public final List<PncArtifact> dependencies;

    public BuildArtifacts() {
        buildArtifacts = new ArrayList<>();
        dependencies = new ArrayList<>();
    }

    public static class PncArtifact {
        public final int id;
        public final String type;
        public final String identifier;
        public final String deployPath;
        public final String checksum;
        public final String deployUrl;
        public final long size;

        public PncArtifact(int id, String type, String identifier, String deployPath, String checksum, String deployUrl, long size) {
            this.id = id;
            this.type = type;
            this.identifier = identifier;
            this.deployPath = deployPath;
            this.checksum = checksum;
            this.deployUrl = deployUrl;
            this.size = size;
        }
    }
}
