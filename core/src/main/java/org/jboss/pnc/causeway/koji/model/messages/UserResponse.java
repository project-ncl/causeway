package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.IndexRefs;
import org.commonjava.rwx.binding.anno.Response;
import org.jboss.pnc.causeway.koji.model.KojiUserInfo;

/**
 * Created by jdcasey on 12/3/15.
 */
@Response
public class UserResponse
{
    @DataIndex( 0 )
    private KojiUserInfo userInfo;

    @IndexRefs( { 0 } )
    public UserResponse( KojiUserInfo userInfo )
    {
        this.userInfo = userInfo;
    }

    public KojiUserInfo getUserInfo()
    {
        return userInfo;
    }
}
