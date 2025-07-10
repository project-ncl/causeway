/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.ctl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.pnc.causeway.ErrorMessages.tagsAreMissingInKoji;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.causeway.dto.push.PushResult;
import org.jboss.pnc.api.constants.Attributes;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.CausewayFailure;
import org.jboss.pnc.causeway.brewclient.BrewBuild;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BrewNVR;
import org.jboss.pnc.causeway.brewclient.BuildTranslatorImpl;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.dto.ArtifactRef;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.User;
import org.jboss.pnc.enums.ArtifactQuality;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.enums.SystemImageType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.red.build.koji.model.json.KojiImport;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ImportControllerTest {

    @ConfigProperty(name = "causeway.koji.url")
    String kojiUrl;
    @ConfigProperty(name = "causeway.koji.web-url")
    String kojiBuildUrl;

    @InjectMock
    PncClient pncClient;
    @InjectMock
    BrewClient brewClient;

    @InjectMock
    BuildTranslatorImpl translator;

    @Inject
    ImportControllerImpl importController;

    private static final ImportFileGenerator IMPORT_FILE_GENERATOR = mock(ImportFileGenerator.class);
    private static final KojiImport KOJI_IMPORT = mock(KojiImport.class);

    private static final Random generator = new Random();
    private static final String MVN_GROUP_ID = "my.test-artifact";
    private static final String MVN_ARTIFACT_ID = "artifact-test";
    private static final String MVN_BUILD_NAME = MVN_GROUP_ID + ":" + MVN_ARTIFACT_ID;
    private static final String MVN_VERSION = "1.1.1.redhat-00001";
    private static final String MVN_VERSION_KOJI_STYLE = "1.1.1.redhat_00001";
    private static final String NPM_ARTIFACT_SCOPE = "foo-bar";
    private static final String NPM_ARTIFACT_NAME = "baz";
    private static final String NPM_VERSION = "1.1.1-redhat-00001";
    private static final String NPM_VERSION_KOJI_STYLE = "1.1.1_redhat_00001";
    private static final String NPM_BUILD_NAME = NPM_ARTIFACT_SCOPE + "-" + NPM_ARTIFACT_NAME + "-npm";
    private static final BrewNVR MVN_NVR = new BrewNVR(MVN_BUILD_NAME, MVN_VERSION_KOJI_STYLE, "1");
    private static final BrewNVR MVN_NVR2 = new BrewNVR(MVN_BUILD_NAME, MVN_VERSION_KOJI_STYLE, "2");
    private static final String USERNAME = "joe";
    private static final String TAG_PREFIX = "pnc-foo-0.1";

    @Test
    public void testReImportBuildWhenPreviousUntaggedImportExists() throws Exception {
        // Test setup
        mockBrew();
        mockTranslator();

        // Mock existing Brew build
        int existingKojiBuildID = 11;
        BrewBuild brewBuild = mockExistingBuild(existingKojiBuildID, MVN_NVR, false);
        String buildId = mockMVNBuild().getId();

        // Run import
        PushResult pushResult = importController.importBuild(buildId, TAG_PREFIX, true, USERNAME);

        // Verify
        verify(brewClient).tagBuild(eq(TAG_PREFIX), same(brewBuild));
        assertThat(pushResult).extracting(PushResult::getResult).isEqualTo(ResultStatus.SUCCESS);
        assertThat(pushResult).extracting(PushResult::getBrewBuildId).isEqualTo(existingKojiBuildID);
    }

    @Test
    public void testReImportBuildWhenPreviousTaggedImportExists() throws Exception {
        // Test setup
        mockBrew();
        mockTranslator();

        // Mock existing Brew build
        String buildId = mockMVNBuild().getId();
        mockExistingBuild(11, MVN_NVR, true);

        // Mock Brew import
        KojiImport kojiImport = mock(KojiImport.class);
        doReturn(kojiImport).when(translator).translate(eq(MVN_NVR2), any(), any(), any(), any(), any(), any());
        int newKojiBuildID = 12;
        BrewBuild brewBuild = new BrewBuild(newKojiBuildID, MVN_NVR2);
        doReturn(brewBuild).when(brewClient).importBuild(eq(MVN_NVR2), same(kojiImport), same(IMPORT_FILE_GENERATOR));

        // Run import
        PushResult pushResult = importController.importBuild(buildId, TAG_PREFIX, true, USERNAME);

        // Verify
        verify(brewClient).tagBuild(eq(TAG_PREFIX), same(brewBuild));
        assertThat(pushResult).extracting(PushResult::getResult).isEqualTo(ResultStatus.SUCCESS);
        assertThat(pushResult).extracting(PushResult::getBrewBuildId).isEqualTo(newKojiBuildID);
        assertThat(pushResult).extracting(PushResult::getBrewBuildUrl).isEqualTo(kojiBuildUrl + newKojiBuildID);
        assertThat(pushResult).extracting(PushResult::getBuildId).isEqualTo(buildId);
    }

    @Test
    public void testImportBuildWhenPreviousTaggedImportExists() throws Exception {
        // Test setup
        mockBrew();
        mockTranslator();

        // Mock existing Brew build
        int existingKojiBuildID = 11;
        mockExistingBuild(existingKojiBuildID, MVN_NVR, true);
        String buildId = mockMVNBuild().getId();

        // Run import
        PushResult pushResult = importController.importBuild(buildId, TAG_PREFIX, false, USERNAME);

        // Verify
        assertThat(pushResult).extracting(PushResult::getResult).isEqualTo(ResultStatus.SUCCESS);
        assertThat(pushResult).extracting(PushResult::getBrewBuildId).isEqualTo(existingKojiBuildID);
    }

    @Test
    public void testImportBuildWhenConflictingBrewBuildExists() throws Exception {
        // Test setup
        mockBrew();
        mockTranslator();

        // Mock existing Brew build
        String buildId = mockMVNBuild().getId();
        doThrow(new CausewayException("Conflicting brew build exists.")).when(brewClient)
                .findBrewBuildOfNVR(eq(MVN_NVR));

        // Run import
        assertThatThrownBy(() -> importController.importBuild(buildId, TAG_PREFIX, true, USERNAME))
                .isInstanceOf(CausewayException.class)
                .hasMessageContaining("Conflicting brew build exists.");
    }

    @Test
    public void testImportBuild() throws Exception {
        // Test setup
        mockBrew();
        mockTranslator();

        // Mock Brew import
        int kojiBuildID = 11;
        BrewBuild brewBuild = new BrewBuild(kojiBuildID, MVN_NVR);
        String buildId = mockMVNBuild().getId();
        doReturn(brewBuild).when(brewClient).importBuild(eq(MVN_NVR), same(KOJI_IMPORT), same(IMPORT_FILE_GENERATOR));

        // Run import
        PushResult pushResult = importController.importBuild(buildId, TAG_PREFIX, true, USERNAME);

        // Verify
        verify(brewClient).tagBuild(eq(TAG_PREFIX), same(brewBuild));
        assertThat(pushResult).extracting(PushResult::getResult).isEqualTo(ResultStatus.SUCCESS);
        assertThat(pushResult).extracting(PushResult::getBrewBuildId).isEqualTo(kojiBuildID);
        assertThat(pushResult).extracting(PushResult::getBrewBuildUrl).isEqualTo(kojiBuildUrl + kojiBuildID);
        assertThat(pushResult).extracting(PushResult::getBuildId).isEqualTo(buildId);
    }

    @Test
    public void testImportBuildWithEmptyArtifacts() throws Exception {
        // Test setup
        mockBrew();

        // Mock no builds in milestone
        String buildId = mockMVNBuild().getId();
        doReturn(new BuildArtifacts()).when(pncClient).findBuildArtifacts(eq(buildId));

        // Run import
        PushResult pushResult = importController.importBuild(buildId, TAG_PREFIX, true, USERNAME);

        // verify
        assertThat(pushResult).extracting(PushResult::getResult).isEqualTo(ResultStatus.SUCCESS);
        assertThat(pushResult).extracting(PushResult::getBrewBuildId).isNull();
        assertThat(pushResult).extracting(PushResult::getBrewBuildUrl).isNull();
        assertThat(pushResult).extracting(PushResult::getBuildId).isEqualTo(buildId);
    }

    @Test
    public void testImportBuildWhereImportBuildThrowsException() throws Exception {
        String exceptionMessage = "Import build failed";

        // Test setup
        mockBrew();
        mockTranslator();

        // Mock exception from Brew Client
        String buildId = mockMVNBuild().getId();
        doThrow(new CausewayException(exceptionMessage)).when(brewClient).findBrewBuildOfNVR(eq(MVN_NVR));

        // Run import
        assertThatThrownBy(() -> importController.importBuild(buildId, TAG_PREFIX, true, USERNAME))
                .isInstanceOf(CausewayException.class)
                .hasMessageContaining(exceptionMessage);
    }

    @Test
    public void testImportBuildWithArtifactImportErrors() throws Exception {
        final String exceptionMessage = "Failure while importing artifacts";

        // Test setup
        mockBrew();
        mockTranslator();

        String buildId = mockMVNBuild().getId();
        doThrow(new CausewayFailure(exceptionMessage)).when(brewClient)
                .importBuild(eq(MVN_NVR), same(KOJI_IMPORT), same(IMPORT_FILE_GENERATOR));

        // Run import
        assertThatThrownBy(() -> importController.importBuild(buildId, TAG_PREFIX, true, USERNAME))
                .isInstanceOf(CausewayFailure.class)
                .hasMessageContaining(exceptionMessage);
    }

    @Test
    public void testImportBuildWhenTagDoesNotExistInBrew() throws Exception {
        // Test setup
        mockBrew();
        doReturn(false).when(brewClient).tagsExists(TAG_PREFIX);
        String buildId = mockMVNBuild().getId();

        // Run import
        assertThatThrownBy(() -> importController.importBuild(buildId, TAG_PREFIX, true, USERNAME))
                .isInstanceOf(CausewayFailure.class)
                .hasMessageContaining(tagsAreMissingInKoji(TAG_PREFIX, kojiUrl));
    }

    @Test
    public void testGetNVR() throws CausewayException {
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        ArtifactRef artifact = createArtifact("32");
        buildArtifacts.getBuildArtifacts().add(artifact);
        Build build = mockMVNBuild();
        Build build2 = build.toBuilder().attributes(Map.of(Attributes.BUILD_BREW_NAME, MVN_BUILD_NAME)).build();

        BrewNVR nvr = importController.getNVR(build, new BuildArtifacts());
        assertEquals(MVN_BUILD_NAME, nvr.getName());
        assertEquals(MVN_VERSION_KOJI_STYLE, nvr.getVersion());

        BrewNVR nvr2 = importController.getNVR(build2, buildArtifacts);
        assertEquals(MVN_BUILD_NAME, nvr2.getName());
        assertEquals(MVN_VERSION_KOJI_STYLE, nvr2.getVersion());
    }

    @Test
    public void testAutomaticVersionNpm() throws CausewayException {
        Build build = mockNPMBuild().toBuilder().attributes(Map.of(Attributes.BUILD_BREW_NAME, NPM_BUILD_NAME)).build();
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        ArtifactRef artifact = createNPMArtifact("32");
        buildArtifacts.getBuildArtifacts().add(artifact);

        BrewNVR nvr = importController.getNVR(build, buildArtifacts);
        assertEquals(NPM_BUILD_NAME, nvr.getName());
        assertEquals(NPM_VERSION_KOJI_STYLE, nvr.getVersion());
    }

    private Build mockMVNBuild() throws CausewayException {
        String buildId = String.valueOf(generator.nextInt());
        Integer id = generator.nextInt();

        Environment env = createEnvironment(BuildType.MVN);

        // Mock BuildConfigurationAudited
        BuildConfigurationRevision bcar = createBuildConfiguration(id, env, BuildType.MVN);

        // Mock BuildRecord
        Build buildRecord = createBuildRecord(buildId, bcar, MVN_BUILD_NAME, MVN_VERSION);

        // Mock BuildArtifacts
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        buildArtifacts.getBuildArtifacts().add(createArtifact(buildId));

        doReturn(buildRecord).when(pncClient).findBuild(eq(buildId));
        doReturn(buildArtifacts).when(pncClient).findBuildArtifacts(eq(buildId));
        doAnswer(i -> mockBARF("Log of build " + i.getArguments()[0])).when(pncClient).getBuildLog(anyString());
        doAnswer(i -> mockBARF("Log of alignment " + i.getArguments()[0])).when(pncClient).getAlignLog(anyString());
        return buildRecord;
    }

    private Build mockNPMBuild() throws CausewayException {
        String buildId = String.valueOf(generator.nextInt());
        Integer id = generator.nextInt();

        Environment env = createEnvironment(BuildType.NPM);

        // Mock BuildConfigurationAudited
        BuildConfigurationRevision bcar = createBuildConfiguration(id, env, BuildType.NPM);

        // Mock BuildRecord
        Build buildRecord = createBuildRecord(buildId, bcar, NPM_BUILD_NAME, NPM_VERSION);

        // Mock BuildArtifacts
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        buildArtifacts.getBuildArtifacts().add(createArtifact(buildId));

        doReturn(buildRecord).when(pncClient).findBuild(eq(buildId));
        doReturn(buildArtifacts).when(pncClient).findBuildArtifacts(eq(buildId));
        doAnswer(i -> mockBARF("Log of build " + i.getArguments()[0])).when(pncClient).getBuildLog(anyString());
        doAnswer(i -> mockBARF("Log of alignment " + i.getArguments()[0])).when(pncClient).getAlignLog(anyString());
        return buildRecord;
    }

    private Environment createEnvironment(BuildType buildType) {
        // Mock BuildEnvironment
        Map<String, String> attrs = new HashMap<>();
        attrs.put("OS", "Fedora25");
        switch (buildType) {
            case MVN:
            case GRADLE:
                attrs.put("JDK", "1.8");
                break;
            case NPM:
                attrs.put("NPM", "5");
        }

        return Environment.builder().attributes(attrs).systemImageType(SystemImageType.DOCKER_IMAGE).build();
    }

    private BuildConfigurationRevision createBuildConfiguration(Integer id, Environment env, BuildType buildType) {
        SCMRepository scm = SCMRepository.builder().id(String.valueOf(1)).internalUrl("http://repo.url").build();

        return BuildConfigurationRevision.builder()
                .id(String.valueOf(id))
                .rev(1)
                .scmRepository(scm)
                .buildType(buildType)
                .scmRevision("r21345")
                .environment(env)
                .build();
    }

    private Build createBuildRecord(
            String buildId,
            BuildConfigurationRevision bcar,
            String brewBuildName,
            String brewBuildVersion) {
        User user = User.builder().id(String.valueOf(1)).build();

        Date submit = new Date();
        Date start = new Date(submit.getTime() + 1000L);
        Date end = new Date(start.getTime() + 100000L);
        Map<String, String> attributes = new HashMap<>();
        if (brewBuildName != null) {
            attributes.put(Attributes.BUILD_BREW_NAME, brewBuildName);
        }
        if (brewBuildVersion != null) {
            attributes.put(Attributes.BUILD_BREW_VERSION, brewBuildVersion);
        }
        return Build.builder()
                .id(String.valueOf(buildId))
                .status(BuildStatus.SUCCESS)
                .submitTime(submit.toInstant())
                .startTime(start.toInstant())
                .endTime(end.toInstant())
                .user(user)
                .attributes(attributes)
                .buildConfigRevision(bcar)
                .build();
    }

    private static ArtifactRef createArtifact(String id) {
        String filename = MVN_ARTIFACT_ID + "-" + MVN_VERSION + ".pom";
        String path = MVN_GROUP_ID.replace('.', '/') + "/" + MVN_ARTIFACT_ID + "/" + MVN_VERSION + "/" + filename;
        return ArtifactRef.refBuilder()
                .id(id)
                .identifier(MVN_GROUP_ID + ":" + MVN_ARTIFACT_ID + ":pom:" + MVN_VERSION)
                .filename(filename)
                .md5("bedf8af1b107b36c72f52009e6fcc768")
                .deployUrl("https://example.com/api/content/mvn/hosted/shared-imports/" + path)
                .deployPath("/" + path)
                .size(13245L)
                .artifactQuality(ArtifactQuality.NEW)
                .build();
    }

    private static ArtifactRef createNPMArtifact(String id) {
        String filename = NPM_ARTIFACT_NAME + "-" + NPM_VERSION + ".tgz";
        String path = "@" + NPM_ARTIFACT_SCOPE + "/" + NPM_ARTIFACT_NAME + "/-/" + filename;
        return ArtifactRef.refBuilder()
                .id(id)
                .identifier("@" + NPM_ARTIFACT_SCOPE + "/" + NPM_ARTIFACT_NAME + ":" + NPM_VERSION)
                .filename(filename)
                .md5("bedf8af1b107b36c72f52009e6fcc768")
                .deployUrl("https://example.com/api/content/npm/hosted/shared-imports/" + path)
                .deployPath("/" + path)
                .size(13245L)
                .artifactQuality(ArtifactQuality.NEW)
                .build();
    }

    private BurnAfterReadingFile mockBARF(String content) throws IOException {
        BurnAfterReadingFile file = Mockito.mock(BurnAfterReadingFile.class);
        Mockito.when(file.read()).then(invocationOnMock -> new ByteArrayInputStream(content.getBytes()));
        return file;
    }

    private void mockBrew() throws CausewayException {
        doReturn(true).when(brewClient).tagsExists(eq(TAG_PREFIX));
        when(brewClient.getBuildUrl(anyInt())).then(inv -> kojiBuildUrl + inv.getArguments()[0]);
        doNothing().when(brewClient).tagBuild(eq(TAG_PREFIX), any());
    }

    private void mockTranslator() throws CausewayException {
        doReturn(KOJI_IMPORT).when(translator).translate(eq(MVN_NVR), any(), any(), any(), any(), any(), any());
        doReturn(IMPORT_FILE_GENERATOR).when(translator).getImportFiles(any(), any(), any(), any());
        doReturn("/path/to/sources.tar.gz").when(translator).getSourcesDeployPath(any(), any());
    }

    private BrewBuild mockExistingBuild(int id, BrewNVR nvr, boolean tagged) throws Exception {
        BrewBuild brewBuild = new BrewBuild(id, nvr);
        when(brewClient.findBrewBuildOfNVR(eq(nvr))).thenReturn(brewBuild);
        when(brewClient.findBrewBuild(eq(id))).thenReturn(brewBuild);
        when(brewClient.isBuildTagged(eq(TAG_PREFIX), same(brewBuild))).thenReturn(tagged);
        return brewBuild;
    }
}
