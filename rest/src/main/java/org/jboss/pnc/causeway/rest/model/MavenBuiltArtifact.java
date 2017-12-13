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
 * @author Honza Brázdil <janinko.g@gmail.com>
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
