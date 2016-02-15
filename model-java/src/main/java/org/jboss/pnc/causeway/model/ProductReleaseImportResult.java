package org.jboss.pnc.causeway.model;

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
