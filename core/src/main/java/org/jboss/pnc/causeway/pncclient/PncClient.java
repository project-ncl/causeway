package org.jboss.pnc.causeway.pncclient;

import static java.util.stream.Collectors.toList;

import static org.apache.http.client.utils.HttpClientUtils.closeQuietly;
import static org.jboss.resteasy.util.HttpResponseCodes.SC_OK;

import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.JHttpCException;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.PncBuild.PncArtifact;
import org.jboss.pnc.rest.restmodel.ArtifactRest;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.response.Page;
import org.jboss.pnc.rest.restmodel.response.Singleton;
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

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Created by jdcasey on 2/9/16.
 */
@ApplicationScoped
public class PncClient
{
    private static final String BUILD_RECORDS_PER_RELEASE_RESOURCE = "/some/path/to/a/rest/call";
    public static final int MAX_ARTIFACTS = 20000;

    private final HttpFactory httpFactory;

    private final CausewayConfig config;

    private final RestEndpointProxyFactory restEndpointProxyFactory;

    @Inject
    public PncClient(CausewayConfig config, HttpFactory httpFactory )
    {
        this(config, httpFactory, new RestEndpointProxyFactory(config, new ResteasyClientBuilder().build()));
    }

    PncClient(CausewayConfig config, HttpFactory httpFactory, RestEndpointProxyFactory restEndpointProxyFactory) {
        this.config = config;
        this.httpFactory = httpFactory;
        this.restEndpointProxyFactory = restEndpointProxyFactory;
    }

//    public Set<BuildRecord> findBuildIdsOfProductRelease( Integer releaseId )
//            throws ProjectNewcastleClientException
//    {
//        Set<BuildRecord> result = new HashSet<>();
//        withClient( (client)->{
//            HttpGet request = new HttpGet( Paths.get( config.getPnclURL(), BUILD_RECORDS_PER_RELEASE_RESOURCE, releaseId.toString() ).toUri() );
//
//            CloseableHttpResponse response = client.execute( request );
//            // TODO: deserialize response body
//
//            return new ClientCommandResult();
//        });
//
//        return result;
//    }

    private void withClient( ClientCommands commands )
            throws PncClientException
    {
        CloseableHttpClient client = null;
        try
        {
            client = httpFactory.createClient( config.getPnclSiteConfig() );
            commands.execute( client ).throwError();
        }
        catch ( JHttpCException e )
        {
            throw new PncClientException( "Failed to create HTTP client to communicate with Project Newcastle.", e );
        }
        catch ( IOException e )
        {
            throw new PncClientException( "Communication with Project Newcastle server failed.", e );
        }
        finally
        {
            closeQuietly( client );
        }
    }

    public Collection<Integer> findBuildIdsOfProductRelease(int productReleaseId) throws CausewayException {
        Response response = null;
        try {
            ProductReleaseEndpoint endpoint = restEndpointProxyFactory.createRestEndpoint(ProductReleaseEndpoint.class);
            response = endpoint.getAllBuildsInDistributedRecordsetOfProductRelease(productReleaseId);
            if (response.getStatus() == SC_OK) {
                Page<BuildRecordRest> wrapper = ((Page<BuildRecordRest>) response.readEntity(new GenericType<Page<BuildRecordRest>>() {}));
                return extractIds(wrapper.getContent());
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        throw new CausewayException("Can not read build ids for product release " + productReleaseId + ( response == null ? "" : " - response " + response.getStatus()));
    }

    static List<Integer> extractIds(Collection<BuildRecordRest> content) {
        return content.stream().map(record -> record.getId()).collect(toList());
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

    public BuildRecordRest findBuild(int buildId) throws CausewayException{
        Response response = null;
        try {
            BuildRecordEndpoint endpoint = restEndpointProxyFactory.createRestEndpoint(BuildRecordEndpoint.class);

            response = endpoint.getSpecific(buildId);

            if (response.getStatus() == SC_OK) {
                return response.readEntity(new GenericType<Singleton<BuildRecordRest>>() {}).getContent();
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        throw new CausewayException("Can read info for build id " + buildId + ( response == null ? "" : " - response " + response.getStatus()));
    }

    public PncBuild findBuild(Integer buildId) throws CausewayException {
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

            PncBuild build = new PncBuild(buildId);

            for (ArtifactRest artifactRest : artifactRestsBuilt) {
                PncArtifact artifact = new PncArtifact("maven", artifactRest.getIdentifier(), artifactRest.getFilename(), artifactRest.getChecksum(), artifactRest.getDeployUrl());
                build.buildArtifacts.add(artifact);
            }
            for (ArtifactRest artifactRest : artifactRestsDepend) {
                PncArtifact artifact = new PncArtifact("maven", artifactRest.getIdentifier(), artifactRest.getFilename(), artifactRest.getChecksum(), artifactRest.getDeployUrl());
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

    public interface ClientCommands
    {
        ClientCommandResult execute( CloseableHttpClient client )
                throws IOException;
    }

    public class ClientCommandResult
    {
        private PncClientException error;

        public ClientCommandResult( PncClientException error )
        {
            this.error = error;
        }

        public ClientCommandResult()
        {
        }

        public ClientCommandResult throwError()
                throws PncClientException
        {
            if ( error != null )
            {
                throw error;
            }

            return this;
        }
    }

    public static final String PAGE_INDEX_QUERY_PARAM = "pageIndex";
    public static final String PAGE_INDEX_DEFAULT_VALUE = "0";
    public static final String PAGE_SIZE_QUERY_PARAM = "pageSize";
    public static final String PAGE_SIZE_DEFAULT_VALUE = "50";
    public static final String SORTING_QUERY_PARAM = "sort";
    public static final String QUERY_QUERY_PARAM = "q";

    @Path("/product-releases")
    @Consumes("application/json")
    public interface ProductReleaseEndpoint { //FIXME remove when resolved https://projects.engineering.redhat.com/browse/NCL-1645

        @GET
        @Path("/{id}/distributed-build-records-ids")
        public Response getAllBuildsInDistributedRecordsetOfProductRelease(
                @PathParam("id") Integer id);
        @GET
        @Path("/{id}")
        public Response getSpecific(@PathParam("id") Integer id);

    }

    @Path("/build-records")
    @Consumes("application/json")
    public interface BuildRecordEndpoint { //FIXME remove when resolved https://projects.engineering.redhat.com/browse/NCL-1645

        @GET
        @Path("/{id}/artifacts")
        public Response getArtifacts(@PathParam("id") Integer id,
                @QueryParam(PAGE_INDEX_QUERY_PARAM) @DefaultValue(PAGE_INDEX_DEFAULT_VALUE) int pageIndex,
                @QueryParam(PAGE_SIZE_QUERY_PARAM) @DefaultValue(PAGE_SIZE_DEFAULT_VALUE) int pageSize,
                @QueryParam(SORTING_QUERY_PARAM) String sort,
                @QueryParam(QUERY_QUERY_PARAM) String q);

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
        @Path("/{id}")
        public Response getSpecific(@PathParam("id") Integer id);
    }

}
