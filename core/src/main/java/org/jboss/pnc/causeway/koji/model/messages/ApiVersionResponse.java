package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.IndexRefs;
import org.commonjava.rwx.binding.anno.Request;
import org.commonjava.rwx.binding.anno.Response;

/**
 * Created by jdcasey on 12/3/15.
 */
@Response
public class ApiVersionResponse
{
    @DataIndex( 0 )
    private int apiVersion;

    @IndexRefs( { 0 } )
    public ApiVersionResponse( int apiVersion )
    {
        this.apiVersion = apiVersion;
    }

    public int getApiVersion()
    {
        return apiVersion;
    }
}
