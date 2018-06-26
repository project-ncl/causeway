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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
        property = "@artifactType")
@JsonSubTypes(
        @JsonSubTypes.Type(MavenBuiltArtifact.class))
public class BuiltArtifact {
    private final int id;
    @NonNull
    private final String filename;
    @NonNull
    private final String architecture;
    @NonNull
    private final String md5;
    @NonNull
    private final String artifactPath;
    @NonNull
    private final String repositoryPath;
    private final int size;

    public BuiltArtifact(int id, String filename, String architecture, String md5, String artifactPath, String repositoryPath, int size) {
        this.id = id;
        this.filename = Objects.requireNonNull(filename, "Filename must be set");
        this.architecture = Objects.requireNonNull(architecture, "Architecture must be set");
        this.md5 = Objects.requireNonNull(md5, "MD5 checksum must be set");
        this.artifactPath = Objects.requireNonNull(artifactPath, "Artifact path must be set");
        this.repositoryPath = Objects.requireNonNull(repositoryPath, "Repository path must be set");
        this.size = size;
    }
}
