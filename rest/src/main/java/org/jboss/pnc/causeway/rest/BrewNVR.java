package org.jboss.pnc.causeway.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BrewNVR {

    private final String name;
    private final String version;
    private final String release;

    public BrewNVR(String name, String version, String release) {
        this.name = name;
        this.version = version;
        this.release = release;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getRelease() {
        return release;
    }

    @JsonIgnore
    public String getNVR(){
        return name + "-" + version + "-" + release;
    }
}
