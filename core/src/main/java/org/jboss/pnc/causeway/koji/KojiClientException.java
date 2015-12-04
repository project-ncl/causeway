package org.jboss.pnc.causeway.koji;

import org.commonjava.rwx.error.XmlRpcException;
import org.jboss.pnc.causeway.CausewayException;

/**
 * Created by jdcasey on 12/3/15.
 */
public class KojiClientException
        extends CausewayException
{
    public KojiClientException( String format, Throwable cause, Object... params )
    {
        super( format, cause, params );
    }

    public KojiClientException( String format, Object... params )
    {
        super( format, params );
    }

}
