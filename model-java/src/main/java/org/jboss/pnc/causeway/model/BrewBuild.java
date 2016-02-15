package org.jboss.pnc.causeway.model;

public class BrewBuild extends BrewNVR {

    private final Long id;

    public BrewBuild(Long id, BrewNVR nvr) {
        super(nvr.getName(), nvr.getRelease(), nvr.getVersion());
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
