/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.pnc.causeway.ErrorMessages;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;
import org.jboss.pnc.common.log.MDCUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.red.build.koji.model.ImportFile;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@Slf4j
public class ImportFileGenerator implements Iterable<Supplier<ImportFile>> {
    private static final Logger userLog = LoggerFactory.getLogger("org.jboss.pnc._userlog_.brew-push");
    protected final Set<Artifact> artifacts = new HashSet<>();
    protected final List<BurnAfterReadingFile> files = new ArrayList<>();
    protected final Map<String, String> paths = new HashMap<>();

    public ImportFileGenerator(BurnAfterReadingFile... files) {
        this.files.addAll(Arrays.stream(files).filter(Objects::nonNull).toList());
    }

    /**
     * Adds artifact URL to the generator.
     *
     * @param id External ID of the artifact.
     * @param url URL of the artifact.
     * @param filePath Deploy path for the artifact.
     * @param size Size of the artifact in bytes.
     */
    public void addUrl(String id, String url, String filePath, long size) throws MalformedURLException {
        URL artifactUrl = new URL(url);
        artifacts.add(new Artifact(id, artifactUrl, filePath, size));
        paths.put(filePath, id);
    }

    /**
     * Returns external ID of artifact given it's deploy path.
     *
     * @param path Deploy path of the artifact.
     * @return External ID of the artifact or null if aritfact not present.
     */
    public String getId(String path) {
        return paths.get(path);
    }

    @Override
    public Iterator<Supplier<ImportFile>> iterator() {
        return new ImportFileIterator();
    }

    private record Artifact(String id, URL url, String filePath, long size) {
    }

    private class ImportFileIterator implements Iterator<Supplier<ImportFile>> {

        private final Iterator<Artifact> it;
        private int nextFile = 0;

        protected ImportFileIterator() {
            this.it = artifacts.iterator();
        }

        private ImportFileSupplier getNext() {
            Artifact artifact = it.next();
            userLog.info("Reading file {} from {}", artifact.filePath(), artifact.url());
            return new ImportFileSupplier(artifact);
        }

        @Override
        public boolean hasNext() {
            if (nextFile < files.size()) {
                return true;
            }
            return it.hasNext();
        }

        @Override
        public Supplier<ImportFile> next() {
            if (nextFile < files.size()) {
                BurnAfterReadingFile file = files.get(nextFile++);
                return () -> {
                    try {
                        return new ImportFile(file.getName(), file.read(), file.getSize());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                };
            }
            return getNext();
        }
    }

    private static class ImportFileSupplier implements Supplier<ImportFile> {

        private final Artifact artifact;

        public ImportFileSupplier(Artifact artifact) {
            this.artifact = artifact;
        }

        @Override
        public ImportFile get() {
            try {
                HttpURLConnection connection = (HttpURLConnection) artifact.url().openConnection();
                try {
                    MDCUtils.getHeadersFromMDC().forEach(connection::addRequestProperty);
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        String responseMessage = connection.getResponseMessage();
                        connection.disconnect();
                        throw new RuntimeException(ErrorMessages.failedToObtainArtifact(responseCode, responseMessage));
                    }
                    InputStream stream = connection.getInputStream();
                    return new ImportFile(artifact.filePath(), stream, artifact.size());
                } catch (RuntimeException ex) {
                    connection.disconnect();
                    throw ex;
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
