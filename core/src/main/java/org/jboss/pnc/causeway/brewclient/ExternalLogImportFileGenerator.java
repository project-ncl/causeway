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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import lombok.Data;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class ExternalLogImportFileGenerator extends ImportFileGenerator {
    private final Set<Log> logs = new HashSet<>();

    /**
     * Add log url to the generator.
     * 
     * @param url Url of the log.
     * @param filePath Deploy path for the log.
     * @param size Size of the log file.
     */
    public void addLog(String url, String filePath, long size) throws MalformedURLException {
        URL artifactUrl = new URL(url);
        logs.add(new Log(artifactUrl, filePath, size));
    }

    @Override
    public Iterator<Supplier<ImportFile>> iterator() {
        return new ExternalLogImportFileIterator(artifacts.iterator(), logs.iterator());
    }

    @Data
    private static class Log {
        private final URL url;
        private final String filePath;
        private final long size;
    }

    private class ExternalLogImportFileIterator extends ImportFileIterator {
        private final Iterator<Log> logIt;

        public ExternalLogImportFileIterator(Iterator<Artifact> it, Iterator<Log> logIt) {
            super(it);
            this.logIt = logIt;
        }

        @Override
        public boolean hasNext() {
            if (logIt.hasNext()) {
                return true;
            }
            return super.hasNext();
        }

        @Override
        public Supplier<ImportFile> next() {
            if (logIt.hasNext()) {
                Log next1 = logIt.next();
                return () -> {
                    try {
                        return new ImportFile(next1.filePath, next1.getUrl().openStream(), next1.size);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                };
            }
            return super.next();
        }
    }

}
