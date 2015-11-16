package org.jboss.pnc.causeway.koji;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcCommonsTransport;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.client.XmlRpcHttpClientConfig;
import org.apache.xmlrpc.client.XmlRpcHttpTransport;
import org.apache.xmlrpc.client.XmlRpcHttpTransportException;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.util.HttpUtil;
import org.apache.xmlrpc.util.XmlRpcIOException;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 11/13/15.
 */
public class JHttpCTransport
        extends XmlRpcHttpTransport
{
    private static final int MAX_REDIRECT_ATTEMPTS = 100;

    private static final String CAUSEWAY_USER_AGENT = "Causeway/1.0/Java " + System.getProperty( "java.version" );

    private XmlRpcHttpClientConfig config;

    private HttpPost request;

    private HttpResponse response;

    private SiteConfig siteConfig;

    private CloseableHttpClient httpClient;

    public JHttpCTransport( XmlRpcClient client, SiteConfig siteConfig, CloseableHttpClient httpClient )
    {
        super( client, CAUSEWAY_USER_AGENT );
        this.siteConfig = siteConfig;
        this.httpClient = httpClient;
        this.request = new HttpPost( siteConfig.getUri() );
    }

    @Override
    protected void setContentLength( int pLength )
    {
        // Not needed, it'll be handled in the writeRequest() method.
    }

    protected void setRequestHeader( String key, String value )
    {
        request.addHeader( key, value );
    }

    protected InputStream getInputStream()
            throws XmlRpcException
    {
        try
        {
            checkStatus();

            //FIXME: Do we need to wrap this so the client gets closed with the stream??
            return response.getEntity().getContent();
        }
        catch ( IOException e )
        {
            throw new XmlRpcClientException( "I/O error in server communication: " + e.getMessage(), e );
        }
    }

    protected void close()
            throws XmlRpcClientException
    {
        try
        {
            httpClient.close();
        }
        catch ( IOException e )
        {
            throw new XmlRpcClientException( "Failed to close HTTP client: " + e.getMessage(), e );
        }
    }

    @Override
    protected boolean isResponseGzipCompressed( XmlRpcStreamRequestConfig xmlRpcStreamRequestConfig )
    {
        // TODO: Handle gzip compression
        return false;
    }

    protected void writeRequest( final ReqWriter pWriter )
            throws XmlRpcException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            pWriter.write( baos );
        }
        catch ( IOException e )
        {
            throw new XmlRpcClientException( "Failed to write request to byte-array stream: " + e.getMessage(), e );
        }
        catch ( SAXException e )
        {
            throw new XmlRpcClientException( "Failed to render request to byte-array stream: " + e.getMessage(), e );
        }

        byte[] data = baos.toByteArray();
        request.setEntity(
                new InputStreamEntity( new ByteArrayInputStream( data ), data.length, ContentType.TEXT_XML ) );

        try
        {
            response = httpClient.execute( request );
        }
        catch ( IOException e )
        {
            throw new XmlRpcException( "I/O error while communicating with HTTP server: " + e.getMessage(), e );
        }
    }

    private void checkStatus()
            throws XmlRpcHttpTransportException
    {
        if ( response == null )
        {
            throw new IllegalStateException( "Request has not been executed!" );
        }

        int status = response.getStatusLine().getStatusCode();
        if ( status < 200 || status > 299 )
        {
            throw new XmlRpcHttpTransportException( status, response.getStatusLine().getReasonPhrase() );
        }
    }
}
