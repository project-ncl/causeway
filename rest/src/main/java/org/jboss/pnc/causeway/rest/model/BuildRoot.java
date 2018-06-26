package org.jboss.pnc.causeway.rest.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = BuildRoot.BuildRootBuilder.class)
public class BuildRoot {

    @NonNull
    private final String container;
    @NonNull
    private final String containerArchitecture;
    @NonNull
    private final String host;
    @NonNull
    private final String hostArchitecture;
    @NonNull
    private final Map<String, String> tools;

    @JsonPOJOBuilder(withPrefix = "")
    public static class BuildRootBuilder {

    }
}
