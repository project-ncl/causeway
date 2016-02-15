package org.jboss.pnc.causeway.model;

public class BuildImportResult {
    public final BrewBuild brewBuild;
    public final String error;

    public BuildImportResult(BrewBuild brewBuild, String error) {
        this.brewBuild = brewBuild;
        this.error = error;
    }
}
