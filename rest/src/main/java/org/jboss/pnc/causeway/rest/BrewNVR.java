package org.jboss.pnc.causeway.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class BrewNVR {

    private final String name;
    private final String version;
    private final String release;

    public BrewNVR(String name, String version, String release) {
        this.name = name;
        this.version = version.replace( '-', '_' );
        this.release = release.replace( '-', '_' );
    }

    public String getName() {
        return name;
    }

    public String getKojiName() {
        return name.replaceAll(":", "-");
    }

    public String getVersion() {
        return version;
    }

    public String getRelease() {
        return release;
    }

    @JsonIgnore
    public String getNVR(){
        return getKojiName() + "-" + version + "-" + release;
    }
}
