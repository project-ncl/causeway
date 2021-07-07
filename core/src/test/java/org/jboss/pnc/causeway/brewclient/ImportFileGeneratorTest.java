package org.jboss.pnc.causeway.brewclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jboss.pnc.causeway.source.RenamedSources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.redhat.red.build.koji.model.ImportFile;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class ImportFileGeneratorTest {

    @Rule
    public WireMockRule wireMockRule = (new WireMockRule(8081));
    private static final String HOST = "http://localhost:8081";

    private static final String FIRST_PATH = "path/to/artifact.jar";
    private static final String FIRST_LOCATION = "/api/" + FIRST_PATH;
    private static final String SECOND_PATH = "path/to/second/artifact.jar";
    private static final String SECOND_LOCATION = "/api/" + SECOND_PATH;
    private static final String NONEXISTING_PATH = "path/to/non-existing/artifact.jar";
    private static final String NONEXISTING_LOCATION = "/api/" + NONEXISTING_PATH;
    private static final String LOG_PATH = "build.log";
    private static final String LOG_LOCATION = "/other-api/" + LOG_PATH;
    private static final String SOURCES_PATH = "sources.tar.gz";
    private static final String FIRST_ARTIFACT = "First artifact";
    private static final String SECOND_ARTIFACT = "This is second artifact";
    private static final String BUILD_LOG = "foobar";
    private static final String SOURCES = "Burn this after reading!";

    @Before
    public void stubArtifacts() {
        stubFor(
                get(urlEqualTo(FIRST_LOCATION)).willReturn(
                        aResponse().withStatus(200)
                                .withHeader("Content-Type", "text/plain")
                                .withHeader("Content-Length", "14")
                                .withBody(FIRST_ARTIFACT)));
        stubFor(
                head(urlEqualTo(FIRST_LOCATION))
                        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain")));
        stubFor(
                get(urlEqualTo(SECOND_LOCATION)).willReturn(
                        aResponse().withStatus(200)
                                .withHeader("Content-Type", "text/plain")
                                .withHeader("Content-Length", "23")
                                .withBody(SECOND_ARTIFACT)));
        stubFor(
                head(urlEqualTo(SECOND_LOCATION))
                        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain")));
        stubFor(
                get(urlEqualTo(LOG_LOCATION)).willReturn(
                        aResponse().withStatus(200).withHeader("Content-Type", "text/plain").withBody(BUILD_LOG)));
    }

    private RenamedSources prepareSourcesFile() throws IOException {
        Path tempFile = Files.createTempFile("burn", "me");
        Files.write(tempFile, Collections.singleton(SOURCES));
        return new RenamedSources(tempFile, SOURCES_PATH, "01234");
    }

    @Test
    public void testStringLogImportFileGenerator() throws MalformedURLException, IOException {
        final StringLogImportFileGenerator ifg = new StringLogImportFileGenerator(BUILD_LOG, prepareSourcesFile());
        doTestImportFileGenerator(ifg);
    }

    @Test
    public void testExternalLongImportFileGenerator() throws MalformedURLException, IOException {
        final ExternalLogImportFileGenerator ifg = new ExternalLogImportFileGenerator(prepareSourcesFile());
        ifg.addLog(HOST + LOG_LOCATION, LOG_PATH, 6);
        doTestImportFileGenerator(ifg);
    }

    private void doTestImportFileGenerator(ImportFileGenerator ifg) throws MalformedURLException, IOException {
        ifg.addUrl("1", HOST + FIRST_LOCATION, FIRST_PATH, FIRST_ARTIFACT.length());
        ifg.addUrl("2", HOST + SECOND_LOCATION, SECOND_PATH, SECOND_ARTIFACT.length());

        assertEquals("1", ifg.getId(FIRST_PATH));
        assertEquals("2", ifg.getId(SECOND_PATH));
        assertNull(ifg.getId(NONEXISTING_PATH));

        int count = 0;
        for (Supplier<ImportFile> supp : ifg) {
            ImportFile file = supp.get();
            switch (file.getFilePath()) {
                case FIRST_PATH:
                    assertEquals(14, file.getSize());
                    assertEquals(FIRST_ARTIFACT, toString(file.getStream()));
                    break;
                case SECOND_PATH:
                    assertEquals(23, file.getSize());
                    assertEquals(SECOND_ARTIFACT, toString(file.getStream()));
                    break;
                case LOG_PATH:
                    assertEquals(6, file.getSize());
                    assertEquals(BUILD_LOG, toString(file.getStream()));
                    break;
                case SOURCES_PATH:
                    assertEquals(25, file.getSize());
                    assertEquals(SOURCES, toString(file.getStream()));
                    break;
                default:
                    fail("Unexpected file path: " + file.getFilePath());
            }
            count++;
        }
        assertEquals(4, count);
    }

    @Test
    public void testStringLogImportFileGeneratorFail() throws MalformedURLException, IOException {
        final StringLogImportFileGenerator ifg = new StringLogImportFileGenerator(BUILD_LOG, prepareSourcesFile());
        doTestImportFileGeneratorFail(ifg);
    }

    @Test
    public void testExternalLongImportFileGeneratorFail() throws MalformedURLException, IOException {
        final ExternalLogImportFileGenerator ifg = new ExternalLogImportFileGenerator(prepareSourcesFile());
        ifg.addLog(HOST + LOG_LOCATION, LOG_PATH, 6);
        doTestImportFileGeneratorFail(ifg);
    }

    private void doTestImportFileGeneratorFail(ImportFileGenerator ifg) throws MalformedURLException, IOException {
        ifg.addUrl("1", HOST + FIRST_LOCATION, FIRST_PATH, FIRST_ARTIFACT.length());
        ifg.addUrl("2", HOST + NONEXISTING_LOCATION, NONEXISTING_PATH, 42);

        try {
            for (Supplier<ImportFile> supp : ifg) {
                ImportFile file = supp.get();
                file.getStream().close();
            }
            fail("Should have thrown an exception");
        } catch (RuntimeException ex) {
            // ok
        }
    }

    public static String toString(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

}
