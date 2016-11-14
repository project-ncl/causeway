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
package org.jboss.pnc.causeway.brewclient;

import java.io.ByteArrayInputStream;

import com.redhat.red.build.koji.model.ImportFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
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
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
public class ImportFileGenerator implements Iterable<Supplier<ImportFile>>{
    private final Set<URL> urls = new HashSet<>();
    private final Map<String, Integer> paths = new HashMap<>();
    private final Map<Integer, String> errors = new HashMap<>();
    private final String log;

    public ImportFileGenerator(String log) {
        this.log = log;
    }

    public void addUrl(Integer id, String url) throws MalformedURLException {
        URL artifactUrl = new URL(url);
        urls.add(artifactUrl);
        paths.put(getPath(artifactUrl), id);
    }

    public Map<Integer, String> getErrors() {
        return errors;
    }

    @Override
    public Iterator<Supplier<ImportFile>> iterator() {
        return new ImportFileIterator(urls.iterator());
    }

    public Integer getId(String path) {
        return paths.get(path);
    }

    private static String getPath(URL url) {
        return Paths.get(url.getPath()).getFileName().toString();
    }

    private class ImportFileIterator implements Iterator<Supplier<ImportFile>>{
        private Iterator<URL> it;
        private ImportFileSupplier next;
        private boolean logGiven = false;

        public ImportFileIterator(Iterator<URL> it) {
            this.it = it;
        }

        private ImportFileSupplier getNext() {
            URL url = it.next();
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                if (connection.getResponseCode() != 200) {
                    fail(url, "Failed to obtain artifact (status " + connection.getResponseCode() + " " + connection.getResponseMessage() + ")");
                    return null;
                }
                String contentLength = connection.getHeaderField("Content-Length");
                if (contentLength == null) {
                    fail(url, "Failed to obtain file size of artifact");
                    return null;
                }
                long size = Long.parseLong(contentLength);
                connection.disconnect();

                Logger.getLogger(ImportFileGenerator.class.getName()).log(Level.FINE, "Next is '" + getPath(url) + "' from '" + url + "'");
                return new ImportFileSupplier(getPath(url), url, size);
            } catch (IOException | NumberFormatException ex) {
                fail(url, "Failed to obtain file size of artifact", ex);
                return null;
            }
        }

        private void fail(URL url, String message) {
            Logger.getLogger(ImportFileGenerator.class.getName()).log(Level.WARNING, message + " '" + url + "'");
            Integer id = paths.get(getPath(url));
            errors.put(id, message + " '" + url + "'.");
        }

        private void fail(URL url, String message, Exception ex) {
            Logger.getLogger(ImportFileGenerator.class.getName()).log(Level.WARNING, message + " '" + url + "'", ex);
            Integer id = paths.get(getPath(url));
            errors.put(id, message + " '" + url + "': " + ex.getMessage());
        }

        @Override
        public boolean hasNext() {
            if (!logGiven) {
                return true;
            }
            while(next == null && it.hasNext()){
                next = getNext();
            }

            return next != null;
        }

        @Override
        public Supplier<ImportFile> next() {
            if (!logGiven) {
                logGiven = true;
                byte[] bytes = log.getBytes();
                return () -> new ImportFile("build.log", new ByteArrayInputStream(bytes), bytes.length);
            }
            while(next == null){ // will throw NoSuchElementException if there is no next
                next = getNext();
            }

            ImportFileSupplier ret = next;
            next = null;
            return ret;
        }
    }

    private static class ImportFileSupplier implements Supplier<ImportFile>{
        private final URL url;
        private final String filePath;
        private final long size;

        public ImportFileSupplier(String filePath, URL url, long size) {
            this.url = url;
            this.filePath = filePath;
            this.size = size;
        }

        @Override
        public ImportFile get() {
            try {
                InputStream stream = url.openStream();
                return new ImportFile(filePath, stream, size);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
