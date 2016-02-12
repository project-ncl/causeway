package org.jboss.pnc.causeway.test.util;

/**
 * Created by jdcasey on 2/11/16.
 */
public class HttpCommandResult
{
    private Exception error;

    public HttpCommandResult()
    {
    }

    public HttpCommandResult( Exception error )
    {
        this.error = error;
    }

    public void throwError()
            throws Exception
    {
        if ( error != null )
        {
            throw error;
        }
    }
}
