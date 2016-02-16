package org.jboss.pnc.causeway.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImportedBuild
{
    private final Long buildId;

    private final BrewBuild brewBuild;

    @JsonCreator
    public ImportedBuild(@JsonProperty("buildId")Long buildId, @JsonProperty("brewBuild")BrewBuild brewBuild) {
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
