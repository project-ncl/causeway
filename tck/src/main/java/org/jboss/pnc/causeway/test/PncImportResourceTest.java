package org.jboss.pnc.causeway.test;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.jboss.pnc.causeway.test.util.HttpCommandResult;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 2/11/16.
 */
public class PncImportResourceTest
    extends AbstractTest
{
    @Test
    public void checkTestMethod()
            throws Exception
    {
        withNewHttpClient( (driver, client)->{
            String var = Long.toHexString( new Date().getTime() );

            try
            {
                String url = formatUrl( "/import/test", var );
                HttpGet request = new HttpGet( url );

                HttpResponse response = client.execute( request );
                assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );

                String result = IOUtils.toString( response.getEntity().getContent() ).trim();
                assertThat( result, equalTo( var ) );
            }
            catch ( Exception e )
            {
                return new HttpCommandResult( e );
            }

            return new HttpCommandResult();
        });
    }
}
