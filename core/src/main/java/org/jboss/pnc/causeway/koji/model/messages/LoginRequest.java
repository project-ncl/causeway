package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.Request;

/**
 * Created by jdcasey on 12/3/15.
 */
@Request( method="sslLogin" )
public class LoginRequest
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
