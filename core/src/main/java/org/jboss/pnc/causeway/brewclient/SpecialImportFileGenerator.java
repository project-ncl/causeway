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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import lombok.Getter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.pnc.causeway.source.RenamedSources;

import lombok.Data;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class SpecialImportFileGenerator extends ImportFileGenerator {
    @Getter
    private final Set<Log> logs = new HashSet<>();

    public SpecialImportFileGenerator(RenamedSources sources) {
        super(sources);
    }

    /**
     * Add log url to the generator.
     *
     * @param url Url of the log.
     * @param filePath Deploy path for the log.
     */
    public void addLog(String url, String filePath) throws IOException, InterruptedException {
        logs.add(new Log(filePath, doGetRequest(url)));
    }

    private byte[] doGetRequest(String url) throws IOException {
        // Apache http client should automatically redirect for GETs and also automatically retry (4 times)
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            entity.writeTo(baos);
            return baos.toByteArray();
        }
    }

    @Override
    public Iterator<Supplier<ImportFile>> iterator() {
        return new ExternalLogImportFileIterator(artifacts.iterator(), logs.iterator());
    }

    @Data
    public static class Log {
        private final String filePath;
        private final byte[] log;
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
                return () -> new ImportFile(next1.filePath, new ByteArrayInputStream(next1.log), next1.log.length);
            }
            return super.next();
        }
    }

}
