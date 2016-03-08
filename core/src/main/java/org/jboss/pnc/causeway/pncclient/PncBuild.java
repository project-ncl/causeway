package org.jboss.pnc.causeway.pncclient;

import org.jboss.pnc.causeway.rest.BrewNVR;

import java.util.ArrayList;
import java.util.List;

public final class PncBuild {

    public final List<PncArtifact> buildArtifacts;
    public final List<PncArtifact> dependencies;

    public PncBuild() {
        buildArtifacts = new ArrayList<>();
        dependencies = new ArrayList<>();
    }

    public BrewNVR createNVR() {
        return null;
    }

    public static class PncArtifact {
        public final String type;
        public final String identifier;
        public final String filename;
        public final String checksum;

        public PncArtifact(String type, String identifier, String filename, String checksum) {
            this.type = type;
            this.identifier = identifier;
            this.filename = filename;
            this.checksum = checksum;
        }
    }
}
