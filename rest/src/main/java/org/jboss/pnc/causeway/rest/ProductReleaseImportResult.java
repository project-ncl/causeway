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
package org.jboss.pnc.causeway.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProductReleaseImportResult
{
    private final Map<Long, String> importErrors;

    private final Set<ImportedBuild> importedBuilds;

    public ProductReleaseImportResult( Map<Long, String> importErrors, Set<ImportedBuild> importedBuilds )
    {
        this.importErrors = importErrors;
        this.importedBuilds = importedBuilds;
    }

    public ProductReleaseImportResult()
    {
        this(new HashMap<>(), new HashSet<>());
    }

    public Map<Long, String> getImportErrors()
    {
        return importErrors;
    }

    public Set<ImportedBuild> getImportedBuilds()
    {
        return importedBuilds;
    }

    public void addResult(long buildId, BuildImportResult importResult) {
        if (importResult.brewBuild != null) {
            importedBuilds.add(new ImportedBuild(buildId, importResult.brewBuild));
        }
        if (importResult.error != null) {
            importErrors.put(buildId, importResult.error);
        }
    }
}
