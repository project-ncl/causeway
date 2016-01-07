package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.IndexRefs;
import org.commonjava.rwx.binding.anno.Response;
import org.commonjava.rwx.binding.anno.StructPart;
import org.jboss.pnc.causeway.koji.model.KojiSessionInfo;

/**
 * Created by jdcasey on 11/17/15.
 */
@Response
public class LoginResponse
{
    @DataIndex( 0 )
    private KojiSessionInfo sessionInfo;

    @IndexRefs( {0} )
    public LoginResponse( KojiSessionInfo sessionInfo )
    {
        this.sessionInfo = sessionInfo;
    }

    public KojiSessionInfo getSessionInfo()
    {
        return sessionInfo;
    }
}
