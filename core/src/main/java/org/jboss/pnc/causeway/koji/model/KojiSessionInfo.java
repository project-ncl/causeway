package org.jboss.pnc.causeway.koji.model;

import org.commonjava.rwx.binding.anno.DataKey;
import org.commonjava.rwx.binding.anno.KeyRefs;
import org.commonjava.rwx.binding.anno.StructPart;

/**
 * Created by jdcasey on 12/3/15.
 */
@StructPart
public class KojiSessionInfo
{
    @DataKey( "session-id" )
    private int sessionId;

    @DataKey( "session-key" )
    private String sessionKey;

    @KeyRefs( {"session-id", "session-key"} )
    public KojiSessionInfo( int sessionId, String sessionKey )
    {
        this.sessionId = sessionId;
        this.sessionKey = sessionKey;
    }

    public int getSessionId()
    {
        return sessionId;
    }

    public String getSessionKey()
    {
        return sessionKey;
    }

    @Override
    public String toString()
    {
        return "KojiSessionInfo{" +
                "sessionId=" + sessionId +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
