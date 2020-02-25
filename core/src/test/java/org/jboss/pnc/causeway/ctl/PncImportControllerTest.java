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
package org.jboss.pnc.causeway.ctl;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.redhat.red.build.koji.model.json.KojiImport;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.bpmclient.BPMClient;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BuildTranslatorImpl;
import org.jboss.pnc.causeway.brewclient.StringLogImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.pncmetrics.MetricsConfiguration;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.CallbackMethod;
import org.jboss.pnc.causeway.rest.CallbackTarget;
import org.jboss.pnc.causeway.rest.pnc.BuildImportResultRest;
import org.jboss.pnc.causeway.rest.pnc.BuildImportStatus;
import org.jboss.pnc.causeway.rest.pnc.MilestoneReleaseResultRest;
import org.jboss.pnc.causeway.rest.pnc.ReleaseStatus;
import org.jboss.pnc.dto.ArtifactImportError;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.SCMRepository;
import org.jboss.pnc.dto.User;
import org.jboss.pnc.enums.ArtifactQuality;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.enums.SystemImageType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.jboss.pnc.constants.Attributes.BUILD_BREW_NAME;
import static org.jboss.pnc.constants.Attributes.BUILD_BREW_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PncImportControllerTest {

    private static final String USERNAME = "joe";
    private static final String TAG_PREFIX = "pnc-foo-0.1";
    private static final String BREW_BUILD_VERSION = "1.1.1";
    private static final String BREW_BUILD_NAME = "test:artifact";
    private static final String CALLBACK_ID = "callbackId";
    private static final String CALLBACK_URL = "http://dummy.org";
    private static final CallbackTarget CALLBACK_TARGET = new CallbackTarget(CALLBACK_URL, CallbackMethod.PUT);

    private static final BrewNVR NVR = new BrewNVR(BREW_BUILD_NAME, BREW_BUILD_VERSION, "1");

    private static final StringLogImportFileGenerator IMPORT_FILE_GENERATOR = mock(StringLogImportFileGenerator.class);
    private static final KojiImport KOJI_IMPORT = mock(KojiImport.class);

    private static final Random generator = new Random();
    @Mock
    private PncClient pncClient;
    @Mock
    private BrewClient brewClient;
    @Mock
    private BPMClient bpmClient;
    @Mock
    private CausewayConfig causewayConfig;

    @Mock
    public MetricsConfiguration metricsConfiguration;

    @Mock
    public MetricRegistry metricRegistry;

    @Mock
    public BuildTranslatorImpl translator;

    @InjectMocks
    private PncImportControllerImpl importController;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(metricsConfiguration.getMetricRegistry()).thenReturn(metricRegistry);
        when(metricRegistry.meter(anyString())).thenReturn(mock(Meter.class));
        Timer timer = mock(Timer.class);
        when(metricRegistry.timer(anyString())).thenReturn(timer);
        when(timer.time()).thenReturn(mock(Timer.Context.class));
        Histogram histogram = mock(Histogram.class);
        when(metricRegistry.register(anyString(), any(Histogram.class))).thenReturn(histogram);
        when(metricRegistry.histogram(anyString())).thenReturn(histogram);
        // importController = new PncImportControllerImpl(pncClient, brewClient, bpmClient, translator, causewayConfig);
    }

    private void mockPNC(Integer milestoneId, BuildType buildType) throws CausewayException {
        mockPNC(milestoneId, generator.nextInt(), buildType);
    }

    private void mockPNC(Integer milestoneId, Integer buildId, BuildType buildType) throws CausewayException {
        Integer id = generator.nextInt();

        Environment env = createEnvironment(buildType);

        // Mock BuildConfigurationAudited
        BuildConfigurationRevision bcar = createBuildConfiguration(id, env, buildType);

        // Mock BuildRecord
        Build buildRecord = createBuildRecord(buildId, bcar, BREW_BUILD_NAME, BREW_BUILD_VERSION);

        Collection<Build> buildRecords = new HashSet<>();
        buildRecords.add(buildRecord);

        // Mock BuildArtifacts
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        buildArtifacts.buildArtifacts.add(createArtifact(buildId));

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));
        doReturn(TAG_PREFIX).when(pncClient).getTagForMilestone(eq(milestoneId));
        doReturn(buildArtifacts).when(pncClient).findBuildArtifacts(eq(buildId));
        doAnswer(i -> "Log of build " + i.getArguments()[0]).when(pncClient).getBuildLog(anyInt());
    }

    private Environment createEnvironment(BuildType buildType) {
        // Mock BuildEnvironment
        Map<String, String> attrs = new HashMap();
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

    private void mockBrew() throws CausewayException {
        doReturn(true).when(brewClient).tagsExists(eq(TAG_PREFIX));
        doNothing().when(brewClient).tagBuild(TAG_PREFIX, NVR);
    }

    private void mockTranslator() throws CausewayException {
        doReturn(KOJI_IMPORT).when(translator).translate(eq(NVR), any(), any(), anyString(), any());
        doReturn(IMPORT_FILE_GENERATOR).when(translator).getImportFiles(any(), anyString());
    }

    @Test
    public void testImportProductMilestoneWithExistingBrewBuildIsImported() throws Exception {
        Integer milestoneId = generator.nextInt();

        // Test setup
        mockPNC(milestoneId, BuildType.MVN);
        mockBrew();

        // Mock existing Brew build
        doReturn(new BrewBuild(11, NVR)).when(brewClient).findBrewBuildOfNVR(eq(NVR));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        verifySuccess();
    }

    @Test
    public void testImportProductMilestoneWithNonExistingBrewBuildIsImported() throws Exception {
        Integer milestoneId = generator.nextInt();
        Integer buildId = generator.nextInt();

        // Test setup
        mockPNC(milestoneId, buildId, BuildType.MVN);
        mockBrew();
        mockTranslator();

        // Mock Brew import
        BuildImportResultRest buildImportResultRest = new BuildImportResultRest(
                buildId,
                11,
                "https://koji.myco.com/brew/buildinfo?buildID=11",
                BuildImportStatus.SUCCESSFUL,
                null,
                null);
        doReturn(buildImportResultRest).when(brewClient)
                .importBuild(eq(NVR), eq(buildId), same(KOJI_IMPORT), same(IMPORT_FILE_GENERATOR));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        verifySuccess();
    }

    @Test
    public void testImportProductReleaseWithPncReleaseNotFound() throws Exception {
        Integer milestoneId = generator.nextInt();

        // Test setup
        mockPNC(milestoneId, BuildType.MVN);
        mockBrew();

        // Mock exception from PNC client
        final String exceptionMessage = "Test Exception";
        doThrow(new RuntimeException(exceptionMessage)).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        MilestoneReleaseResultRest result = verifyFailure();
        // that user gets the exception message
        assertNotNull(result.getErrorMessage());
        assertTrue("Error message doesn't contain expected data", result.getErrorMessage().contains(exceptionMessage));
    }

    @Test
    public void testImportProductReleaseWithEmptyBuildConfigurations() throws Exception {
        Integer milestoneId = generator.nextInt();

        // Test setup
        mockPNC(milestoneId, BuildType.MVN);
        mockBrew();

        // Mock no builds in milestone
        Collection<Build> buildRecords = new HashSet<>();

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        verifyFailure();
        // TODO verify the error message somehow?
    }

    @Test
    public void testImportProductReleaseWhereImportBuildThrowsException() throws Exception {
        Integer milestoneId = generator.nextInt();
        Integer buildId = generator.nextInt();
        String exceptionMessage = "Import build failed";

        // Test setup
        mockPNC(milestoneId, buildId, BuildType.MVN);
        mockBrew();

        // Mock exception from Brew Client
        doThrow(new CausewayException(exceptionMessage)).when(brewClient).findBrewBuildOfNVR(eq(NVR));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        MilestoneReleaseResultRest result = verifyFailure();
        // that the build is in the result with error
        assertNotNull(result.getBuilds());
        assertEquals(1, result.getBuilds().size());
        BuildImportResultRest buildImportResultRest = result.getBuilds().get(0);
        assertEquals(buildId, buildImportResultRest.getBuildRecordId());
        assertEquals(BuildImportStatus.ERROR, buildImportResultRest.getStatus());
        // that user gets the exception message
        assertNotNull(buildImportResultRest.getErrorMessage());
        assertTrue(
                "Build error message doesn't contain expected data",
                buildImportResultRest.getErrorMessage().contains(exceptionMessage));
    }

    @Test
    public void testImportProductReleaseWithArtifactImportErrors() throws Exception {
        Integer milestoneId = generator.nextInt();
        Integer buildId = generator.nextInt();
        String errorMessage = "Artifact import error";

        // Test setup
        mockPNC(milestoneId, buildId, BuildType.MVN);
        mockBrew();
        mockTranslator();

        List<ArtifactImportError> artifactImportErrors = new ArrayList<>();
        ArtifactImportError importError = ArtifactImportError.builder()
                .artifactId(String.valueOf(123))
                .errorMessage(errorMessage)
                .build();
        artifactImportErrors.add(importError);
        BuildImportResultRest buildImportResultRest = new BuildImportResultRest(
                buildId,
                11,
                "https://koji.myco.com/brew/buildinfo?buildID=11",
                BuildImportStatus.FAILED,
                null,
                artifactImportErrors);

        doReturn(buildImportResultRest).when(brewClient)
                .importBuild(eq(NVR), eq(buildId), same(KOJI_IMPORT), same(IMPORT_FILE_GENERATOR));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        MilestoneReleaseResultRest result = verifyFailure(ReleaseStatus.IMPORT_ERROR);
        // that the build is in the result with error
        assertNotNull(result.getBuilds());
        assertEquals(1, result.getBuilds().size());
        BuildImportResultRest buildResult = result.getBuilds().get(0);
        assertEquals(buildId, buildResult.getBuildRecordId());
        assertEquals(BuildImportStatus.FAILED, buildResult.getStatus());
        // that the aritfact import error is present
        assertNotNull(buildImportResultRest.getErrors());
        assertEquals(1, buildImportResultRest.getErrors().size());
        ArtifactImportError resultImportError = buildImportResultRest.getErrors().get(0);
        // that user gets the exception message
        assertNotNull(resultImportError.getErrorMessage());
        assertTrue(
                "Build error message doesn't contain expected data",
                resultImportError.getErrorMessage().contains(errorMessage));
    }

    @Test
    public void testImportProductWhenTagDoesNotExistInBrew() throws Exception {
        Integer milestoneId = generator.nextInt();

        // Test setup
        mockPNC(milestoneId, BuildType.MVN);
        mockBrew();
        doReturn(false).when(brewClient).tagsExists(TAG_PREFIX);

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        MilestoneReleaseResultRest result = verifyFailure();
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains(TAG_PREFIX)); // We inform user about missing tag
    }

    @Test
    public void testGetNVR() throws IOException, CausewayException, ReflectiveOperationException {
        Environment env = createEnvironment(BuildType.MVN);
        BuildConfigurationRevision bcar = createBuildConfiguration(1, env, BuildType.MVN);
        Build buildRecordRest = createBuildRecord(1, bcar, BREW_BUILD_NAME, BREW_BUILD_VERSION);
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        buildArtifacts.buildArtifacts.add(createArtifact(1));

        BrewNVR nvr = importController.getNVR(buildRecordRest, buildArtifacts);
        assertEquals(BREW_BUILD_NAME, nvr.getName());
        assertEquals(BREW_BUILD_VERSION, nvr.getVersion());

        buildRecordRest = createBuildRecord(1, bcar, BREW_BUILD_NAME, null);
        nvr = importController.getNVR(buildRecordRest, buildArtifacts);
        assertEquals(BREW_BUILD_NAME, nvr.getName());
        assertEquals("1.1.1.redhat_1", nvr.getVersion());

        buildRecordRest = createBuildRecord(1, bcar, null, null);
        try {
            importController.getNVR(buildRecordRest, buildArtifacts);
            fail("Expected CausewayException to be thrown.");
        } catch (CausewayException ex) {
            // ok
        }
    }

    @Test
    public void testAutomaticVersionNPM() throws CausewayException {
        Environment env = createEnvironment(BuildType.NPM);
        BuildConfigurationRevision bcar = createBuildConfiguration(1, env, BuildType.NPM);
        Build build = createBuildRecord(1, bcar, BREW_BUILD_NAME, null);
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        buildArtifacts.buildArtifacts.add(createNpmArtifact(1));

        BrewNVR nvr = importController.getNVR(build, buildArtifacts);

        assertEquals(BREW_BUILD_NAME, nvr.getName());
        assertEquals("0.1.18.redhat_1", nvr.getVersion());
    }

    private void verifySuccess() {
        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor
                .forClass(MilestoneReleaseResultRest.class);
        verify(bpmClient).success(eq(CALLBACK_URL), eq(CALLBACK_ID), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(ReleaseStatus.SUCCESS, milestoneReleaseResultRest.getReleaseStatus());
        assertTrue(milestoneReleaseResultRest.isSuccessful());
        assertNull(milestoneReleaseResultRest.getErrorMessage());
    }

    private MilestoneReleaseResultRest verifyFailure() {
        return verifyFailure(ReleaseStatus.SET_UP_ERROR);
    }

    private MilestoneReleaseResultRest verifyFailure(ReleaseStatus expectedStatus) {
        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor
                .forClass(MilestoneReleaseResultRest.class);
        verify(bpmClient).failure(eq(CALLBACK_URL), eq(CALLBACK_ID), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(expectedStatus, milestoneReleaseResultRest.getReleaseStatus());
        assertFalse(milestoneReleaseResultRest.isSuccessful());
        return milestoneReleaseResultRest;
    }

    private static BuildArtifacts.PncArtifact createArtifact(Integer buildId) {
        return new BuildArtifacts.PncArtifact(
                buildId,
                "org.apache.geronimo.specs:geronimo-annotation_1.0_spec:pom:1.1.1.redhat-1",
                "geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom",
                "bedf8af1b107b36c72f52009e6fcc768",
                "http://ulozto.cz/api/hosted/"
                        + "build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/org/apache/geronimo/specs/geronimo-annotation_1.0_spec/1.1.1.redhat-1/geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom",
                13245,
                ArtifactQuality.NEW);
    }

    private static BuildArtifacts.PncArtifact createNpmArtifact(Integer buildId) {
        return new BuildArtifacts.PncArtifact(
                buildId,
                "async:0.1.18.redhat-1",
                "async-0.1.18.redhat-1-wow-much-good.tar-gz",
                "dsdfs1dfs6ads588few98ncv98465ew2",
                "http://ulozto.cz/path/to/deploy/" + "async/async-0.1.18.redhat-1-wow-much-good.tar-gz",
                1337,
                ArtifactQuality.NEW);
    }

    private Build createBuildRecord(
            Integer buildId,
            BuildConfigurationRevision bcar,
            String brewBuildName,
            String brewBuildVersion) {
        User user = User.builder().id(String.valueOf(1)).build();

        Date submit = new Date();
        Date start = new Date(submit.getTime() + 1000L);
        Date end = new Date(start.getTime() + 100000L);
        Map<String, String> attributes = new HashMap<>();
        if (brewBuildName != null) {
            attributes.put(BUILD_BREW_NAME, brewBuildName);
        }
        if (brewBuildVersion != null) {
            attributes.put(BUILD_BREW_VERSION, brewBuildVersion);
        }
        Build buildRecord = Build.builder()
                .id(String.valueOf(buildId))
                .status(BuildStatus.SUCCESS)
                .submitTime(submit.toInstant())
                .startTime(start.toInstant())
                .endTime(end.toInstant())
                .user(user)
                .attributes(attributes)
                .buildConfigRevision(bcar)
                .build();
        return buildRecord;
    }

    public static String createRandomString() {
        return "" + generator.nextLong();
    }
}
