/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.redhat.red.build.koji.model.ImportFile;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkiverse.wiremock.devservice.WireMockConfigKey;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.causeway.source.RenamedSources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@QuarkusTest
@ConnectWireMock
public class ImportFileGeneratorTest {
    WireMock wiremock;

    @ConfigProperty(name = WireMockConfigKey.PORT)
    Integer port;

    private static final String HOST = "http://localhost:";

    private static final String FIRST_PATH = "path/to/artifact.jar";
    private static final String FIRST_LOCATION = "/api/" + FIRST_PATH;
    private static final String SECOND_PATH = "path/to/second/artifact.jar";
    private static final String SECOND_LOCATION = "/api/" + SECOND_PATH;
    private static final String NONEXISTING_PATH = "path/to/non-existing/artifact.jar";
    private static final String NONEXISTING_LOCATION = "/api/" + NONEXISTING_PATH;
    private static final String LOG_PATH = "build.log";
    private static final String SOURCES_PATH = "sources.tar.gz";
    private static final String FIRST_ARTIFACT = "First artifact";
    private static final String SECOND_ARTIFACT = "This is second artifact";
    private static final String BUILD_LOG = "foobar";
    private static final String SOURCES = "Burn this after reading!";

    private String host() {
        return HOST + port;
    }

    @BeforeEach
    public void stubArtifacts() {
        wiremock.register(
                get(urlEqualTo(FIRST_LOCATION)).willReturn(
                        aResponse().withStatus(200)
                                .withHeader("Content-Type", "text/plain")
                                .withHeader("Content-Length", "14")
                                .withBody(FIRST_ARTIFACT)));
        wiremock.register(
                head(urlEqualTo(FIRST_LOCATION))
                        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain")));
        wiremock.register(
                get(urlEqualTo(SECOND_LOCATION)).willReturn(
                        aResponse().withStatus(200)
                                .withHeader("Content-Type", "text/plain")
                                .withHeader("Content-Length", "23")
                                .withBody(SECOND_ARTIFACT)));
        wiremock.register(
                head(urlEqualTo(SECOND_LOCATION))
                        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain")));
    }

    private RenamedSources prepareBurningFile(String text, String path) throws IOException {
        Path tempFile = Files.createTempFile("burn", "me");
        Files.write(tempFile, Collections.singleton(text));
        return new RenamedSources(tempFile, path, "01234", null);
    }

    @Test
    public void testLogImportFileGenerator() throws IOException {
        final ImportFileGenerator ifg = new ImportFileGenerator(
                prepareBurningFile(BUILD_LOG, LOG_PATH),
                prepareBurningFile(SOURCES, SOURCES_PATH));
        doTestImportFileGenerator(ifg);
    }

    private void doTestImportFileGenerator(ImportFileGenerator ifg) throws IOException {
        ifg.addUrl("1", host() + FIRST_LOCATION, FIRST_PATH, FIRST_ARTIFACT.length());
        ifg.addUrl("2", host() + SECOND_LOCATION, SECOND_PATH, SECOND_ARTIFACT.length());

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
                    assertEquals(7, file.getSize());
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
    public void testImportFileGeneratorFail() throws IOException {
        final ImportFileGenerator ifg = new ImportFileGenerator(
                prepareBurningFile(BUILD_LOG, SOURCES_PATH),
                prepareBurningFile(SOURCES, SOURCES_PATH));
        doTestImportFileGeneratorFail(ifg);
    }

    private void doTestImportFileGeneratorFail(ImportFileGenerator ifg) throws IOException {
        ifg.addUrl("1", host() + FIRST_LOCATION, FIRST_PATH, FIRST_ARTIFACT.length());
        ifg.addUrl("2", host() + NONEXISTING_LOCATION, NONEXISTING_PATH, 42);

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
