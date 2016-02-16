package org.jboss.pnc.causeway.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BrewBuild extends BrewNVR {

    private final Long id;

    @JsonCreator
    public BrewBuild(@JsonProperty("id")Long id, @JsonProperty("name")String name, @JsonProperty("version")String version, @JsonProperty("release")String release) {
        this(id, new BrewNVR(name, version, release));
    }

    public BrewBuild(Long id, BrewNVR nvr) {
        super(nvr.getName(), nvr.getRelease(), nvr.getVersion());
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
