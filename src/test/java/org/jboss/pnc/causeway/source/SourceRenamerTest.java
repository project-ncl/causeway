/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.source;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.brewclient.BuildTranslatorImpl;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.enums.BuildType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.pnc.causeway.source.SourceRenamer.ARCHIVE_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class SourceRenamerTest {

    @Inject
    BuildTranslatorImpl buildTranslator;

    @Inject
    SourceRenamer renamer;

    @Test
    public void shouldRenameRootDirectory() throws CausewayException, IOException {
        String groupId = "org.foo.bar";
        String artifactId = "foo-bar-utils";
        String version = "1.0.0.Final-redhat-00001";

        InputStream sources = SourceRenamerTest.class.getResourceAsStream("foobar.tar.gz");
        RenamedSources repack = renamer.repackMaven(sources, groupId, artifactId, version);
        String newName = artifactId + "-" + version;
        assertEquals(
                "org/foo/bar/foo-bar-utils/1.0.0.Final-redhat-00001/" + newName + ARCHIVE_SUFFIX,
                repack.getName());

        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(repack.read()));

        ArchiveEntry entry = tarArchiveInputStream.getNextEntry();
        int count = 0;
        while (entry != null) {
            count++;
            assertTrue(entry.getName().startsWith(newName));
            entry = tarArchiveInputStream.getNextEntry();
        }
        assertEquals(5, count);
    }

    @Test
    public void shouldGetMavenDeployPath() {
        String groupId = "org.foo.bar";
        String artifactId = "foo-bar-utils";
        String version = "1.0.0.Final-redhat-00001";

        String path = renamer.getMavenDeployPath(groupId, artifactId, version);

        String newName = artifactId + "-" + version;
        assertEquals("/org/foo/bar/foo-bar-utils/1.0.0.Final-redhat-00001/" + newName + ARCHIVE_SUFFIX, path);
    }

    @Test
    public void shouldGetMavenDeployPath2() throws CausewayException {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("BREW_BUILD_VERSION", "1.8.2.fuse-790037-redhat-00001");
        attributes.put("BREW_BUILD_NAME", "org.arquillian.cube:arquillian-cube-parent");
        Build build = Build.builder()
                .attributes(attributes)
                .buildConfigRevision(BuildConfigurationRevision.builder().buildType(BuildType.MVN).build())
                .build();
        BuildArtifacts artifacts = new BuildArtifacts();
        String path = buildTranslator.getSourcesDeployPath(build, artifacts);
        assertEquals(
                "/org/arquillian/cube/arquillian-cube-parent/1.8.2.fuse-790037-redhat-00001/arquillian-cube-parent-1.8.2.fuse-790037-redhat-00001-project-sources.tar.gz",
                path);
    }

    @Test
    public void shouldGetNPMDeployPath() {
        String project = "foo-bar";
        String version = "1.0.0.Final-redhat-00001";

        String path = renamer.getNPMDeployPath(project, version);

        String newName = project + "-" + version;
        assertEquals("/foo-bar/-/" + newName + ARCHIVE_SUFFIX, path);
    }
}
