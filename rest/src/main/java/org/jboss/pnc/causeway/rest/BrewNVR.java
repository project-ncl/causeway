package org.jboss.pnc.causeway.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.builder.EqualsBuilder;

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

    @Override
    // Added the equals nethod so that this object can be correctly used in Mockito's eq() expression
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BrewNVR that = (BrewNVR) o;

        return new EqualsBuilder()
                .append(name, that.name)
                .append(version, that.version)
                .append(release, that.release)
                .isEquals();
    }
}
