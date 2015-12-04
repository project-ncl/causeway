package org.jboss.pnc.causeway.koji.model;

import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.IndexRefs;
import org.commonjava.rwx.binding.anno.Response;

/**
 * Created by jdcasey on 11/17/15.
 */
@Response
public class LogoutResponse
{
    @DataIndex( 0 )
    private String altUsername;

    public String getAltUsername()
    {
        return altUsername;
    }

    public void setAltUsername( String altUsername )
    {
        this.altUsername = altUsername;
    }
}
