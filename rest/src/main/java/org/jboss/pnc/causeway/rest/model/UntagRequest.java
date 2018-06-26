package org.jboss.pnc.causeway.rest.model;

import org.jboss.pnc.causeway.rest.model.*;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

import org.jboss.pnc.causeway.rest.CallbackTarget;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UntagRequest {

    private final CallbackTarget callback;
    @NonNull
    private final TaggedBuild build;

    @JsonCreator
    public UntagRequest(@JsonProperty("callback") CallbackTarget callback,
            @JsonProperty("build") TaggedBuild build) {
        this.callback = callback;
        this.build = Objects.requireNonNull(build, "Build information must be specified.");
    }
}
