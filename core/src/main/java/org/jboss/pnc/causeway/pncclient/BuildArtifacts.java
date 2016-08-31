package org.jboss.pnc.causeway.pncclient;

import java.util.ArrayList;
import java.util.List;

public final class BuildArtifacts {

    public final List<PncArtifact> buildArtifacts;
    public final List<PncArtifact> dependencies;

    public BuildArtifacts() {
        buildArtifacts = new ArrayList<>();
        dependencies = new ArrayList<>();
    }

    public static class PncArtifact {
        public final String type;
        public final String identifier;
        public final String filename;
        public final String checksum;
        public final String deployUrl;
        public final long size;

        public PncArtifact(String type, String identifier, String filename, String checksum, String deployUrl, long size) {
            this.type = type;
            this.identifier = identifier;
            this.filename = filename;
            this.checksum = checksum;
            this.deployUrl = deployUrl;
            this.size = size;
        }
    }
}
