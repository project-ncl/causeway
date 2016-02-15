package org.jboss.pnc.causeway.model;

public class ImportedBuild
{
    private final Long buildId;

    private final BrewBuild brewBuild;

    public ImportedBuild(Long buildId, BrewBuild brewBuild) {
        this.buildId = buildId;
        this.brewBuild = brewBuild;
    }

    public Long getBuildId() {
        return buildId;
    }

    public BrewBuild getBrewBuild() {
        return brewBuild;
    }

}
