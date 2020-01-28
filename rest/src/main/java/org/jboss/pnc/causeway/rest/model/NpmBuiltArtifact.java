package org.jboss.pnc.causeway.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Objects;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(value = "npm")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = NpmBuiltArtifact.NpmBuiltArtifactBuilder.class)
public class NpmBuiltArtifact extends BuiltArtifact {

    @NonNull
    private final String name;
    @NonNull
    private final String version;

    @Builder
    public NpmBuiltArtifact(String name, String version, int id, String filename, String architecture, String md5,
            String artifactPath, String repositoryPath, int size) {
        super(id, filename, architecture, md5, artifactPath, repositoryPath, size);
        this.name = Objects.requireNonNull(name, "Artifact name must be set");
        this.version = Objects.requireNonNull(version, "Artifact version must be set");
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class NpmBuiltArtifactBuilder {

    }
}
