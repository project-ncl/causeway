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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement(name = "BuildRecord")
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildRecordRest {

    private Integer id;

    private Date submitTime;

    private Date startTime;

    private Date endTime;

    private BuildCoordinationStatus status;

    private Integer buildConfigurationId;

    private String buildConfigurationName;

    private Integer buildConfigurationRev;

    private Integer projectId;

    private String projectName;

    private Integer userId;

    private String username;

    private String scmRepoURL;

    private String scmRevision;

    private Integer buildEnvironmentId;

    private Map<String, String> attributes = new HashMap<>();

    private String liveLogsUri;

    private Integer buildConfigSetRecordId;

    private String buildContentId;

    /**
     * The IDs of the build record sets which represent the builds performed for a milestone to which this build record belongs
     */
    private Integer productMilestoneId;

    /**
     * Required in order to use rsql on user
     */
    private UserRest user;

    /**
     * Required in order to use rsql on buildConfiguration
     */
    private BuildConfigurationAuditedRest buildConfigurationAudited;

    @Getter
    @Setter
    private String executionRootName;

    @Getter
    @Setter
    private String executionRootVersion;

    public BuildRecordRest() {
    }

    public BuildRecordRest(Integer id, BuildCoordinationStatus buildCoordinationStatus, Date submitTime, Date startTime,
            Date endTime, UserRest user, BuildConfigurationAuditedRest buildConfigurationAudited) {
        this.id = id;
        this.submitTime = submitTime;
        this.startTime = startTime;
        this.endTime = endTime;

        this.status = buildCoordinationStatus;

        this.userId = user.getId();
        this.username = user.getUsername();

        this.user = user;
        this.buildConfigurationAudited = buildConfigurationAudited;
        this.buildConfigurationId = buildConfigurationAudited.getId();
        this.buildConfigurationName = buildConfigurationAudited.getName();
        this.projectId = buildConfigurationAudited.getProjectId();

        if (buildConfigurationAudited.getProject() != null) {
            this.projectName = buildConfigurationAudited.getProject().getName();
        }

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public BuildCoordinationStatus getStatus() {
        return status;
    }

    public void setStatus(BuildCoordinationStatus status) {
        this.status = status;
    }

    public Integer getBuildConfigurationId() {
        return buildConfigurationId;
    }

    public void setBuildConfigurationId(Integer buildConfigurationId) {
        this.buildConfigurationId = buildConfigurationId;
    }

    public Integer getBuildConfigurationRev() {
        return buildConfigurationRev;
    }

    public void setBuildConfigurationRev(Integer buildConfigurationRev) {
        this.buildConfigurationRev = buildConfigurationRev;
    }

    public String getBuildConfigurationName() {
        return buildConfigurationName;
    }

    public void setBuildConfigurationName(String buildConfigurationName) {
        this.buildConfigurationName = buildConfigurationName;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public Integer getBuildEnvironmentId() {
        return buildEnvironmentId;
    }

    public void setBuildEnvironmentId(Integer buildEnvironmentId) {
        this.buildEnvironmentId = buildEnvironmentId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void putAttribute(String name, String value) {
        this.attributes.put(name, value);
    }

    public String getLiveLogsUri() {
        return liveLogsUri;
    }

    public void setLiveLogsUri(String liveLogsUri) {
        this.liveLogsUri = liveLogsUri;
    }

    public Integer getBuildConfigSetRecordId() {
        return buildConfigSetRecordId;
    }

    public String getBuildContentId() {
        return buildContentId;
    }

    public void setBuildContentId(String buildContentId) {
        this.buildContentId = buildContentId;
    }

    public Integer getProductMilestoneId() {
        return productMilestoneId;
    }

    public void setProductMilestoneId(Integer productMilestoneId) {
        this.productMilestoneId = productMilestoneId;
    }

    public UserRest getUser() {
        return user;
    }

    public BuildConfigurationAuditedRest getBuildConfigurationAudited() {
        return buildConfigurationAudited;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }
}
