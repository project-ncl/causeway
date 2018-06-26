package org.jboss.pnc.causeway.rest.model;

import org.jboss.pnc.causeway.rest.CallbackTarget;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildImportRequest {

    @NonNull
    private final CallbackTarget callback;
    @NonNull
    private final Build build;

    @JsonCreator
    public BuildImportRequest(@JsonProperty("callback") CallbackTarget callback,
            @JsonProperty("build") Build build) {
        this.callback = Objects.requireNonNull(callback, "Callback must be specified.");
        this.build = Objects.requireNonNull(build, "Build information must be specified.");
    }
}
