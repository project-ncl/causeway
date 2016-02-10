package org.jboss.pnc.causeway.model;

import java.util.Map;
import java.util.Set;

/**
 * Created by jdcasey on 2/9/16.
 */
public class ProductReleaseImportResult
{
    private Map<Integer, String> importErrors;

    private Set<ImportedBuild> importedBuilds;

    public ProductReleaseImportResult( Map<Integer, String> importErrors, Set<ImportedBuild> importedBuilds )
    {
        this.importErrors = importErrors;
        this.importedBuilds = importedBuilds;
    }

    public Map<Integer, String> getImportErrors()
    {
        return importErrors;
    }

    public void setImportErrors( Map<Integer, String> importErrors )
    {
        this.importErrors = importErrors;
    }

    public Set<ImportedBuild> getImportedBuilds()
    {
        return importedBuilds;
    }

    public void setImportedBuilds( Set<ImportedBuild> importedBuilds )
    {
        this.importedBuilds = importedBuilds;
    }
}
