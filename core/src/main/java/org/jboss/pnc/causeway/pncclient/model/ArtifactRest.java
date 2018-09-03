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

@XmlRootElement(name = "Artifact")
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactRest {

    public enum Quality {

        /**
         * The artifact has not yet been verified or tested
         */
        NEW,
        /**
         * The artifact has been verified by an automated process, but has not yet been tested against
         * a complete product or other large set of components.
         */
        VERIFIED,
        /**
         * The artifact has passed integration testing.
         */
        TESTED,
        /**
         * The artifact should no longer be used due to lack of support and/or a better alternative
         * being available.
         */
        DEPRECATED,
        /**
         * The artifact contains a severe defect, possibly a functional or security issue.
         */
        BLACKLISTED,
        TEMPORARY,

    }
    public enum Type {

        /**
         * Maven artifact repository such as Maven central (http://central.maven.org/maven2/)
         */
        MAVEN,
        /**
         * Node.js package repository such as https://registry.npmjs.org/
         */
        NPM,
        /**
         * CocoaPod repository for managing Swift and Objective-C Cocoa dependencies
         */
        COCOA_POD,
        /**
         * Generic HTTP proxy that captures artifacts with an unsupported, or no specific, repository type.
         */
        GENERIC_PROXY
    }

    private Integer id;

    private String identifier;

    private Quality artifactQuality;

    private Type repoType;

    @Getter
    @Setter
    private String md5;

    @Getter
    @Setter
    private String sha1;

    @Getter
    @Setter
    private String sha256;

    private String filename;

    private String deployPath;

    private Set<Integer> buildRecordIds;

    private Set<Integer> dependantBuildRecordIds;

    private Date importDate;

    private String originUrl;

    @Getter
    @Setter
    private Long size;

    /**
     * Internal url to the artifact using internal (cloud) network domain
     */
    @Getter
    @Setter
    private String deployUrl;

    /**
     * Public url to the artifact using public network domain
     */
    @Getter
    @Setter
    private String publicUrl;

    public ArtifactRest() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Type getRepoType() {
        return repoType;
    }

    public void setRepoType(Type repoType) {
        this.repoType = repoType;
    }

    public Quality getArtifactQuality() {
        return artifactQuality;
    }

    public void setArtifactQuality(Quality artifactQuality) {
        this.artifactQuality = artifactQuality;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public Date getImportDate() {
        return importDate;
    }

    public void setImportDate(Date importDate) {
        this.importDate = importDate;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public Set<Integer> getBuildRecordIds() {
        return buildRecordIds;
    }

    public void setBuildRecordIds(Set<Integer> buildRecordIds) {
        this.buildRecordIds = buildRecordIds;
    }

    public Set<Integer> getDependantBuildRecordIds() {
        return dependantBuildRecordIds;
    }

    public void setDependantBuildRecordIds(Set<Integer> dependantBuildRecordIds) {
        this.dependantBuildRecordIds = dependantBuildRecordIds;
    }

    @Override
    public String toString() {
        return "ArtifactRest{"
                + "id=" + id
                + ", identifier='" + identifier + '\''
                + ", artifactQuality=" + artifactQuality
                + ", repoType=" + repoType
                + ", md5='" + md5 + '\''
                + ", sha1='" + sha1 + '\''
                + ", sha256='" + sha256 + '\''
                + ", filename='" + filename + '\''
                + ", deployPath='" + deployPath + '\''
                + ", buildRecordIds=" + buildRecordIds
                + ", dependantBuildRecordIds=" + dependantBuildRecordIds
                + ", importDate=" + importDate
                + ", originUrl='" + originUrl + '\''
                + ", deployUrl='" + deployUrl + '\''
                + ", publicUrl='" + publicUrl + '\''
                + '}';
    }
}
