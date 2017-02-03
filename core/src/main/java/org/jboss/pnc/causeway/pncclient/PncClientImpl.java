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
package org.jboss.pnc.causeway.pncclient;

import static org.jboss.resteasy.util.HttpResponseCodes.SC_OK;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts.PncArtifact;
import org.jboss.pnc.model.ProductVersion;
import org.jboss.pnc.rest.restmodel.ArtifactRest;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.ProductMilestoneRest;
import org.jboss.pnc.rest.restmodel.ProductVersionRest;
import org.jboss.pnc.rest.restmodel.response.Page;
import org.jboss.pnc.rest.restmodel.response.Singleton;
import static org.jboss.pnc.spi.BuildCoordinationStatus.DONE;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Created by jdcasey on 2/9/16.
 */
@ApplicationScoped
public class PncClientImpl implements PncClient
{
    public static final int MAX_ARTIFACTS = 200;
    public static final int MAX_BUILDS = 20000;

    private final RestEndpointProxyFactory restEndpointProxyFactory;

    @Inject
    public PncClientImpl(CausewayConfig config)
    {
        this(new RestEndpointProxyFactory(config, new ResteasyClientBuilder().connectionPoolSize(10).build()));
    }

    PncClientImpl(RestEndpointProxyFactory restEndpointProxyFactory) {
        this.restEndpointProxyFactory = restEndpointProxyFactory;
    }

    @Override
    public String getTagForMilestone(int milestoneId) throws CausewayException {
        ProductMilestoneEndpoint milestoneEndpoint = restEndpointProxyFactory.createRestEndpoint(ProductMilestoneEndpoint.class);
        ProductVersionEndpoint versionEndpoint = restEndpointProxyFactory.createRestEndpoint(ProductVersionEndpoint.class);
        Response response = null;
        try {
            response = milestoneEndpoint.getSpecific(milestoneId);
            if (response.getStatus() == SC_OK) {
                Singleton<ProductMilestoneRest> milestone = ((Singleton<ProductMilestoneRest>) response.readEntity(new GenericType<Singleton<ProductMilestoneRest>>() {
                }));
                Integer productVersionId = milestone.getContent().getProductVersionId();
                response.close();

                response = versionEndpoint.getSpecific(productVersionId);
                Singleton<ProductVersionRest> version = ((Singleton<ProductVersionRest>) response.readEntity(new GenericType<Singleton<ProductVersionRest>>() {
                }));

                return version.getContent().getAttributes().get(ProductVersion.ATTRIBUTE_KEY_BREW_TAG_PREFIX);
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        throw new CausewayException("Can not read tag for product version of milestone " + milestoneId + (response == null ? "" : " - response " + response.getStatus()));
    }

    @Override
    public Collection<BuildRecordRest> findBuildsOfProductMilestone(int milestoneId) throws CausewayException {
        Response response = null;
        try {
            ProductMilestoneEndpoint endpoint = restEndpointProxyFactory.createRestEndpoint(ProductMilestoneEndpoint.class);
            response = endpoint.getPerformedBuilds(milestoneId, 0, MAX_BUILDS, "", "");
            if (response.getStatus() == SC_OK) {
                Page<BuildRecordRest> wrapper = ((Page<BuildRecordRest>) response.readEntity(new GenericType<Page<BuildRecordRest>>() {}));
                return wrapper.getContent().stream().filter(b -> b.getStatus() == DONE).collect(Collectors.toSet());
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        throw new CausewayException("Can not read builds for product milestone " + milestoneId + ( response == null ? "" : " - response " + response.getStatus()));
    }

    @Override
    public String getBuildLog(int buildId) throws CausewayException {
        Response response = null;
        try {
            BuildRecordEndpoint endpoint = restEndpointProxyFactory.createRestEndpoint(BuildRecordEndpoint.class);
            response = endpoint.getLogs(buildId);
            if (response.getStatus() == SC_OK) {
                return response.readEntity(String.class);
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        throw new CausewayException("Can not read build log of build " + buildId + (response == null ? "" : " - response " + response.getStatus()));
    }

    static class RestEndpointProxyFactory {
        private final ResteasyClient client;
        private final CausewayConfig config;

        public RestEndpointProxyFactory(CausewayConfig config, ResteasyClient client) {
            this.config = config;
            this.client = client;
        }

        public <T> T createRestEndpoint(Class<T> aClass) {
            ResteasyWebTarget target = client.target(config.getPnclURL());
            return target.proxy(aClass);
        }
    }

    @Override
    public BuildArtifacts findBuildArtifacts(Integer buildId) throws CausewayException {
        BuildRecordEndpoint endpoint = restEndpointProxyFactory.createRestEndpoint(BuildRecordEndpoint.class);

        Collection<ArtifactRest> artifactRestsBuilt = getArtifacts(buildId, (p) -> endpoint.getBuiltArtifacts(buildId, p, MAX_ARTIFACTS, "", ""));
        Collection<ArtifactRest> artifactRestsDepend = getArtifacts(buildId, (p) -> endpoint.getDependencyArtifacts(buildId, p, MAX_ARTIFACTS, "", ""));

        BuildArtifacts build = new BuildArtifacts();

        for (ArtifactRest artifactRest : artifactRestsBuilt) {
            PncArtifact artifact = toPncArtifact(artifactRest);
            build.buildArtifacts.add(artifact);
        }
        for (ArtifactRest artifactRest : artifactRestsDepend) {
            PncArtifact artifact = toPncArtifact(artifactRest);
            build.dependencies.add(artifact);
        }
        return build;
    }

    private PncArtifact toPncArtifact(ArtifactRest artifactRest) {
        return new PncArtifact(artifactRest.getId(),
                "maven",
                artifactRest.getIdentifier(),
                artifactRest.getFilename(),
                artifactRest.getMd5(),
                artifactRest.getDeployUrl(),
                artifactRest.getSize() == null ? 1 : artifactRest.getSize());
    }

    public Collection<ArtifactRest> getArtifacts(Integer buildId, IntFunction<Response> query) throws CausewayException{
        Response response = null;
        try{
            Collection<ArtifactRest> artifacts = new ArrayList<>();
            response = query.apply(0);
            if (response.getStatus() != SC_OK) {
                throw new CausewayException("Can't read info for build id " + buildId + " - response " + response.getStatus() + ": " + response.getEntity());
            }
            Page<ArtifactRest> page = (Page<ArtifactRest>) response.readEntity(new GenericType<Page<ArtifactRest>>() {});
            response.close();

            artifacts.addAll(page.getContent());
            for(int p=1; p< page.getTotalPages(); p++){
                response = query.apply(p);
                if (response.getStatus() != SC_OK) {
                    throw new CausewayException("Can't read info for build id " + buildId + " - response " + response.getStatus() + ": " + response.getEntity());
                }
                page = (Page<ArtifactRest>) response.readEntity(new GenericType<Page<ArtifactRest>>() {});
                artifacts.addAll(page.getContent());
            }
            return artifacts;
        }finally{
            if (response != null) {
                response.close();
            }
        }
    }

    public static final String PAGE_INDEX_QUERY_PARAM = "pageIndex";
    public static final String PAGE_INDEX_DEFAULT_VALUE = "0";
    public static final String PAGE_SIZE_QUERY_PARAM = "pageSize";
    public static final String PAGE_SIZE_DEFAULT_VALUE = "50";
    public static final String SORTING_QUERY_PARAM = "sort";
    public static final String QUERY_QUERY_PARAM = "q";

    @Path("/product-versions")
    @Consumes("application/json")
    public interface ProductVersionEndpoint { //FIXME remove when resolved https://projects.engineering.redhat.com/browse/NCL-1645

        @GET
        @Path("/{id}")
        public Response getSpecific(@PathParam("id") Integer id);

    }

    @Path("/product-milestones")
    @Consumes("application/json")
    public interface ProductMilestoneEndpoint { //FIXME remove when resolved https://projects.engineering.redhat.com/browse/NCL-1645

        @GET
        @Path("/{id}")
        public Response getSpecific(@PathParam("id") Integer id);

        @GET
        @Path("/{id}/performed-builds")
        public Response getPerformedBuilds(
                @PathParam("id") Integer id,
                @QueryParam(PAGE_INDEX_QUERY_PARAM) @DefaultValue(PAGE_INDEX_DEFAULT_VALUE) int pageIndex,
                @QueryParam(PAGE_SIZE_QUERY_PARAM) @DefaultValue(PAGE_SIZE_DEFAULT_VALUE) int pageSize,
                @QueryParam(SORTING_QUERY_PARAM) String sort,
                @QueryParam(QUERY_QUERY_PARAM) String q);

        @GET
        @Path("/{id}/distributed-builds")
        public Response getDistributedBuilds(
                @QueryParam(PAGE_INDEX_QUERY_PARAM) @DefaultValue(PAGE_INDEX_DEFAULT_VALUE) int pageIndex,
                @QueryParam(PAGE_SIZE_QUERY_PARAM) @DefaultValue(PAGE_SIZE_DEFAULT_VALUE) int pageSize,
                @QueryParam(SORTING_QUERY_PARAM) String sort,
                @QueryParam(QUERY_QUERY_PARAM) String q,
                @PathParam("id") Integer milestoneId);

    }

    @Path("/build-records")
    @Consumes("application/json")
    public interface BuildRecordEndpoint { //FIXME remove when resolved https://projects.engineering.redhat.com/browse/NCL-1645

        @GET
        @Path("/{id}/built-artifacts")
        public Response getBuiltArtifacts(@PathParam("id") Integer id,
                @QueryParam(PAGE_INDEX_QUERY_PARAM) @DefaultValue(PAGE_INDEX_DEFAULT_VALUE) int pageIndex,
                @QueryParam(PAGE_SIZE_QUERY_PARAM) @DefaultValue(PAGE_SIZE_DEFAULT_VALUE) int pageSize,
                @QueryParam(SORTING_QUERY_PARAM) String sort,
                @QueryParam(QUERY_QUERY_PARAM) String q);

        @GET
        @Path("/{id}/dependency-artifacts")
        public Response getDependencyArtifacts(@PathParam("id") Integer id,
                @QueryParam(PAGE_INDEX_QUERY_PARAM) @DefaultValue(PAGE_INDEX_DEFAULT_VALUE) int pageIndex,
                @QueryParam(PAGE_SIZE_QUERY_PARAM) @DefaultValue(PAGE_SIZE_DEFAULT_VALUE) int pageSize,
                @QueryParam(SORTING_QUERY_PARAM) String sort,
                @QueryParam(QUERY_QUERY_PARAM) String q);

        @GET
        @Path("/{id}/log")
        @Produces(MediaType.TEXT_PLAIN)
        public Response getLogs(@PathParam("id") Integer id);

    }

}
