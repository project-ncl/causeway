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

import javax.xml.bind.annotation.XmlRootElement;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement(name = "ProductMilestone")
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductMilestoneRest {

    private Integer id;

    private String version;

    private Date endDate;

    private Date startingDate;

    private Date plannedEndDate;

    private String downloadUrl;

    private String issueTrackerUrl;

    private Integer productVersionId;

    private Set<Integer> performedBuilds;

    private Set<Integer> distributedArtifactIds;

    private Integer productReleaseId;

    public ProductMilestoneRest() {
    }

    public ProductMilestoneRest(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartingDate() {
        return startingDate;
    }

    public void setStartingDate(Date startingDate) {
        this.startingDate = startingDate;
    }

    public Date getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(Date plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getIssueTrackerUrl() {
        return issueTrackerUrl;
    }

    public void setIssueTrackerUrl(String issueTrackerUrl) {
        this.issueTrackerUrl = issueTrackerUrl;
    }

    public Integer getProductVersionId() {
        return productVersionId;
    }

    public void setProductVersionId(Integer productVersionId) {
        this.productVersionId = productVersionId;
    }

    public Integer getProductReleaseId() {
        return productReleaseId;
    }

    public void setProductReleaseId(Integer productReleaseId) {
        this.productReleaseId = productReleaseId;
    }

    public Set<Integer> getPerformedBuilds() {
        return performedBuilds;
    }

    public void setPerformedBuildRecordSetId(Set<Integer> performedBuilds) {
        this.performedBuilds = performedBuilds;
    }

    public Set<Integer> getDistributedArtifactIds() {
        return distributedArtifactIds;
    }

    public void setDistributedBuildRecordSetId(Set<Integer> distributedArtifactIds) {
        this.distributedArtifactIds = distributedArtifactIds;
    }

}
