package org.jboss.pnc.causeway.pncclient;

import org.jboss.pnc.causeway.rest.BrewNVR;

import java.util.ArrayList;
import java.util.List;

public final class PncBuild {

    private final int id;
    public final List<PncArtifact> buildArtifacts;
    public final List<PncArtifact> dependencies;

    public PncBuild(int id) {
        this.id = id;
        buildArtifacts = new ArrayList<>();
        dependencies = new ArrayList<>();
    }

    public BrewNVR createNVR() {
        return null;
    }

    public int getId() {
        return id;
    }

    public static class PncArtifact {
        public final String type;
        public final String identifier;
        public final String filename;
        public final String checksum;
        public final String deployUrl;

        public PncArtifact(String type, String identifier, String filename, String checksum, String deployUrl) {
            this.type = type;
            this.identifier = identifier;
            this.filename = filename;
            this.checksum = checksum;
            this.deployUrl = deployUrl;
        }
    }
}
