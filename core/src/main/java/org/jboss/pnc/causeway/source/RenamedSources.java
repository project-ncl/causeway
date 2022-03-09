package org.jboss.pnc.causeway.source;

import lombok.Getter;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.version.VersionSpec;
import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.jboss.pnc.causeway.source.SourceRenamer.ARTIFACT_CLASSIFIER;
import static org.jboss.pnc.causeway.source.SourceRenamer.ARTIFACT_TYPE;

public class RenamedSources {
    private final Path file;
    @Getter
    private final int size;
    @Getter
    private final String name;
    @Getter
    private final String md5;
    private boolean read = false;
    @Getter
    private final ArtifactType artifactType;

    public RenamedSources(Path file, String name, String md5, ArtifactType artifactType) throws IOException {
        this.file = file;
        this.name = name;
        this.md5 = md5;
        this.size = (int) Files.size(file);
        this.artifactType = artifactType;
    }

    public InputStream read() throws IOException {
        if (read) {
            throw new IllegalStateException("File already read.");
        }
        read = true;
        return Files.newInputStream(file, StandardOpenOption.DELETE_ON_CLOSE);
    }

    public static class ArtifactType {

        private final SimpleArtifactRef mavenInfoAndType;
        private final NpmPackageRef npmInfoAndType;

        public ArtifactType(String groupId, String artifactId, String version) {
            this.mavenInfoAndType = new SimpleArtifactRef(
                    groupId,
                    artifactId,
                    version,
                    ARTIFACT_TYPE,
                    ARTIFACT_CLASSIFIER);
            this.npmInfoAndType = null;
        }

        public ArtifactType(String name, String version) {
            this.mavenInfoAndType = null;
            this.npmInfoAndType = new NpmPackageRef(name + "-" + ARTIFACT_CLASSIFIER, version);
        }

        public boolean isMavenType() {
            return mavenInfoAndType != null;
        }

        public boolean isNPMType() {
            return npmInfoAndType != null;
        }

        public SimpleArtifactRef getMavenInfoAndType() {
            if (!isMavenType()) {
                throw new IllegalStateException("This is not an NPM type.");
            }
            return mavenInfoAndType;
        }

        public NpmPackageRef getNpmInfoAndType() {
            if (!isNPMType()) {
                throw new IllegalStateException("This is not an NPM type.");
            }
            return npmInfoAndType;
        }
    }
}
