package org.jboss.pnc.causeway.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BrewBuild extends BrewNVR {

    private final Integer id;

    @JsonCreator
    public BrewBuild(@JsonProperty("id")Integer id, @JsonProperty("name")String name, @JsonProperty("version")String version, @JsonProperty("release")String release) {
        this(id, new BrewNVR(name, version, release));
    }

    public BrewBuild(Integer id, BrewNVR nvr) {
        super(nvr.getName(), nvr.getRelease(), nvr.getVersion());
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
