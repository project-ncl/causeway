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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.ImportFile;

/**
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
public class ImportFileGenerator implements Iterable<ImportFile>{
    private final Collection<String> urls;

    public ImportFileGenerator(Collection<String> urls) {
        this.urls = urls;
    }

    @Override
    public Iterator<ImportFile> iterator() {
        return new ImportFileIterator(urls.iterator());
    }

    Integer getId(KojiClientException value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static class ImportFileIterator implements Iterator<ImportFile>{
        private Iterator<String> it;

        public ImportFileIterator(Iterator<String> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public ImportFile next() {
            String next = it.next();
            try {
                URL url = new URL(next);
                String[] parts = url.getPath().split("/", 5);
                String path = parts[4]; // /api/TYPE/NAME/p/a/t/h => p/a/t/h

                InputStream stream = url.openStream();
                return new ImportFile(path, stream);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to get input stream for url '" + next+ "'", ex);
            }
        }
    }


}
