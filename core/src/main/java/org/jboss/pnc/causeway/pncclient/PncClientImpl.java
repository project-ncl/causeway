package org.jboss.pnc.causeway.pncclient;

import static org.jboss.resteasy.util.HttpResponseCodes.SC_OK;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts.PncArtifact;
import org.jboss.pnc.rest.restmodel.ArtifactRest;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.response.Page;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by jdcasey on 2/9/16.
 */
@ApplicationScoped
public class PncClientImpl implements PncClient
{
    public static final int MAX_ARTIFACTS = 20000;
    public static final int MAX_BUILDS = 20000;

    private final RestEndpointProxyFactory restEndpointProxyFactory;

    @Inject
    public PncClientImpl(CausewayConfig config)
    {
        this(new RestEndpointProxyFactory(config, new ResteasyClientBuilder().build()));
    }

    PncClientImpl(RestEndpointProxyFactory restEndpointProxyFactory) {
        this.restEndpointProxyFactory = restEndpointProxyFactory;
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
        Response responseBuilt = null;
        Response responseDepend = null;
        try {
            BuildRecordEndpoint endpoint = restEndpointProxyFactory.createRestEndpoint(BuildRecordEndpoint.class);

            responseBuilt = endpoint.getBuiltArtifacts(buildId, 0, MAX_ARTIFACTS, "", "");
            if (responseBuilt.getStatus() != SC_OK) {
                throw new CausewayException("Can read info for build id " + buildId + " - responseBuilt " + responseBuilt.getStatus());
            }
            Collection<ArtifactRest> artifactRestsBuilt = ((Page<ArtifactRest>) responseBuilt.readEntity(new GenericType<Page<ArtifactRest>>() {})).getContent();
            responseBuilt.close();
            responseBuilt = null;

            responseDepend = endpoint.getDependencyArtifacts(buildId, 0, MAX_ARTIFACTS, "", "");
            if (responseDepend.getStatus() != SC_OK) {
                throw new CausewayException("Can read info for build id " + buildId  + " - responseDepend " + responseDepend.getStatus());
            }
            Collection<ArtifactRest> artifactRestsDepend = ((Page<ArtifactRest>) responseDepend.readEntity(new GenericType<Page<ArtifactRest>>() {})).getContent();
            responseDepend.close();
            responseDepend = null;

            BuildArtifacts build = new BuildArtifacts();

            for (ArtifactRest artifactRest : artifactRestsBuilt) {
                PncArtifact artifact = new PncArtifact("maven", artifactRest.getIdentifier(), artifactRest.getFilename(), artifactRest.getChecksum(), artifactRest.getDeployUrl(), artifactRest.getSize());
                build.buildArtifacts.add(artifact);
            }
            for (ArtifactRest artifactRest : artifactRestsDepend) {
                PncArtifact artifact = new PncArtifact("maven", artifactRest.getIdentifier(), artifactRest.getFilename(), artifactRest.getChecksum(), artifactRest.getDeployUrl(), artifactRest.getSize());
                build.dependencies.add(artifact);
            }
            return build;
        } finally {
            if (responseBuilt != null) {
                responseBuilt.close();
            }
            if (responseDepend != null) {
                responseDepend.close();
            }
        }
    }

    public static final String PAGE_INDEX_QUERY_PARAM = "pageIndex";
    public static final String PAGE_INDEX_DEFAULT_VALUE = "0";
    public static final String PAGE_SIZE_QUERY_PARAM = "pageSize";
    public static final String PAGE_SIZE_DEFAULT_VALUE = "50";
    public static final String SORTING_QUERY_PARAM = "sort";
    public static final String QUERY_QUERY_PARAM = "q";

    @Path("/product-milestones")
    @Consumes("application/json")
    public interface ProductMilestoneEndpoint { //FIXME remove when resolved https://projects.engineering.redhat.com/browse/NCL-1645

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

    }

}
