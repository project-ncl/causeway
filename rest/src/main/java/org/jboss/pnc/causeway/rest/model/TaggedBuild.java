package org.jboss.pnc.causeway.rest.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaggedBuild {

    @NonNull
    private final String tagPrefix;

    private final int brewBuildId;

    @JsonCreator
    public TaggedBuild(@JsonProperty("tagPrefix") String tagPrefix,
            @JsonProperty("brewBuildId") int brewBuildId) {
        this.tagPrefix = Objects.requireNonNull(tagPrefix, "Tag prefix must be specified.");
        this.brewBuildId = brewBuildId;
    }
}
