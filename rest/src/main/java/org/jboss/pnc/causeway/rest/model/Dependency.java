package org.jboss.pnc.causeway.rest.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil <janinko.g@gmail.com>
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = Dependency.DependencyBuilder.class)
public class Dependency {
    @NonNull
    private final String filename;
    @NonNull
    private final String md5;
    private final long size;

    @JsonPOJOBuilder(withPrefix = "")
    public static class DependencyBuilder {

    }
}
