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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.util.KojiObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.CausewayFailure;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.brewclient.BuildTranslatorImpl;
import org.jboss.pnc.causeway.brewclient.ExternalLogImportFileGenerator;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.pncmetrics.MetricsConfiguration;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.CallbackMethod;
import org.jboss.pnc.causeway.rest.CallbackTarget;
import org.jboss.pnc.causeway.rest.model.Build;
import org.jboss.pnc.causeway.rest.model.MavenBuild;
import org.jboss.pnc.causeway.rest.model.MavenBuiltArtifact;
import org.jboss.pnc.causeway.rest.model.NpmBuild;
import org.jboss.pnc.causeway.rest.model.NpmBuiltArtifact;
import org.jboss.pnc.dto.ArtifactImportError;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.jboss.pnc.causeway.ctl.PncImportControllerImpl.messageMissingTag;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;

public class ImportControllerTest {

    @Rule
    public WireMockRule wireMockRule = (new WireMockRule(8081));

    private static final String USERNAME = "joe";
    private static final String TAG_PREFIX = "pnc-foo-0.1";
    private static final String BUILD_VERSION = "1.1.1";
    private static final String ARTIFACTS_VERSION = "1.1.1.redhat_1";
    private static final String BUILD_NAME = "org.apache.geronimo.specs:geronimo-annotation_1.0_spec";
    private static final String CALLBACK_URL = "http://localhost:8081/callback";
    private static final CallbackTarget CALLBACK_TARGET = new CallbackTarget(CALLBACK_URL, CallbackMethod.POST);
    private static final String KOJI_URL = "http://koji.example.com/koji";
    private static final String KOJI_BUILD_URL = KOJI_URL + "/build?id=";

    private static final String BUILD_NPM_NAME = "npm_async_3.1.0-npm";
    private static final String ARTIFACT_NPM_VERSION = "3.1.0.redhat_1";

    private static final BrewNVR NVR = new BrewNVR(BUILD_NAME, BUILD_VERSION, "1");

    private static final ExternalLogImportFileGenerator IMPORT_FILE_GENERATOR = mock(ExternalLogImportFileGenerator.class);
    private static final KojiImport KOJI_IMPORT = mock(KojiImport.class);

    private static final ObjectMapper mapper = new KojiObjectMapper();

    @Mock
    private BrewClient brewClient;
    @Mock
    private CausewayConfig causewayConfig;

    @Mock
    public BuildTranslatorImpl translator;

    @Mock
    public MetricsConfiguration metricsConfiguration;

    @Mock
    public MetricRegistry metricRegistry;
    
    @InjectMocks
    private ImportControllerImpl importController;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(causewayConfig.getKojiURL()).thenReturn(KOJI_URL);
        when(causewayConfig.getKojiWebURL()).thenReturn(KOJI_BUILD_URL);
        when(metricsConfiguration.getMetricRegistry()).thenReturn(metricRegistry);
        when(metricRegistry.meter(anyString())).thenReturn(mock(Meter.class));
        Timer timer = mock(Timer.class);
        when(metricRegistry.timer(anyString())).thenReturn(timer);
        when(timer.time()).thenReturn(mock(Timer.Context.class));

        Histogram histogram = mock(Histogram.class);
        when(metricRegistry.register(anyString(), any(Histogram.class))).thenReturn(histogram);
        when(metricRegistry.histogram(anyString())).thenReturn(histogram);

        mapper.registerSubtypes(MavenBuild.class, NpmBuild.class, MavenBuiltArtifact.class, NpmBuiltArtifact.class);

        stubFor(post(urlEqualTo("/callback"))
                .willReturn(aResponse()
                        .withStatus(200)));
    }

    private Build getMavenBuild() throws IOException {
        return getBuild("build.json");
    }

    private Build getNpmBuild() throws IOException {
        return getBuild("npmbuild.json");
    }

    private Build getBuild(String buildFileName) throws IOException {
        String json = readResponseBodyFromTemplate(buildFileName);
        return mapper.readValue(json, Build.class);
    }

    private void mockBrew() throws CausewayException {
        doReturn(true).when(brewClient).tagsExists(eq(TAG_PREFIX));
        doReturn(KOJI_BUILD_URL + "11").when(brewClient).getBuildUrl(11);
        doNothing().when(brewClient).tagBuild(TAG_PREFIX, NVR);
    }

    private void mockTranslator() throws CausewayException {
        doReturn(KOJI_IMPORT).when(translator).translate(eq(NVR), any(), any());
        doReturn(IMPORT_FILE_GENERATOR).when(translator).getImportFiles(any());
    }

    @Test
    public void testImportBuildWhenExistingBrewBuildIsImported() throws Exception {
        // Test setup
        mockBrew();
        
        // Mock existing Brew build
        doReturn(new BrewBuild(11, NVR)).when(brewClient).findBrewBuildOfNVR(eq(NVR));

        // Run import
        importController.importBuild(getMavenBuild(), CALLBACK_TARGET, USERNAME);

        // Verify
        verifySuccess("Build was already imported with id 11");
    }

    @Test
    public void testImportBuild() throws Exception {
        // Test setup
        mockBrew();
        mockTranslator();

        // Mock Brew import
        BrewBuild brewBuild = new BrewBuild(11, NVR);
        doReturn(brewBuild).when(brewClient).importBuild(eq(NVR), same(KOJI_IMPORT), same(IMPORT_FILE_GENERATOR));

        // Run import
        importController.importBuild(getMavenBuild(), CALLBACK_TARGET, USERNAME);

        // Verify
        verifySuccess("Build imported with id 11");
    }

    @Test
    public void testImportBuildWithEmptyArtifacts() throws Exception {
        // Test setup
        mockBrew();

        // Mock no builds in milestone
        Build build = getMavenBuild();
        build.getBuiltArtifacts().clear();

        // Run import
        importController.importBuild(build, CALLBACK_TARGET, USERNAME);

        // Verify
        verifyFailure("Build doesn't contain any artifacts");
    }

    @Test
    public void testImportBuildWhereImportBuildThrowsException() throws Exception {
        String exceptionMessage = "Import build failed";

        // Test setup
        mockBrew();

        // Mock exception from Brew Client
        doThrow(new CausewayException(exceptionMessage)).when(brewClient).findBrewBuildOfNVR(eq(NVR));

        // Run import
        importController.importBuild(getMavenBuild(), CALLBACK_TARGET, USERNAME);

        // Verify
        verifyError(exceptionMessage);
    }

    @Test
    public void testImportBuildWithArtifactImportErrors() throws Exception {
        String errorMessage = "Artifact import error";
        final String exceptionMessage = "Failure while importing artifacts";

        // Test setup
        mockBrew();
        mockTranslator();

        List<ArtifactImportError> artifactImportErrors = new ArrayList<>();
        ArtifactImportError importError = ArtifactImportError.builder()
                .artifactId(String.valueOf(123))
                .errorMessage(errorMessage)
                .build();
        artifactImportErrors.add(importError);
        doThrow(new CausewayFailure(artifactImportErrors, exceptionMessage)).when(brewClient).importBuild(eq(NVR), same(KOJI_IMPORT), same(IMPORT_FILE_GENERATOR));

        // Run import
        importController.importBuild(getMavenBuild(), CALLBACK_TARGET, USERNAME);

        // Verify
        verifyFailure(exceptionMessage, artifactImportErrors);
    }

    @Test
    public void testImportBuildWhenTagDoesNotExistInBrew() throws Exception {

        // Test setup
        mockBrew();
        doReturn(false).when(brewClient).tagsExists(TAG_PREFIX);

        // Run import
        importController.importBuild(getMavenBuild(), CALLBACK_TARGET, USERNAME);

        // Verify
        verifyFailure(messageMissingTag(TAG_PREFIX, KOJI_URL).replace("\n", "\\n"));
    }

    @Test
    public void testGetNVR() throws IOException, CausewayException, ReflectiveOperationException{
        Build build = getMavenBuild();
        BrewNVR nvr = importController.getNVR(build);
        assertEquals(BUILD_NAME, nvr.getName());
        assertEquals(BUILD_VERSION, nvr.getVersion());

        setFinalField(build, Build.class.getDeclaredField("buildVersion"), null);
        nvr = importController.getNVR(build);
        assertEquals(BUILD_NAME, nvr.getName());
        assertEquals(ARTIFACTS_VERSION, nvr.getVersion());
    }

    @Test
    public void testAutomaticVersionNpm() throws IOException, CausewayException, ReflectiveOperationException{
        Build build = getNpmBuild();
        setFinalField(build, Build.class.getDeclaredField("buildVersion"), null);
        BrewNVR nvr = importController.getNVR(build);
        assertEquals(BUILD_NPM_NAME, nvr.getName());
        assertEquals(ARTIFACT_NPM_VERSION, nvr.getVersion());
    }
    static void setFinalField(Object obj, Field field, Object newValue) throws ReflectiveOperationException {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(obj, newValue);
    }

    private void verifySuccess(String log) {

        String result = "{"
                + "\"id\":null,"
                + "\"buildId\":\"61\","
                + "\"status\":\"SUCCESS\","
                + "\"log\":\"" + log + "\","
                + "\"artifactImportErrors\":null,"
                + "\"brewBuildId\":11,"
                + "\"brewBuildUrl\":\"" + KOJI_BUILD_URL + "11\""
                + "}";

        WireMock.verify(postRequestedFor(urlEqualTo("/callback"))
                .withRequestBody(WireMock.equalToJson(result)));
    }

    private void verifyFailure(String log) {
        verifyFailure(log, Collections.emptyList());
    }

    private void verifyFailure(String log, List<ArtifactImportError> artifactImportErrors) {
        String artifacts = "null";
        if (!artifactImportErrors.isEmpty()) {
            artifacts = artifactImportErrors.stream()
                    .map(a -> "{"
                            + "\"artifactId\":\"" + a.getArtifactId() + "\","
                            + "\"errorMessage\":\"" + a.getErrorMessage() + "\""
                            + "}")
                    .collect(Collectors.joining(",", "[", "]"));
        }

        String result = "{"
                + "\"id\":null,"
                + "\"buildId\":\"61\","
                + "\"status\":\"FAILED\","
                + "\"log\":\"" + log + "\","
                + "\"artifactImportErrors\":" + artifacts + ","
                + "\"brewBuildId\":null,"
                + "\"brewBuildUrl\":null"
                + "}";

        WireMock.verify(postRequestedFor(urlEqualTo("/callback"))
                .withRequestBody(WireMock.equalToJson(result)));
    }

    private void verifyError(String log) {
        String result = "{"
                + "\"id\":null,"
                + "\"buildId\":\"61\","
                + "\"status\":\"SYSTEM_ERROR\","
                + "\"log\":\"" + log + "\","
                + "\"artifactImportErrors\":null,"
                + "\"brewBuildId\":null,"
                + "\"brewBuildUrl\":null"
                + "}";

        WireMock.verify(postRequestedFor(urlEqualTo("/callback"))
                .withRequestBody(WireMock.equalToJson(result)));
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
