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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@XmlRootElement(name = "ProductRelease")
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductReleaseRest {

    public enum SupportLevel {

        UNRELEASED, EARLYACCESS, SUPPORTED, EXTENDED_SUPPORT, EOL
    }

    private Integer id;

    private String version;

    private Date releaseDate;

    private String downloadUrl;

    private String issueTrackerUrl;

    private Integer productVersionId;

    private Integer productMilestoneId;

    private SupportLevel supportLevel;

    public ProductReleaseRest() {
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

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
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

    public Integer getProductMilestoneId() {
        return productMilestoneId;
    }

    public void setProductMilestoneId(Integer productMilestoneId) {
        this.productMilestoneId = productMilestoneId;
    }

    public SupportLevel getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(SupportLevel supportLevel) {
        this.supportLevel = supportLevel;
    }

}
