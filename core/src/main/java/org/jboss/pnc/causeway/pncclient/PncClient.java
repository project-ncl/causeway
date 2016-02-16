package org.jboss.pnc.causeway.pncclient;

import static org.apache.http.client.utils.HttpClientUtils.closeQuietly;

import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.JHttpCException;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.rest.restmodel.ProductMilestoneRest;
import org.jboss.pnc.rest.restmodel.ProductReleaseRest;
import org.jboss.pnc.rest.restmodel.response.Singleton;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jdcasey on 2/9/16.
 */
@ApplicationScoped
public class PncClient
{
    private static final String BUILD_RECORDS_PER_RELEASE_RESOURCE = "/some/path/to/a/rest/call";

    private final HttpFactory httpFactory;

    private final CausewayConfig config;

    private final RestEndPointFactory restEndPointFactory;

    @Inject
    public PncClient(CausewayConfig config, HttpFactory httpFactory )
    {
        this(config, httpFactory, new RestEndPointFactory(config, new ResteasyClientBuilder().build()));
    }

    PncClient(CausewayConfig config, HttpFactory httpFactory, RestEndPointFactory restEndPointFactory) {
        this.config = config;
        this.httpFactory = httpFactory;
        this.restEndPointFactory = restEndPointFactory;
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

    public Set<Long> findBuildIdsOfProductRelease(int productReleaseId) {
        ProductReleaseEndpoint productReleaseEndpoint = restEndPointFactory.createRestEndpoint(ProductReleaseEndpoint.class);
        ProductReleaseRest productRelease = productReleaseEndpoint.getEntity(productReleaseId).getContent();
        Integer productMilestoneId = productRelease.getProductMilestoneId();

        ProductMilestoneEndpoint productMilestoneEndpoint = restEndPointFactory.createRestEndpoint(ProductMilestoneEndpoint.class);
        ProductMilestoneRest productMilestone = productMilestoneEndpoint.getEntity(productMilestoneId).getContent();

        return new HashSet<>();
    }

    static class RestEndPointFactory {
        private final ResteasyClient client;
        private final CausewayConfig config;

        public RestEndPointFactory(CausewayConfig config, ResteasyClient client) {
            this.config = config;
            this.client = client;
        }

        public <T> T createRestEndpoint(Class<T> aClass) {
            ResteasyWebTarget target = client.target(config.getPnclURL());
            return target.proxy(aClass);
        }
    }


    public PncBuild findBuild(Long buildId) {
        return null;
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


    @Path("/product-releases")
    public interface ProductReleaseEndpoint { //FIXME remove when resolve https://projects.engineering.redhat.com/browse/NCL-1645

        @GET
        @Path("/{id}")
        public Singleton<ProductReleaseRest> getEntity(@PathParam("id") int productReleaseId);
    }

    @Path("/product-milestones")
    public interface ProductMilestoneEndpoint { //FIXME remove when resolve https://projects.engineering.redhat.com/browse/NCL-1645

        @GET
        @Path("/{id}")
        public Singleton<ProductMilestoneRest> getEntity(@PathParam("id") int productMilestoneId);
    }
}
