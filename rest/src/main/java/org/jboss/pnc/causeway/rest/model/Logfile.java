package org.jboss.pnc.causeway.rest.model;

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
@JsonDeserialize(builder = Logfile.LogfileBuilder.class)
public class Logfile {
    
    @NonNull
    private final String filename;
    @NonNull
    private final String deployPath;
    private final int size;
    @NonNull
    private final String md5;
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class LogfileBuilder {

    }
}
