package org.jboss.pnc.causeway.rest.model;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil <janinko.g@gmail.com>
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
        property = "@buildType")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes(
    @JsonSubTypes.Type(MavenBuild.class))
public abstract class Build {

    @NonNull
    private final String buildName;
    @NonNull
    private final String buildVersion;
    @NonNull
    private final String externalBuildSystem;
    private final int externalBuildID;
    @NonNull
    private final String externalBuildURL;
    @NonNull
    private final Date startTime;
    @NonNull
    private final Date endTime;
    @NonNull
    private final String scmURL;
    @NonNull
    private final String scmRevision;
    @NonNull
    private final BuildRoot buildRoot;
    @NonNull
    private final Set<Logfile> logs;
    @NonNull
    private final Set<Dependency> dependencies;
    @NonNull
    private final Set<BuiltArtifact> builtArtifacts;
    @NonNull
    private final String tagPrefix;

    // We use IDE generated constructor instead of lombok.AllArgsConstrucotr because of nicer
    // parametr names.
    protected Build(String buildName, String buildVersion, String externalBuildSystem,
            int externalBuildID, String externalBuildURL, Date startTime, Date endTime,
            String scmURL, String scmRevision, BuildRoot buildRoot, Set<Logfile> logs,
            Set<Dependency> dependencies, Set<BuiltArtifact> builtArtifacts, String tagPrefix) {
        this.buildName = buildName;
        this.buildVersion = buildVersion;
        this.externalBuildSystem = externalBuildSystem;
        this.externalBuildID = externalBuildID;
        this.externalBuildURL = externalBuildURL;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scmURL = scmURL;
        this.scmRevision = scmRevision;
        this.buildRoot = buildRoot;
        this.logs = logs;
        this.dependencies = dependencies;
        this.builtArtifacts = builtArtifacts;
        this.tagPrefix = tagPrefix;
    }

}
