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

import com.redhat.red.build.koji.model.json.KojiImport;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.bpmclient.BPMClient;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BuildTranslatorImpl;
import org.jboss.pnc.causeway.brewclient.StringLogImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.pncclient.model.BuildConfigurationAuditedRest;
import org.jboss.pnc.causeway.pncclient.model.BuildCoordinationStatus;
import org.jboss.pnc.causeway.pncclient.model.BuildEnvironmentRest;
import org.jboss.pnc.causeway.pncclient.model.BuildRecordRest;
import org.jboss.pnc.causeway.pncclient.model.IdRev;
import org.jboss.pnc.causeway.pncclient.model.RepositoryConfigurationRest;
import org.jboss.pnc.causeway.pncclient.model.SystemImageType;
import org.jboss.pnc.causeway.pncclient.model.UserRest;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.CallbackMethod;
import org.jboss.pnc.causeway.rest.CallbackTarget;
import org.jboss.pnc.causeway.rest.model.response.ArtifactImportError;
import org.jboss.pnc.causeway.rest.pnc.BuildImportResultRest;
import org.jboss.pnc.causeway.rest.pnc.BuildImportStatus;
import org.jboss.pnc.causeway.rest.pnc.MilestoneReleaseResultRest;
import org.jboss.pnc.causeway.rest.pnc.ReleaseStatus;
import org.junit.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import org.jboss.pnc.causeway.pncclient.model.ArtifactRest;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class PncImportControllerTest {

    private static final String USERNAME = "joe";
    private static final String TAG_PREFIX = "pnc-foo-0.1";
    private static final String EXEC_ROOT_VERSION = "1.1.1";
    private static final String EXEC_ROOT_NAME = "test:artifact";
    private static final String CALLBACK_ID = "callbackId";
    private static final String CALLBACK_URL = "http://dummy.org";
    private static final CallbackTarget CALLBACK_TARGET = new CallbackTarget(CALLBACK_URL, CallbackMethod.PUT);

    private static final BrewNVR NVR = new BrewNVR(EXEC_ROOT_NAME, EXEC_ROOT_VERSION, "1");

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
    public MetricRegistry metricRegistry;

    @Mock
    public BuildTranslatorImpl translator;
    
    @InjectMocks
    private PncImportControllerImpl importController;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(metricRegistry.meter(anyString())).thenReturn(mock(Meter.class));
        Timer timer = mock(Timer.class);
        when(metricRegistry.timer(anyString())).thenReturn(timer);
        when(timer.time()).thenReturn(mock(Timer.Context.class));
        //importController = new PncImportControllerImpl(pncClient, brewClient, bpmClient, translator, causewayConfig);
    }

    private void mockPNC(Integer milestoneId) throws CausewayException{
        mockPNC(milestoneId, generator.nextInt());
    }
    
    private void mockPNC(Integer milestoneId, Integer buildId) throws CausewayException{
        Integer id = generator.nextInt();

        // Mock BuildEnvironment
        Map<String, String> attrs = new HashMap();
        attrs.put("OS", "Fedora25");
        attrs.put("JDK", "1.8");
        BuildEnvironmentRest env = new BuildEnvironmentRest();
        env.setAttributes(attrs);
        env.setSystemImageType(SystemImageType.DOCKER_IMAGE);

        // Mock RepositoryConfiguration
        RepositoryConfigurationRest rc = RepositoryConfigurationRest.builder()
                .id(1)
                .internalUrl("http://repo.url")
                .build();

        // Mock BuildConfigurationAudited
        BuildConfigurationAuditedRest bcar = new BuildConfigurationAuditedRest();
        bcar.setIdRev(new IdRev(id, 1));
        bcar.setRepositoryConfiguration(rc);
        bcar.setScmRevision("r21345");
        bcar.setEnvironment(env);
        
        // Mock UserRest
        UserRest user = new UserRest();
        user.setId(1);

        // Mock BuildRecord
        Date submit = new Date();
        Date start = new Date(submit.getTime() + 1000L);
        Date end = new Date(start.getTime() + 100000L);
        BuildRecordRest buildRecordRest = new BuildRecordRest(buildId, BuildCoordinationStatus.BUILD_COMPLETED, submit, start, end, user, bcar);
        buildRecordRest.setExecutionRootName(EXEC_ROOT_NAME);
        buildRecordRest.setExecutionRootVersion(EXEC_ROOT_VERSION);

        Collection<BuildRecordRest> buildRecords = new HashSet<>();
        buildRecords.add(buildRecordRest);
        
        // Mock BuildArtifacts
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        buildArtifacts.buildArtifacts.add(
                new BuildArtifacts.PncArtifact(
                        buildId,
                        "maven",
                        "org.apache.geronimo.specs:geronimo-annotation_1.0_spec:1.1.1.redhat-1:pom",
                        "geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom",
                        "bedf8af1b107b36c72f52009e6fcc768",
                        "http://pnc-indy-branch-nightly.cloud.pnc.devel.engineering.redhat.com/api/hosted/"
                            + "build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/org/apache/geronimo/specs/geronimo-annotation_1.0_spec/1.1.1.redhat-1/geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom",
                        13245,
                        ArtifactRest.Quality.NEW
                )
        );

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));
        doReturn(TAG_PREFIX).when(pncClient).getTagForMilestone(eq(milestoneId));
        doReturn(buildArtifacts).when(pncClient).findBuildArtifacts(eq(buildId));
        doAnswer(i -> "Log of build " + i.getArguments()[0]).when(pncClient).getBuildLog(anyInt());
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
        mockPNC(milestoneId);
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
        mockPNC(milestoneId, buildId);
        mockBrew();
        mockTranslator();

        // Mock Brew import
        BuildImportResultRest buildImportResultRest = new BuildImportResultRest(buildId, 11, "https://koji.myco.com/brew/buildinfo?buildID=11", BuildImportStatus.SUCCESSFUL, null, null);
        doReturn(buildImportResultRest).when(brewClient).importBuild(eq(NVR), eq(buildId), same(KOJI_IMPORT), same(IMPORT_FILE_GENERATOR));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        verifySuccess();
    }

    @Test
    public void testImportProductReleaseWithPncReleaseNotFound() throws Exception {
        Integer milestoneId = generator.nextInt();

        // Test setup
        mockPNC(milestoneId);
        mockBrew();

        // Mock exception from PNC client
        final String exceptionMessage = "Test Exception";
        doThrow(new RuntimeException(exceptionMessage)).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        MilestoneReleaseResultRest result = verifyFailure();
        //        that user gets the exception message
        assertNotNull(result.getErrorMessage());
        assertTrue("Error message doesn't contain expected data", result.getErrorMessage().contains(exceptionMessage));
    }

    @Test
    public void testImportProductReleaseWithEmptyBuildConfigurations() throws Exception {
        Integer milestoneId = generator.nextInt();

        // Test setup
        mockPNC(milestoneId);
        mockBrew();

        // Mock no builds in milestone
        Collection<BuildRecordRest> buildRecords = new HashSet<>();

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
        mockPNC(milestoneId, buildId);
        mockBrew();

        // Mock exception from Brew Client
        doThrow(new CausewayException(exceptionMessage)).when(brewClient).findBrewBuildOfNVR(eq(NVR));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        MilestoneReleaseResultRest result = verifyFailure();
        //        that the build is in the result with error
        assertNotNull(result.getBuilds());
        assertEquals(1, result.getBuilds().size());
        BuildImportResultRest buildImportResultRest = result.getBuilds().get(0);
        assertEquals(buildId, buildImportResultRest.getBuildRecordId());
        assertEquals(BuildImportStatus.ERROR, buildImportResultRest.getStatus());
        //        that user gets the exception message
        assertNotNull(buildImportResultRest.getErrorMessage());
        assertTrue("Build error message doesn't contain expected data", buildImportResultRest.getErrorMessage().contains(exceptionMessage));
    }

    @Test
    public void testImportProductReleaseWithArtifactImportErrors() throws Exception {
        Integer milestoneId = generator.nextInt();
        Integer buildId = generator.nextInt();
        String errorMessage = "Artifact import error";

        // Test setup
        mockPNC(milestoneId, buildId);
        mockBrew();
        mockTranslator();

        List<ArtifactImportError> artifactImportErrors = new ArrayList<>();
        ArtifactImportError importError = ArtifactImportError.builder()
                .artifactId(123)
                .errorMessage(errorMessage)
                .build();
        artifactImportErrors.add(importError);
        BuildImportResultRest buildImportResultRest = new BuildImportResultRest(buildId, 11, "https://koji.myco.com/brew/buildinfo?buildID=11", BuildImportStatus.FAILED, null, artifactImportErrors);
        
        doReturn(buildImportResultRest).when(brewClient).importBuild(eq(NVR), eq(buildId), same(KOJI_IMPORT), same(IMPORT_FILE_GENERATOR));

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        MilestoneReleaseResultRest result = verifyFailure(ReleaseStatus.IMPORT_ERROR);
        //        that the build is in the result with error
        assertNotNull(result.getBuilds());
        assertEquals(1, result.getBuilds().size());
        BuildImportResultRest buildResult = result.getBuilds().get(0);
        assertEquals(buildId, buildResult.getBuildRecordId());
        assertEquals(BuildImportStatus.FAILED, buildResult.getStatus());
        //        that the aritfact import error is present
        assertNotNull(buildImportResultRest.getErrors());
        assertEquals(1, buildImportResultRest.getErrors().size());
        ArtifactImportError resultImportError = buildImportResultRest.getErrors().get(0);
        //        that user gets the exception message
        assertNotNull(resultImportError.getErrorMessage());
        assertTrue("Build error message doesn't contain expected data", resultImportError.getErrorMessage().contains(errorMessage));
    }

    @Test
    public void testImportProductWhenTagDoesNotExistInBrew() throws Exception {
        Integer milestoneId = generator.nextInt();

        // Test setup
        mockPNC(milestoneId);
        mockBrew();
        doReturn(false).when(brewClient).tagsExists(TAG_PREFIX);

        // Run import
        importController.importMilestone(milestoneId, CALLBACK_TARGET, CALLBACK_ID, USERNAME);

        // Verify
        MilestoneReleaseResultRest result = verifyFailure();
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains(TAG_PREFIX)); // We inform user about missing tag
    }

    private void verifySuccess() {
        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);
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
        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);
        verify(bpmClient).failure(eq(CALLBACK_URL), eq(CALLBACK_ID), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(expectedStatus, milestoneReleaseResultRest.getReleaseStatus());
        assertFalse(milestoneReleaseResultRest.isSuccessful());
        return milestoneReleaseResultRest;
    }

    public static String createRandomString() {
        return "" + generator.nextLong();
    }
}