package org.jboss.pnc.causeway.rest.model;

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
    public MavenBuiltArtifact(String groupId, String artifactId, String version, int id, String filename, String architecture, String md5, String deployPath, int size) {
        super(id, filename, architecture, md5, deployPath, size);
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class MavenBuiltArtifactBuilder {

    }
}
