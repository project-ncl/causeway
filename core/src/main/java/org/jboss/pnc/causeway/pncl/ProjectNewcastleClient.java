package org.jboss.pnc.causeway.pncl;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.JHttpCException;
import org.jboss.pnc.causeway.config.CausewayConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.apache.http.client.utils.HttpClientUtils.closeQuietly;

/**
 * Created by jdcasey on 2/9/16.
 */
@ApplicationScoped
public class ProjectNewcastleClient
{
    private static final String BUILD_RECORDS_PER_RELEASE_RESOURCE = "/some/path/to/a/rest/call";

    private HttpFactory httpFactory;

    private CausewayConfig config;

    @Inject
    public ProjectNewcastleClient( CausewayConfig config, HttpFactory httpFactory )
    {
        this.config = config;
        this.httpFactory = httpFactory;
    }

//    public Set<BuildRecord> getBuildRecordIdsForRelease( Integer releaseId )
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
            throws ProjectNewcastleClientException
    {
        CloseableHttpClient client = null;
        try
        {
            client = httpFactory.createClient( config.getPnclSiteConfig() );
            commands.execute( client ).throwError();
        }
        catch ( JHttpCException e )
        {
            throw new ProjectNewcastleClientException( "Failed to create HTTP client to communicate with Project Newcastle.", e );
        }
        catch ( IOException e )
        {
            throw new ProjectNewcastleClientException( "Communication with Project Newcastle server failed.", e );
        }
        finally
        {
            closeQuietly( client );
        }
    }

    public interface ClientCommands
    {
        ClientCommandResult execute( CloseableHttpClient client )
                throws IOException;
    }

    public class ClientCommandResult
    {
        private ProjectNewcastleClientException error;

        public ClientCommandResult( ProjectNewcastleClientException error )
        {
            this.error = error;
        }

        public ClientCommandResult()
        {
        }

        public ClientCommandResult throwError()
                throws ProjectNewcastleClientException
        {
            if ( error != null )
            {
                throw error;
            }

            return this;
        }
    }
}
