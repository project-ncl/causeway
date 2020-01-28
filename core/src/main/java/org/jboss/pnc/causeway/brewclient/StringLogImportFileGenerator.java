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

import java.io.ByteArrayInputStream;

import com.redhat.red.build.koji.model.ImportFile;

import java.util.Iterator;
import java.util.function.Supplier;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@Deprecated
public class StringLogImportFileGenerator extends ImportFileGenerator {
    private final String log;

    public StringLogImportFileGenerator(String log) {
        this.log = log;
    }

    @Override
    public Iterator<Supplier<ImportFile>> iterator() {
        return new ImportFileIterator2(artifacts.iterator());
    }

    private class ImportFileIterator2 extends ImportFileIterator {
        private boolean logGiven = false;

        public ImportFileIterator2(Iterator<Artifact> it) {
            super(it);
        }

        @Override
        public boolean hasNext() {
            if (!logGiven) {
                return true;
            }
            return super.hasNext();
        }

        @Override
        public Supplier<ImportFile> next() {
            if (log != null && !logGiven) {
                logGiven = true;
                byte[] bytes = log.getBytes();
                return () -> new ImportFile("build.log", new ByteArrayInputStream(bytes), bytes.length);
            }
            return super.next();
        }
    }

}
