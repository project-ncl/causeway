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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(value = "maven")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = MavenBuiltArtifact.MavenBuiltArtifactBuilder.class)
public class MavenBuiltArtifact extends BuiltArtifact {

    @NonNull
    private final String groupId;
    @NonNull
    private final String artifactId;
    @NonNull
    private final String version;

    @Builder
    public MavenBuiltArtifact(String groupId, String artifactId, String version, int id, String filename, String architecture, String md5, String artifactPath, String repositoryPath, int size) {
        super(id, filename, architecture, md5, artifactPath, repositoryPath, size);
        this.groupId = Objects.requireNonNull(groupId, "GroupID must be set");
        this.artifactId = Objects.requireNonNull(artifactId, "ArtifactID must be set");
        this.version = Objects.requireNonNull(version, "Version must be set");
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class MavenBuiltArtifactBuilder {

    }
}
