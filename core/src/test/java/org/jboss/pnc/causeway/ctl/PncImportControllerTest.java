/*
 * Copyright 2016 Honza Brázdil <jbrazdil@redhat.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.jboss.pnc.causeway.brewclient.BuildTranslator;
import org.jboss.pnc.causeway.brewclient.BuildTranslatorImpl;
import org.jboss.pnc.causeway.brewclient.ImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.CallbackMethod;
import org.jboss.pnc.causeway.rest.CallbackTarget;
import org.jboss.pnc.model.BuildConfigurationAudited;
import org.jboss.pnc.model.BuildEnvironment;
import org.jboss.pnc.model.BuildRecord;
import org.jboss.pnc.model.IdRev;
import org.jboss.pnc.model.SystemImageType;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.causeway.ArtifactImportError;
import org.jboss.pnc.rest.restmodel.causeway.BuildImportResultRest;
import org.jboss.pnc.rest.restmodel.causeway.BuildImportStatus;
import org.jboss.pnc.rest.restmodel.causeway.MilestoneReleaseResultRest;
import org.jboss.pnc.rest.restmodel.causeway.ReleaseStatus;
import org.junit.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class PncImportControllerTest {

    private PncImportController importController;
    @Mock
    private PncClient pncClient;
    @Mock
    private BrewClient brewClient;
    @Mock
    private BPMClient bpmClient;
    @Mock
    private CausewayConfig causewayConfig;

    public BuildTranslator translator = new BuildTranslatorImpl();

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        importController = new PncImportControllerImpl(pncClient, brewClient, bpmClient, translator, causewayConfig);
    }

    @Test
    public void testImportProductMilestoneWithExistingBrewBuildIsImported() throws Exception {
        Integer milestoneId = createRandomInt();
        Integer id = createRandomInt();

        // Test setup
        Collection<BuildRecordRest> buildRecords = new HashSet<>();
        BuildRecordRest buildRecordRest = new BuildRecordRest();
        buildRecordRest.setId(id);
        buildRecordRest.setExecutionRootName("Test Execution Name");
        buildRecordRest.setExecutionRootVersion("Test-execution-root-version");
        buildRecords.add(buildRecordRest);

        BrewNVR brewNVR = new BrewNVR(buildRecordRest.getExecutionRootName(), buildRecordRest.getExecutionRootVersion().replace( '-', '_' ), "1");

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));
        doReturn(new BrewBuild(11, brewNVR)).when(brewClient).findBrewBuildOfNVR(eq(brewNVR));
        doNothing().when(brewClient).tagBuild("brewTag", brewNVR);

        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);

        // Run import
        importController.importMilestone(milestoneId, new CallbackTarget("http://dummy.org", CallbackMethod.PUT), "callbackId");

        verify(bpmClient).success(anyString(), anyString(), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(ReleaseStatus.SUCCESS, milestoneReleaseResultRest.getReleaseStatus());
        assertTrue(milestoneReleaseResultRest.isSuccessful());
        assertNull(milestoneReleaseResultRest.getErrorMessage());
    }

    @Test
    public void testImportProductMilestoneWithNonExistingBrewBuildIsImported() throws Exception {
        Integer milestoneId = createRandomInt();
        Integer id = createRandomInt();

        // Test setup

        // Mock BuildEnvironment
        Map<String, String> attrs = new HashMap();
        attrs.put("OS", "Fedora25");
        attrs.put("JDK", "1.8");
        BuildEnvironment bEnv = new BuildEnvironment();
        bEnv.setAttributes(attrs);
        bEnv.setSystemImageType(SystemImageType.DOCKER_IMAGE);

        // Mock BuildConfigurationAudited
        BuildConfigurationAudited bcar = new BuildConfigurationAudited();
        bcar.setIdRev(new IdRev(id, 1));
        bcar.setScmRepoURL("http://repo.url");
        bcar.setScmRevision("r21345");
        bcar.setBuildEnvironment(bEnv);

        // Mock BuildRecord
        BuildRecord br = new BuildRecord();
        br.setId(id);
        Date start = new Date();
        br.setStartTime(start);
        br.setEndTime(new Date(start.getTime() + 100000L));
        br.setExecutionRootName("test:artifact");
        br.setExecutionRootVersion("1.1.1");
        br.setBuildConfigurationAudited(bcar);

        Collection<BuildRecordRest> buildRecords = new HashSet<>();
        BuildRecordRest buildRecordRest = new BuildRecordRest(br);
        buildRecords.add(buildRecordRest);

        BrewNVR brewNVR = new BrewNVR(buildRecordRest.getExecutionRootName(), buildRecordRest.getExecutionRootVersion().replace( '-', '_' ), "1");

        // In this test we don't mock the BrewBuild, so need to mock BuildArtifacts instead
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        buildArtifacts.buildArtifacts.add(
                new BuildArtifacts.PncArtifact(
                        2371,
                        "maven",
                        "org.apache.geronimo.specs:geronimo-annotation_1.0_spec:1.1.1.redhat-1:pom",
                        "geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom",
                        "bedf8af1b107b36c72f52009e6fcc768",
                        "http://pnc-indy-branch-nightly.cloud.pnc.devel.engineering.redhat.com/api/hosted/"
                            + "build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/org/apache/geronimo/specs/geronimo-annotation_1.0_spec/1.1.1.redhat-1/geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom",
                        13245
                )
        );

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));
        doReturn(buildArtifacts).when(pncClient).findBuildArtifacts(eq(id));
        doNothing().when(brewClient).tagBuild("brewTag", brewNVR);

        KojiImport kojiImport = translator.translate(brewNVR, buildRecordRest, buildArtifacts);
        ImportFileGenerator importFiles = translator.getImportFiles(buildArtifacts);

        BuildImportResultRest buildImportResultRest = new BuildImportResultRest(id, 11, "https://koji.myco.com/brew/buildinfo?buildID=11", BuildImportStatus.SUCCESSFUL, null, null);
        doReturn(buildImportResultRest).when(brewClient).importBuild(eq(brewNVR), eq(id), eq(kojiImport), eq(importFiles));

        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);
        // Run import
        importController.importMilestone(milestoneId, new CallbackTarget("http://dummy.org", CallbackMethod.PUT), "callbackId");

        verify(bpmClient).success(anyString(), anyString(), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(ReleaseStatus.SUCCESS, milestoneReleaseResultRest.getReleaseStatus());
        assertTrue(milestoneReleaseResultRest.isSuccessful());
        assertNull(milestoneReleaseResultRest.getErrorMessage());
    }

    @Test
    public void testImportProductReleaseWithPncReleaseNotFound() throws Exception {
        Integer milestoneId = createRandomInt();

        doThrow(new RuntimeException("Test Exception")).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));

        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);

        // Run import
        importController.importMilestone(milestoneId, new CallbackTarget("http://dummy.org", CallbackMethod.PUT), "callbackId");

        verify(bpmClient).failure(anyString(), anyString(), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(ReleaseStatus.SET_UP_ERROR, milestoneReleaseResultRest.getReleaseStatus());
        assertFalse(milestoneReleaseResultRest.isSuccessful());

        // TODO verify the exception log message somehow, by verifying the Logger.log method was called?
    }

    @Test
    public void testImportProductReleaseWithNullBuildConfigurations() throws Exception {
        Integer milestoneId = createRandomInt();

        // Test setup
        Collection<BuildRecordRest> buildRecords = null;

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));

        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);

        // Run import
        importController.importMilestone(milestoneId, new CallbackTarget("http://dummy.org", CallbackMethod.PUT), "callbackId");

        verify(bpmClient).failure(anyString(), anyString(), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(ReleaseStatus.SET_UP_ERROR, milestoneReleaseResultRest.getReleaseStatus());
        assertFalse(milestoneReleaseResultRest.isSuccessful());

        // TODO verify the exception log message somehow
    }

    @Test
    public void testImportProductReleaseWithEmptyBuildConfigurations() throws Exception {
        Integer milestoneId = createRandomInt();

        // Test setup
        Collection<BuildRecordRest> buildRecords = new HashSet<>();

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));

        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);

        // Run import
        importController.importMilestone(milestoneId, new CallbackTarget("http://dummy.org", CallbackMethod.PUT), "callbackId");

        verify(bpmClient).failure(anyString(), anyString(), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(ReleaseStatus.SET_UP_ERROR, milestoneReleaseResultRest.getReleaseStatus());
        assertFalse(milestoneReleaseResultRest.isSuccessful());

        // TODO verify the exception log message somehow
    }

    @Test
    public void testImportProductReleaseWhereImportBuildThrowsException() throws Exception {
        Integer milestoneId = createRandomInt();
        Integer id = createRandomInt();
        String message = "Import build failed";

        // Test setup
        Collection<BuildRecordRest> buildRecords = new HashSet<>();
        BuildRecordRest buildRecordRest = new BuildRecordRest();
        buildRecordRest.setId(id);
        buildRecordRest.setExecutionRootName("Test Execution Name");
        buildRecordRest.setExecutionRootVersion("Test-execution-root-version");
        buildRecords.add(buildRecordRest);

        BrewNVR brewNVR = new BrewNVR(buildRecordRest.getExecutionRootName(), buildRecordRest.getExecutionRootVersion().replace( '-', '_' ), "1");

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));
        doThrow(new CausewayException(message)).when(brewClient).findBrewBuildOfNVR(eq(brewNVR));

        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);

        // Run import
        importController.importMilestone(milestoneId, new CallbackTarget("http://dummy.org", CallbackMethod.PUT), "callbackId");

        verify(bpmClient).failure(anyString(), anyString(), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(ReleaseStatus.SET_UP_ERROR, milestoneReleaseResultRest.getReleaseStatus());
        assertNotNull(milestoneReleaseResultRest.getBuilds());
        assertEquals(1, milestoneReleaseResultRest.getBuilds().size());
        BuildImportResultRest buildImportResultRest = milestoneReleaseResultRest.getBuilds().get(0);
        assertEquals(id, buildImportResultRest.getBuildRecordId());
        assertEquals(BuildImportStatus.ERROR, buildImportResultRest.getStatus());
        assertEquals(message, buildImportResultRest.getErrorMessage());
    }

    @Test
    public void testImportProductReleaseWithArtifactImportErrors() throws Exception {
        Integer milestoneId = createRandomInt();
        Integer id = createRandomInt();
        String message = "Artifact import error";

        // Test setup

        // Mock BuildEnvironment
        Map<String, String> attrs = new HashMap();
        attrs.put("OS", "Fedora25");
        attrs.put("JDK", "1.8");
        BuildEnvironment bEnv = new BuildEnvironment();
        bEnv.setAttributes(attrs);
        bEnv.setSystemImageType(SystemImageType.DOCKER_IMAGE);

        // Mock BuildConfigurationAudited
        BuildConfigurationAudited bcar = new BuildConfigurationAudited();
        bcar.setIdRev(new IdRev(id, 1));
        bcar.setScmRepoURL("http://repo.url");
        bcar.setScmRevision("r21345");
        bcar.setBuildEnvironment(bEnv);

        // Mock BuildRecord
        BuildRecord br = new BuildRecord();
        br.setId(id);
        Date start = new Date();
        br.setStartTime(start);
        br.setEndTime(new Date(start.getTime() + 100000L));
        br.setExecutionRootName("test:artifact");
        br.setExecutionRootVersion("1.1.1");
        br.setBuildConfigurationAudited(bcar);

        Collection<BuildRecordRest> buildRecords = new HashSet<>();
        BuildRecordRest buildRecordRest = new BuildRecordRest(br);
        buildRecords.add(buildRecordRest);

        BrewNVR brewNVR = new BrewNVR(buildRecordRest.getExecutionRootName(), buildRecordRest.getExecutionRootVersion().replace( '-', '_' ), "1");

        // In this test we don't mock the BrewBuild, so need to mock BuildArtifacts instead
        BuildArtifacts buildArtifacts = new BuildArtifacts();
        buildArtifacts.buildArtifacts.add(
                new BuildArtifacts.PncArtifact(
                        2371,
                        "maven",
                        "org.apache.geronimo.specs:geronimo-annotation_1.0_spec:1.1.1.redhat-1:pom",
                        "geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom",
                        "bedf8af1b107b36c72f52009e6fcc768",
                        "http://pnc-indy-branch-nightly.cloud.pnc.devel.engineering.redhat.com/api/hosted/"
                                + "build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/org/apache/geronimo/specs/geronimo-annotation_1.0_spec/1.1.1.redhat-1/geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom",
                        13245
                )
        );

        List<ArtifactImportError> artifactImportErrors = new ArrayList<>();
        ArtifactImportError importError = new ArtifactImportError();
        importError.setArtifactId(123);
        importError.setErrorMessage(message);
        artifactImportErrors.add(importError);

        doReturn(buildRecords).when(pncClient).findBuildsOfProductMilestone(eq(milestoneId));
        doReturn(buildArtifacts).when(pncClient).findBuildArtifacts(eq(id));
        doNothing().when(brewClient).tagBuild("brewTag", brewNVR);

        KojiImport kojiImport = translator.translate(brewNVR, buildRecordRest, buildArtifacts);
        ImportFileGenerator importFiles = translator.getImportFiles(buildArtifacts);

        BuildImportResultRest buildImportResultRest = new BuildImportResultRest(id, 11, "https://koji.myco.com/brew/buildinfo?buildID=11", BuildImportStatus.FAILED, null, artifactImportErrors);
        doReturn(buildImportResultRest).when(brewClient).importBuild(eq(brewNVR), eq(id), eq(kojiImport), eq(importFiles));

        ArgumentCaptor<MilestoneReleaseResultRest> resultArgument = ArgumentCaptor.forClass(MilestoneReleaseResultRest.class);
        // Run import
        importController.importMilestone(milestoneId, new CallbackTarget("http://dummy.org", CallbackMethod.PUT), "callbackId");

        verify(bpmClient).failure(anyString(), anyString(), resultArgument.capture());
        MilestoneReleaseResultRest milestoneReleaseResultRest = resultArgument.getValue();
        assertEquals(ReleaseStatus.IMPORT_ERROR, milestoneReleaseResultRest.getReleaseStatus());

        // TODO verify the exception log message somehow
    }

    public static Integer createRandomInt() {
        return new Random().nextInt();
    }
    public static Long createRandomLong() {
        return new Random().nextLong();
    }
    public static String createRandomString() {
        return "" + createRandomLong();
    }
}