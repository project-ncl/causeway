package org.jboss.pnc.causeway.rest;

public class BuildImportResult {
    public final BrewBuild brewBuild;
    public final String error;

    public BuildImportResult(BrewBuild brewBuild, String error) {
        this.brewBuild = brewBuild;
        this.error = error;
    }
}
