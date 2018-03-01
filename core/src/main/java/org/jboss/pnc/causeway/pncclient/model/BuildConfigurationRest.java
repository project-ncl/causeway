/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.pncclient.model;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement(name = "Configuration")
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildConfigurationRest {

    private Integer id;

    private String name;

    private String description;

    private String buildScript;

    private String scmRepoURL;

    private String scmRevision;

    @Getter
    @Setter
    private String scmExternalRepoURL;

    @Getter
    @Setter
    private String scmExternalRevision;

    @Deprecated // no longer used
    @Getter
    @Setter
    private String scmMirrorRepoURL;

    @Deprecated // no longer used
    @Getter
    @Setter
    private String scmMirrorRevision;

    private Date creationTime;

    private Date lastModificationTime;

    private boolean archived;

    private String repositories;

    private ProjectRest project;

    private BuildEnvironmentRest environment;

    private Set<Integer> dependencyIds;

    private Integer productVersionId;

    public BuildConfigurationRest() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBuildScript() {
        return buildScript;
    }

    public void setBuildScript(String buildScript) {
        this.buildScript = buildScript;
    }

    public String getScmRepoURL() {
        return scmRepoURL;
    }

    public void setScmRepoURL(String scmRepoURL) {
        this.scmRepoURL = scmRepoURL;
    }

    public String getScmRevision() {
        return scmRevision;
    }

    public void setScmRevision(String scmRevision) {
        this.scmRevision = scmRevision;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getLastModificationTime() {
        return lastModificationTime;
    }

    public void setLastModificationTime(Date lastModificationTime) {
        this.lastModificationTime = lastModificationTime;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getRepositories() {
        return repositories;
    }

    public void setRepositories(String repositories) {
        this.repositories = repositories;
    }

    public ProjectRest getProject() {
        return project;
    }

    public void setProject(ProjectRest project) {
        this.project = project;
    }

    public Integer getProductVersionId() {
        return productVersionId;
    }

    public void setProductVersionId(Integer productVersionId) {
        this.productVersionId = productVersionId;
    }

    public Set<Integer> getDependencyIds() {
        return dependencyIds;
    }

    public void setDependencyIds(Set<Integer> dependencyIds) {
        this.dependencyIds = dependencyIds;
    }

    public boolean addDependency(Integer dependencyId) {
        return dependencyIds.add(dependencyId);
    }

    public boolean removeDependency(Integer dependencyId) {
        return dependencyIds.remove(dependencyId);
    }

    public BuildEnvironmentRest getEnvironment() {
        return environment;
    }

    public void setEnvironment(BuildEnvironmentRest environment) {
        this.environment = environment;
    }
}
