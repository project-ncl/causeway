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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.red.build.koji.model.ImportFile;

/**
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
public class ImportFileGenerator implements Iterable<ImportFile>{
    private final Set<URL> urls = new HashSet<>();
    private final Map<String, Integer> paths = new HashMap<>();
    private final Map<Integer, String> errors = new HashMap<>();

    public void addUrl(Integer id, String url) throws MalformedURLException {
        URL artifactUrl = new URL(url);
        urls.add(artifactUrl);
        String[] parts = artifactUrl.getPath().split("/", 5);
        String path = parts[4]; // /api/TYPE/NAME/p/a/t/h => p/a/t/h
        paths.put(path, id);
    }

    public Map<Integer, String> getErrors() {
        return errors;
    }

    @Override
    public Iterator<ImportFile> iterator() {
        return new ImportFileIterator(urls.iterator());
    }

    public Integer getId(String path) {
        return paths.get(path);
    }

    private class ImportFileIterator implements Iterator<ImportFile>{
        private Iterator<URL> it;
        private ImportFile next;

        public ImportFileIterator(Iterator<URL> it) {
            this.it = it;
        }

        private ImportFile getNext() {
            URL url = it.next();
            String[] parts = url.getPath().split("/", 5);
            String path = parts[4]; // /api/TYPE/NAME/p/a/t/h => p/a/t/h
            try {
                InputStream stream = url.openStream();
                System.out.println("Next is " + url);
                return new ImportFile(path, stream);
            } catch (IOException ex) {
                Logger.getLogger(ImportFileGenerator.class.getName()).log(Level.WARNING, null, ex);
                Integer id = paths.get(path);
                errors.put(id, "Failed to obtain artifact: " + ex.getMessage());
                return null;
            }
        }

        @Override
        public boolean hasNext() {
            while(next == null && it.hasNext()){
                next = getNext();
            }

            return next == null;
        }

        @Override
        public ImportFile next() {
            System.out.println("Getting next");
            while(next == null){ // will throw NoSuchElementException if there is no next
                next = getNext();
            }
            return next;
        }
    }


}
