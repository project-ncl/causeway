/**
 * Copyright (C) 2015 Red Hat, Inc. (jbrazdil@redhat.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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

import lombok.extern.slf4j.Slf4j;

import org.jboss.pnc.causeway.source.RenamedSources;
import org.jboss.pnc.causeway.util.MDCUtils;

/**
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@Slf4j
public abstract class ImportFileGenerator implements Iterable<Supplier<ImportFile>> {
    protected final Set<Artifact> artifacts = new HashSet<>();
    protected final RenamedSources sources;
    protected final Map<String, String> paths = new HashMap<>();

    public ImportFileGenerator(RenamedSources sources) {
        this.sources = sources;
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

    @Data
    protected static class Artifact {
        private final String id;
        private final URL url;
        private final String filePath;
        private final long size;
    }

    protected abstract class ImportFileIterator implements Iterator<Supplier<ImportFile>> {

        private Iterator<Artifact> it;
        private boolean sourcesGiven = false;

        protected ImportFileIterator(Iterator<Artifact> it) {
            this.it = it;
        }

        private ImportFileSupplier getNext() {
            Artifact artifact = it.next();
            log.info("Reading file {} from {}", artifact.getFilePath(), artifact.getUrl());
            return new ImportFileSupplier(artifact);
        }

        @Override
        public boolean hasNext() {
            if (!sourcesGiven) {
                return true;
            }
            return it.hasNext();
        }

        @Override
        public Supplier<ImportFile> next() {
            if (!sourcesGiven) {
                sourcesGiven = true;
                return () -> {
                    try {
                        return new ImportFile(sources.getName(), sources.read(), sources.getSize());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                };
            }
            return getNext();
        }
    }

    protected static class ImportFileSupplier implements Supplier<ImportFile> {

        private final Artifact artifact;

        public ImportFileSupplier(Artifact artifact) {
            this.artifact = artifact;
        }

        @Override
        public ImportFile get() {
            try {
                HttpURLConnection connection = (HttpURLConnection) artifact.getUrl().openConnection();
                try {
                    MDCUtils.headersFromContext().forEach(connection::addRequestProperty);
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode != 200) {
                        String responseMessage = connection.getResponseMessage();
                        connection.disconnect();
                        throw new RuntimeException(
                                "Failed to obtain artifact (status " + responseCode + " " + responseMessage + ")");
                    }
                    InputStream stream = connection.getInputStream();
                    return new ImportFile(artifact.getFilePath(), stream, artifact.getSize());
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
