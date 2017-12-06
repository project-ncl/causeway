package org.jboss.pnc.causeway.rest.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil <janinko.g@gmail.com>
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
    private final String deployPath;
    private final int size;

    public BuiltArtifact(int id, String filename, String architecture, String md5, String deployPath, int size) {
        this.id = id;
        this.filename = filename;
        this.architecture = architecture;
        this.md5 = md5;
        this.deployPath = deployPath;
        this.size = size;
    }
}
