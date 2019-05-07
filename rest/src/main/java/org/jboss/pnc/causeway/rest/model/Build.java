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
package org.jboss.pnc.causeway.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NonNull;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Honza Brázdil &lt;janinko.g@gmail.com&gt;
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@buildType")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({ @JsonSubTypes.Type(MavenBuild.class), @JsonSubTypes.Type(NpmBuild.class) })
public abstract class Build {

    @NonNull
    private final String buildName;
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

    private final String scmTag;
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
    protected Build(
            String buildName,
            String buildVersion,
            String externalBuildSystem,
            int externalBuildID,
            String externalBuildURL,
            Date startTime,
            Date endTime,
            String scmURL,
            String scmRevision,
            String scmTag,
            BuildRoot buildRoot,
            Set<Logfile> logs,
            Set<Dependency> dependencies,
            Set<BuiltArtifact> builtArtifacts,
            String tagPrefix) {
        this.buildName = Objects.requireNonNull(buildName);
        this.buildVersion = buildVersion;
        this.externalBuildSystem = Objects.requireNonNull(externalBuildSystem);
        this.externalBuildID = externalBuildID;
        this.externalBuildURL = Objects.requireNonNull(externalBuildURL);
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
        this.scmURL = Objects.requireNonNull(scmURL);
        this.scmRevision = Objects.requireNonNull(scmRevision);
        this.scmTag = scmTag;
        this.buildRoot = Objects.requireNonNull(buildRoot);
        this.logs = Objects.requireNonNull(logs);
        this.dependencies = Objects.requireNonNull(dependencies);
        this.builtArtifacts = Objects.requireNonNull(builtArtifacts);
        this.tagPrefix = Objects.requireNonNull(tagPrefix);
    }

}
