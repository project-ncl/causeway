/**
 * Copyright (C) 2015 Red Hat, Inc. (jbrazdil@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.brewclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.red.build.koji.model.json.BuildOutput;
import com.redhat.red.build.koji.model.json.BuildTool;
import com.redhat.red.build.koji.model.json.KojiImport;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Condition;
import org.jboss.pnc.api.causeway.dto.push.Build;
import org.jboss.pnc.api.causeway.dto.push.BuildRoot;
import org.jboss.pnc.api.causeway.dto.push.BuiltArtifact;
import org.jboss.pnc.api.causeway.dto.push.MavenBuild;
import org.jboss.pnc.api.causeway.dto.push.MavenBuiltArtifact;
import org.jboss.pnc.api.causeway.dto.push.NpmBuild;
import org.jboss.pnc.api.causeway.dto.push.NpmBuiltArtifact;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator.Artifact;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.source.RenamedSources;
import org.jboss.pnc.causeway.source.SourceRenamer;
import org.jboss.pnc.enums.ArtifactQuality;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class TranslatorTest {
    private static final CausewayConfig config = new CausewayConfig();
    private static final SourceRenamer renamer = new SourceRenamer();
    private static final BuildTranslator bt = new BuildTranslatorImpl(config, renamer);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String SOURCES_PATH = "sources.tar.gz";
    private static final String SOURCES = "Burn this after reading!";

    @BeforeClass
    public static void setUp() {
        config.setPnclBuildsURL("http://example.com/build-records/");
        config.setArtifactStorage("http://example.com/storage/");
        mapper.registerSubtypes(MavenBuild.class, MavenBuiltArtifact.class);
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void testReadBuildArtifacts() throws Exception {
        // given
        String groupId = "org.jboss.pnc";
        String artifactId = "parent";
        String version = "2.0.0";

        String json = readResponseBodyFromTemplate("build-dto-1.json");
        org.jboss.pnc.dto.Build build = mapper.readValue(json, org.jboss.pnc.dto.Build.class);

        BuildArtifacts artifacts = new BuildArtifacts();
        artifacts.buildArtifacts.add(
                newArtifact(
                        "2369",
                        "org.apache.geronimo.specs",
                        "geronimo-annotation_1.0_spec",
                        "1.1.1.redhat-1",
                        "pom"));
        artifacts.buildArtifacts.add(
                newArtifact(
                        "2370",
                        "org.apache.geronimo.specs",
                        "geronimo-annotation_1.0_spec",
                        "1.1.1.redhat-1",
                        "jar"));
        artifacts.buildArtifacts.add(
                newArtifact(
                        "2371",
                        "org.apache.geronimo.specs",
                        "geronimo-annotation_1.0_spec",
                        "1.1.1.redhat-1",
                        "tar.gz",
                        "project-sources"));

        artifacts.dependencies.add(newArtifact("7", "org.apache.maven", "maven-project", "2.0.6", "pom"));
        artifacts.dependencies.add(newArtifact("9", "org.apache.maven.shared", "maven-shared-io", "1.1", "jar"));
        artifacts.dependencies.add(newArtifact("10", "xml-apis", "xml-apis", "1.0.b2", "jar"));

        RenamedSources sources = prepareSourcesFile();

        // when
        KojiImport out = bt.translate(
                new BrewNVR(groupId + ":" + artifactId, version, "1"),
                build,
                artifacts,
                sources,
                "foo-bar-logs",
                "joe");

        // Then
        Condition<BuildOutput> buildArtifact = new Condition<>(
                bo -> artifacts.buildArtifacts.stream().anyMatch(a -> a.deployPath.equals(bo.getFilename())),
                "build artifact");
        Condition<BuildOutput> buildLog = new Condition<>(bo -> bo.getOutputType().equals("log"), "build log");
        Condition<BuildOutput> sourceFile = new Condition<>(
                bo -> bo.getOutputType().equals("remote-source-file"),
                "source file");

        assertThat(out.getBuild()).hasFieldOrPropertyWithValue("name", groupId + "-" + artifactId)
                .hasFieldOrPropertyWithValue("version", version)
                .hasFieldOrPropertyWithValue("release", "1")
                .hasFieldOrPropertyWithValue("startTime", Date.from(Instant.parse("2019-02-15T02:02:36.645Z")));
        assertThat(out.getBuild().getSource())
                .hasFieldOrPropertyWithValue("revision", "57ebfa20374d708e232fc8b45f37def055300260");
        assertThat(out.getBuild().getSource().getUrl()).contains("http://github.com/project-ncl/pnc.git");
        assertThat(out.getBuild().getExtraInfo().getMavenExtraInfo()).hasFieldOrPropertyWithValue("groupId", groupId)
                .hasFieldOrPropertyWithValue("artifactId", artifactId)
                .hasFieldOrPropertyWithValue("version", version);
        assertThat(out.getBuildRoots()).hasSize(1);
        assertThat(out.getBuildRoots().get(0).getBuildTools()).hasSize(3)
                .extracting(BuildTool::getName)
                .containsExactly("JDK", "MAVEN", "OS");
        assertThat(out.getOutputs()).hasSize(3 + 2) // 3 artifacts + build log + sources
                .areExactly(3, buildArtifact)
                .areExactly(2, anyOf(buildLog, sourceFile));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTranslateBuildWithMissingTool() throws Exception {
        // given
        String groupId = "org.jboss.pnc";
        String artifactId = "parent";
        String version = "2.0.0";

        String json = readResponseBodyFromTemplate("build-dto-missing-tool-version.json");
        org.jboss.pnc.dto.Build build = mapper.readValue(json, org.jboss.pnc.dto.Build.class);

        BuildArtifacts artifacts = new BuildArtifacts();
        artifacts.buildArtifacts.add(
                newArtifact(
                        "2369",
                        "org.apache.geronimo.specs",
                        "geronimo-annotation_1.0_spec",
                        "1.1.1.redhat-1",
                        "pom"));
        RenamedSources sources = prepareSourcesFile();

        // throw exception when
        KojiImport out = bt.translate(
                new BrewNVR(groupId + ":" + artifactId, version, "1"),
                build,
                artifacts,
                sources,
                "foo-bar-logs",
                "joe");
    }

    @Test
    public void testReadBuild() throws Exception {
        // given
        String groupId = "org.apache.geronimo.specs";
        String artifactId = "geronimo-annotation_1.0_spec";
        String version = "1.1.1";
        String json = readResponseBodyFromTemplate("build.json");

        Build build = mapper.readValue(json, Build.class);
        RenamedSources sources = prepareSourcesFile();

        // when
        KojiImport out = bt.translate(new BrewNVR(groupId + ":" + artifactId, version, "1"), build, sources, "joe");

        // Then
        Condition<BuildOutput> buildArtifact = new Condition<>(
                bo -> bo.getOutputType().equals("maven"),
                "maven artifact");
        Condition<BuildOutput> logFile = new Condition<>(bo -> bo.getOutputType().equals("log"), "log file");
        Condition<BuildOutput> sourceFile = new Condition<>(
                bo -> bo.getOutputType().equals("remote-source-file"),
                "source file");

        assertThat(out.getBuild()).hasFieldOrPropertyWithValue("name", groupId + "-" + artifactId)
                .hasFieldOrPropertyWithValue("version", version)
                .hasFieldOrPropertyWithValue("release", "1")
                .hasFieldOrPropertyWithValue("startTime", new Date(1470309691844l));
        assertThat(out.getBuild().getSource())
                .hasFieldOrPropertyWithValue("revision", "repour-57ebfa20374d708e232fc8b45f37def055300260");
        assertThat(out.getBuild().getSource().getUrl())
                .contains("http://internal.maven.repo.com/productization-test3/geronimo-specs");
        assertThat(out.getBuild().getExtraInfo().getMavenExtraInfo()).hasFieldOrPropertyWithValue("groupId", groupId)
                .hasFieldOrPropertyWithValue("artifactId", artifactId)
                .hasFieldOrPropertyWithValue("version", version);
        assertThat(out.getBuildRoots()).hasSize(1);
        assertThat(out.getBuildRoots().get(0).getBuildTools()).hasSize(3)
                .extracting(BuildTool::getName)
                .containsExactly("JDK", "MAVEN", "OS");
        assertThat(out.getOutputs()).hasSize(3 + 2 + 1) // 3 artifacts + 2 logs + sources
                .areExactly(3, buildArtifact)
                .areExactly(2, logFile)
                .areExactly(1, sourceFile);

        mapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        String jsonOut = mapper.writeValueAsString(out);
        System.out.println("RESULTA:\n" + jsonOut);
    }

    @Test
    public void testNpmImportPathsComposition() throws CausewayException {
        Set<BuiltArtifact> artifacts = new HashSet<>();
        artifacts.add(
                NpmBuiltArtifact.builder()
                        .repositoryPath("/api/content/npm/hosted/pnc-builds/")
                        .artifactPath("/thepackage/-/thepackage-1.0.0.tgz")
                        .architecture("")
                        .filename("thepackage-1.0.0.tgz")
                        .md5("")
                        .name("thepackage_1.0.0")
                        .version("1.0.0")
                        .build());
        BuildRoot buildRoot = BuildRoot.builder()
                .container("")
                .containerArchitecture("")
                .host("")
                .hostArchitecture("")
                .tools(Collections.emptyMap())
                .build();
        Build build = NpmBuild.builder()
                .buildName("")
                .builtArtifacts(artifacts)
                .externalBuildSystem("")
                .externalBuildURL("")
                .startTime(new Date())
                .endTime(new Date())
                .scmURL("")
                .scmRevision("")
                .buildRoot(buildRoot)
                .logs(Collections.emptySet())
                .sourcesURL("")
                .dependencies(Collections.emptySet())
                .tagPrefix("")
                .name("")
                .build();
        ImportFileGenerator importFiles = bt.getImportFiles(build, null);

        Set<Artifact> importArtifacts = importFiles.artifacts;
        assertEquals(1, importArtifacts.size());
        assertEquals(
                "http://example.com/storage/api/content/npm/hosted/pnc-builds/thepackage/-/thepackage-1.0.0.tgz",
                importArtifacts.iterator().next().getUrl().toString());
    }

    private RenamedSources prepareSourcesFile() throws IOException {
        Path tempFile = Files.createTempFile("burn", "me");
        Files.write(tempFile, Collections.singleton(SOURCES));
        return new RenamedSources(tempFile, SOURCES_PATH, "01234");
    }

    private static org.jboss.pnc.causeway.pncclient.BuildArtifacts.PncArtifact newArtifact(
            String id,
            String groupId,
            String artifactId,
            String version,
            String type) {
        return newArtifact(id, groupId, artifactId, version, type, null);
    }

    private static BuildArtifacts.PncArtifact newArtifact(
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
        return new BuildArtifacts.PncArtifact(
                id,
                identifier,
                filename,
                "bedf8af1b107b36c72f52009e6fcc768",
                "http://ulozto.cz/api/hosted/build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/" + path,
                13245,
                ArtifactQuality.NEW);
    }

    private String readResponseBodyFromTemplate(String name) throws IOException {
        String folderName = getClass().getPackage().getName().replace(".", "/");
        try (InputStream inputStream = getContextClassLoader().getResourceAsStream(folderName + "/" + name)) {
            return StringUtils.join(IOUtils.readLines(inputStream, Charset.forName("utf-8")), "\n");
        }
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
