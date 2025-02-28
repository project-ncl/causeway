/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.red.build.koji.model.json.BuildOutput;
import com.redhat.red.build.koji.model.json.BuildTool;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.NpmTypeInfoExtraInfo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Condition;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.source.RenamedSources;
import org.jboss.pnc.dto.ArtifactRef;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.ArtifactQuality;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@QuarkusTest
public class TranslatorTest {

    @Inject
    BuildTranslator bt;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String SOURCES_PATH = "sources.tar.gz";
    private static final String SOURCES = "Burn this after reading!";

    @BeforeAll
    public static void setUp() {
        mapper.registerModule(new JavaTimeModule());
    }

    private BurnAfterReadingFile mockBARF(String content) throws IOException {
        BurnAfterReadingFile file = Mockito.mock(BurnAfterReadingFile.class);
        Mockito.when(file.read()).then(invocationOnMock -> new ByteArrayInputStream(content.getBytes()));
        Mockito.when(file.getName()).thenReturn("some-file");
        Mockito.when(file.getSize()).thenReturn(content.getBytes().length);
        Mockito.when(file.getMd5()).thenReturn("a4c0d35c95a63a805915367dcfe6b751");
        return file;
    }

    @Test
    public void testReadBuildArtifacts() throws Exception {
        // given
        String groupId = "org.jboss.pnc";
        String artifactId = "parent";
        String version = "2.0.0";

        String json = readResponseBodyFromTemplate("build-dto-1.json");
        Build build = mapper.readValue(json, Build.class);

        BuildArtifacts artifacts = new BuildArtifacts();
        artifacts.getBuildArtifacts()
                .add(
                        newArtifact(
                                "2369",
                                "org.apache.geronimo.specs",
                                "geronimo-annotation_1.0_spec",
                                "1.1.1.redhat-1",
                                "pom"));
        artifacts.getBuildArtifacts()
                .add(
                        newArtifact(
                                "2370",
                                "org.apache.geronimo.specs",
                                "geronimo-annotation_1.0_spec",
                                "1.1.1.redhat-1",
                                "jar"));
        artifacts.getBuildArtifacts()
                .add(
                        newArtifact(
                                "2371",
                                "org.apache.geronimo.specs",
                                "geronimo-annotation_1.0_spec",
                                "1.1.1.redhat-1",
                                "tar.gz",
                                "project-sources"));

        artifacts.getDependencies().add(newArtifact("7", "org.apache.maven", "maven-project", "2.0.6", "pom"));
        artifacts.getDependencies().add(newArtifact("9", "org.apache.maven.shared", "maven-shared-io", "1.1", "jar"));
        artifacts.getDependencies().add(newArtifact("10", "xml-apis", "xml-apis", "1.0.b2", "jar"));

        RenamedSources sources = prepareSourcesFile(new RenamedSources.ArtifactType(groupId, artifactId, version));

        // when
        KojiImport out = bt.translate(
                new BrewNVR(groupId + ":" + artifactId, version, "1"),
                build,
                artifacts,
                sources,
                mockBARF("foo-bar-logs"),
                mockBARF("foo-bar-align-logs"),
                "joe");

        // Then

        Condition<BuildOutput> buildArtifact = new Condition<>(
                bo -> artifacts.getBuildArtifacts()
                        .stream()
                        .anyMatch(a -> a.getDeployPath().equals("/" + bo.getFilename())),
                "build artifact");
        Condition<BuildOutput> buildLog = new Condition<>(bo -> bo.getOutputType().equals("log"), "log");
        Condition<BuildOutput> mavenArtifact = new Condition<>(
                bo -> bo.getOutputType().equals("maven"),
                "maven artifact");

        assertThat(out.getBuild()).hasFieldOrPropertyWithValue("name", groupId + "-" + artifactId)
                .hasFieldOrPropertyWithValue("version", version)
                .hasFieldOrPropertyWithValue("release", "1")
                .hasFieldOrPropertyWithValue("startTime", Date.from(Instant.parse("2019-02-15T02:02:36.645Z")));
        assertThat(out.getBuild().getSource())
                .hasFieldOrPropertyWithValue("revision", "57ebfa20374d708e232fc8b45f37def055300260");
        assertThat(out.getBuild().getSource().getUrl()).contains("https://github.com/project-ncl/pnc.git");
        assertThat(out.getBuild().getExtraInfo().getMavenExtraInfo()).hasFieldOrPropertyWithValue("groupId", groupId)
                .hasFieldOrPropertyWithValue("artifactId", artifactId)
                .hasFieldOrPropertyWithValue("version", version);
        assertThat(out.getBuildRoots()).hasSize(1);
        assertThat(out.getBuildRoots().get(0).getBuildTools()).hasSize(3)
                .extracting(BuildTool::getName)
                .containsExactly("JDK", "MAVEN", "OS");
        assertThat(out.getOutputs()).hasSize(3 + 3) // 3 artifacts + build log + align log + sources
                .areExactly(3, buildArtifact)
                .areExactly(4, mavenArtifact)
                .areExactly(2, buildLog);
    }

    @Test
    public void testReadNpmBuildArtifacts() throws Exception {
        // given
        String scope = "@redhat";
        String packageName = "opossum";
        String version = "0.5.0";

        String json = readResponseBodyFromTemplate("build-dto-npm.json");
        Build build = mapper.readValue(json, Build.class);

        BuildArtifacts artifacts = new BuildArtifacts();
        artifacts.getBuildArtifacts().add(newNpmArtifact("1800", scope, packageName, version));

        artifacts.getDependencies().add(newNpmArtifact("7777", null, "once", "1.4.0"));
        artifacts.getDependencies().add(newNpmArtifact("9999", null, "inflight", "1.0.6"));
        artifacts.getDependencies().add(newNpmArtifact("10101", "@babel", "core", "7.11.0"));

        RenamedSources sources = prepareSourcesFile(
                new RenamedSources.ArtifactType(scope + "/" + packageName, version));

        // when
        KojiImport out = bt.translate(
                new BrewNVR(scope + ":" + packageName, version, "1"),
                build,
                artifacts,
                sources,
                mockBARF("foo-bar-logs"),
                mockBARF("foo-bar-align-logs"),
                "joe");

        // Then
        Condition<BuildOutput> buildArtifact = new Condition<>(
                bo -> artifacts.getBuildArtifacts()
                        .stream()
                        .anyMatch(a -> a.getDeployPath().equals("/" + bo.getFilename())),
                "build artifact");
        Condition<BuildOutput> log = new Condition<>(bo -> bo.getOutputType().equals("log"), "log");
        Condition<BuildOutput> npmArtifact = new Condition<>(bo -> bo.getOutputType().equals("npm"), "npm artifact");

        assertThat(out.getBuild()).hasFieldOrPropertyWithValue("name", scope + "-" + packageName)
                .hasFieldOrPropertyWithValue("version", version)
                .hasFieldOrPropertyWithValue("release", "1")
                .hasFieldOrPropertyWithValue("startTime", Date.from(Instant.parse("2021-07-23T15:54:18.259Z")));
        assertThat(out.getBuild().getSource())
                .hasFieldOrPropertyWithValue("revision", "647daeb088cd49354f8831d3e3dab440e039b11a");
        assertThat(out.getBuild().getSource().getUrl()).contains("http://github.com/nodeshift/opossum.git");
        assertThat(out.getBuild().getExtraInfo().getNpmExtraInfo())
                .hasFieldOrPropertyWithValue("name", scope + "-" + packageName)
                .hasFieldOrPropertyWithValue("version", version);
        assertThat(out.getBuild().getExtraInfo().getTypeInfo())
                .hasFieldOrPropertyWithValue("npmTypeInfoExtraInfo", NpmTypeInfoExtraInfo.getInstance());
        assertThat(out.getBuildRoots()).hasSize(1);
        assertThat(out.getBuildRoots().get(0).getBuildTools()).hasSize(3)
                .extracting(BuildTool::getName)
                .containsExactly("NPM", "OS", "Nodejs");
        assertThat(out.getOutputs()).hasSize(4) // 1 artifact + build log + align log + sources
                .areExactly(1, buildArtifact)
                .areExactly(2, npmArtifact)
                .areExactly(2, log);

        // System.out.println(mapper.writeValueAsString(out));
    }

    @Test
    public void testTranslateBuildWithMissingTool() throws Exception {
        // given
        String groupId = "org.jboss.pnc";
        String artifactId = "parent";
        String version = "2.0.0";

        String json = readResponseBodyFromTemplate("build-dto-missing-tool-version.json");
        Build build = mapper.readValue(json, Build.class);

        BuildArtifacts artifacts = new BuildArtifacts();
        artifacts.getBuildArtifacts()
                .add(
                        newArtifact(
                                "2369",
                                "org.apache.geronimo.specs",
                                "geronimo-annotation_1.0_spec",
                                "1.1.1.redhat-1",
                                "pom"));
        RenamedSources sources = prepareSourcesFile(new RenamedSources.ArtifactType(groupId, artifactId, version));

        assertThatThrownBy(
                () -> bt.translate(
                        new BrewNVR(groupId + ":" + artifactId, version, "1"),
                        build,
                        artifacts,
                        sources,
                        mockBARF("foo-bar-logs"),
                        mockBARF("foo-bar-align-logs"),
                        "joe"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RenamedSources prepareSourcesFile(RenamedSources.ArtifactType artifactType) throws IOException {
        Path tempFile = Files.createTempFile("burn", "me");
        Files.write(tempFile, Collections.singleton(SOURCES));
        return new RenamedSources(tempFile, SOURCES_PATH, "01234", artifactType);
    }

    private static ArtifactRef newArtifact(String id, String groupId, String artifactId, String version, String type) {
        return newArtifact(id, groupId, artifactId, version, type, null);
    }

    private static ArtifactRef newArtifact(
            String id,
            String groupId,
            String artifactId,
            String version,
            String type,
            String specifier) {
        final String filename;
        final String identifier;
        if (specifier == null) {
            filename = artifactId + "-" + version + "." + type;
            identifier = groupId + ":" + artifactId + ":" + type + ":" + version;
        } else {
            filename = artifactId + "-" + version + "-" + specifier + "." + type;
            identifier = groupId + ":" + artifactId + ":" + type + ":" + version + ":" + specifier;
        }
        final String path = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + filename;

        return ArtifactRef.refBuilder()
                .id(id)
                .identifier(identifier)
                .filename(filename)
                .md5("bedf8af1b107b36c72f52009e6fcc768")
                .deployUrl(
                        "http://example.com/api/hosted/build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/" + path)
                .deployPath("/" + path)
                .size(13245L)
                .artifactQuality(ArtifactQuality.NEW)
                .build();
    }

    private static ArtifactRef newNpmArtifact(String id, String scope, String packageName, String version) {
        final String identifier;
        if (scope == null) {
            identifier = packageName + ":" + version;
        } else {
            identifier = scope + "/" + packageName + ":" + version;
        }
        final String path = (scope == null ? "" : scope + "/") + packageName + "/-/" + packageName + "-" + version
                + ".tgz";

        return ArtifactRef.refBuilder()
                .id(id)
                .identifier(identifier)
                .filename(path)
                .md5("bedf8af1b107b36c72f52009e6fcc768")
                .deployUrl("http://example.com/build-repo/" + path)
                .deployPath("/" + path)
                .size(13245L)
                .artifactQuality(ArtifactQuality.NEW)
                .build();
    }

    private String readResponseBodyFromTemplate(String name) throws IOException {
        String folderName = getClass().getPackage().getName().replace(".", "/");
        try (InputStream inputStream = getContextClassLoader().getResourceAsStream(folderName + "/" + name)) {
            return StringUtils
                    .join(IOUtils.readLines(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8), "\n");
        }
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
