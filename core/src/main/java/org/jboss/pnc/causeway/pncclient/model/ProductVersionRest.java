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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;


@XmlRootElement(name = "ProductVersion")
@Deprecated
public class ProductVersionRest {

    private Integer id;

    private String version;

    private Integer productId;

    private String productName;

    private Integer currentProductMilestoneId;

    List<ProductMilestoneRest> productMilestones = new ArrayList<ProductMilestoneRest>();

    List<ProductReleaseRest> productReleases = new ArrayList<ProductReleaseRest>();

    List<BuildConfigurationSetRest> buildConfigurationSets = new ArrayList<BuildConfigurationSetRest>();

    List<BuildConfigurationRest> buildConfigurations = new ArrayList<BuildConfigurationRest>();

    @Getter
    @Setter
    private Map<String, String> attributes = new HashMap<>();

    public ProductVersionRest() {
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

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public List<ProductMilestoneRest> getProductMilestones() {
        return productMilestones;
    }

    public void setProductMilestones(List<ProductMilestoneRest> productMilestones) {
        this.productMilestones = productMilestones;
    }

    public List<ProductReleaseRest> getProductReleases() {
        return productReleases;
    }

    public void setProductReleases(List<ProductReleaseRest> productReleases) {
        this.productReleases = productReleases;
    }

    public List<BuildConfigurationSetRest> getBuildConfigurationSets() {
        return buildConfigurationSets;
    }

    public void setBuildConfigurationSets(List<BuildConfigurationSetRest> buildConfigurationSets) {
        this.buildConfigurationSets = buildConfigurationSets;
    }

    public List<BuildConfigurationRest> getBuildConfigurations() {
        return buildConfigurations;
    }

    public void setBuildConfigurations(List<BuildConfigurationRest> buildConfigurations) {
        this.buildConfigurations = buildConfigurations;
    }

    public Integer getCurrentProductMilestoneId() {
        return currentProductMilestoneId;
    }

    public void setCurrentProductMilestoneId(Integer currentProductMilestoneId) {
        this.currentProductMilestoneId = currentProductMilestoneId;
    }

}
