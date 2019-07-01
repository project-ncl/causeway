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

import com.redhat.red.build.koji.model.ImportFile;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public abstract class ImportFileGenerator implements Iterable<Supplier<ImportFile>> {
    protected final Set<Artifact> artifacts = new HashSet<>();
    protected final Map<String, Integer> paths = new HashMap<>();
    protected final Map<Integer, String> errors = new HashMap<>();

    /**
     * Adds artifact URL to the generator.
     * @param id External ID of the artifact.
     * @param url URL of the artifact.
     * @param filePath Deploy path for the artifact.
     */
    public void addUrl(Integer id, String url, String filePath) throws MalformedURLException {
        URL artifactUrl = new URL(url);
        artifacts.add(new Artifact(id, artifactUrl, filePath));
        paths.put(filePath, id);
    }

    public Map<Integer, String> getErrors() {
        return errors;
    }

    /**
     * Returns external ID of artifact given it's deploy path.
     * @param path Deploy path of the artifact.
     * @return External ID of the artifact or null if aritfact not present.
     */
    public Integer getId(String path) {
        return paths.get(path);
    }

    @Data
    protected static class Artifact {
        private final int id;
        private final URL url;
        private final String filePath;
    }

    protected abstract class ImportFileIterator implements Iterator<Supplier<ImportFile>> {

        private Iterator<Artifact> it;
        private ImportFileSupplier next;

        protected ImportFileIterator(Iterator<Artifact> it) {
            this.it = it;
        }

        private ImportFileSupplier getNext() {
            Artifact artifact = it.next();
            try {
                HttpURLConnection connection = (HttpURLConnection) artifact.getUrl().openConnection();
                connection.setRequestMethod("HEAD");
                if (connection.getResponseCode() != 200) {
                    fail(artifact, "Failed to obtain artifact (status " + connection.getResponseCode() + " " + connection.getResponseMessage() + ")");
                    return null;
                }
                String contentLength = connection.getHeaderField("Content-Length");
                if (contentLength == null) {
                    fail(artifact, "Failed to obtain file size of artifact");
                    return null;
                }
                long size = Long.parseLong(contentLength);
                connection.disconnect();

                Logger.getLogger(ImportFileGenerator.class.getName()).log(Level.FINE, "Next is '" + artifact.getFilePath() + "' from '" + artifact + "'");
                return new ImportFileSupplier(artifact, size);
            } catch (IOException | NumberFormatException ex) {
                fail(artifact, "Failed to obtain file size of artifact", ex);
                return null;
            }
        }

        private void fail(Artifact artifact, String message) {
            Logger.getLogger(ImportFileGenerator.class.getName()).log(Level.WARNING, message + " '" + artifact.getUrl() + "'");
            Integer id = artifact.getId();
            errors.put(id, message + " '" + artifact.getUrl() + "'.");
        }

        private void fail(Artifact artifact, String message, Exception ex) {
            Logger.getLogger(ImportFileGenerator.class.getName()).log(Level.WARNING, message + " '" + artifact.getUrl() + "'", ex);
            Integer id = artifact.getId();
            errors.put(id, message + " '" + artifact.getUrl() + "': " + ex.getMessage());
        }

        @Override
        public boolean hasNext() {
            while (next == null && it.hasNext()) {
                next = getNext();
            }

            return next != null;
        }

        @Override
        public Supplier<ImportFile> next() {
            while (next == null) { // will throw NoSuchElementException if there is no next
                next = getNext();
            }

            ImportFileSupplier ret = next;
            next = null;
            return ret;
        }
    }

    protected static class ImportFileSupplier implements Supplier<ImportFile> {

        private final Artifact artifact;
        private final long size;

        public ImportFileSupplier(Artifact artifact, long size) {
            this.artifact = artifact;
            this.size = size;
        }

        @Override
        public ImportFile get() {
            try {
                InputStream stream = artifact.getUrl().openStream();
                return new ImportFile(artifact.getFilePath(), stream, size);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
