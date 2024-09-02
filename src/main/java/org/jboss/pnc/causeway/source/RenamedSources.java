/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.source;

import com.github.zafarkhaja.semver.Version;
import lombok.Getter;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;
import org.jboss.pnc.causeway.ErrorMessages;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.jboss.pnc.causeway.source.SourceRenamer.ARTIFACT_CLASSIFIER;
import static org.jboss.pnc.causeway.source.SourceRenamer.ARTIFACT_TYPE;

public class RenamedSources extends BurnAfterReadingFile {
    @Getter
    private final ArtifactType artifactType;

    public RenamedSources(Path file, String name, String md5, ArtifactType artifactType) throws IOException {
        super(file, name, md5);
        this.artifactType = artifactType;
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
            this.npmInfoAndType = new NpmPackageRef(name + "-" + ARTIFACT_CLASSIFIER, Version.valueOf(version));
        }

        public boolean isMavenType() {
            return mavenInfoAndType != null;
        }

        public boolean isNPMType() {
            return npmInfoAndType != null;
        }

        public SimpleArtifactRef getMavenInfoAndType() {
            if (!isMavenType()) {
                throw new IllegalStateException(ErrorMessages.notMavenType());
            }
            return mavenInfoAndType;
        }

        public NpmPackageRef getNpmInfoAndType() {
            if (!isNPMType()) {
                throw new IllegalStateException(ErrorMessages.notNPMType());
            }
            return npmInfoAndType;
        }
    }
}
