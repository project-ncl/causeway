package org.jboss.pnc.causeway.koji.model;

import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.IndexRefs;
import org.commonjava.rwx.binding.anno.Response;

/**
 * Created by jdcasey on 12/3/15.
 */
@Response
public class LoggedInUserResponse
{
    @DataIndex( 0 )
    private KojiUserInfo userInfo;

    @IndexRefs( { 0 } )
    public LoggedInUserResponse( KojiUserInfo userInfo )
    {
        this.userInfo = userInfo;
    }

    public KojiUserInfo getUserInfo()
    {
        return userInfo;
    }
}
