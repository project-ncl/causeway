package org.jboss.pnc.causeway.pncclient;

import static org.apache.http.client.utils.HttpClientUtils.closeQuietly;

import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.JHttpCException;
import org.jboss.pnc.causeway.config.CausewayConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;

/**
 * Created by jdcasey on 2/9/16.
 */
@ApplicationScoped
public class PncClient
{
    private static final String BUILD_RECORDS_PER_RELEASE_RESOURCE = "/some/path/to/a/rest/call";

    private final HttpFactory httpFactory;

    private final CausewayConfig config;

    @Inject
    public PncClient(CausewayConfig config, HttpFactory httpFactory )
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
}
