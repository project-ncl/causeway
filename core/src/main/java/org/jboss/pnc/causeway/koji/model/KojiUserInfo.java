package org.jboss.pnc.causeway.koji.model;

import org.commonjava.rwx.binding.anno.DataKey;
import org.commonjava.rwx.binding.anno.KeyRefs;
import org.commonjava.rwx.binding.anno.StructPart;

/**
 * Created by jdcasey on 12/3/15.
 */
@StructPart
public class KojiUserInfo
{
    @DataKey( "status" )
    private int status;

    @DataKey( "usertype" )
    private int userType;

    @DataKey( "id" )
    private int userId;

    @DataKey( "name" )
    private String userName;

    @KeyRefs( { "status", "usertype", "id", "name" } )
    public KojiUserInfo( int status, int userType, int userId, String userName )
    {
        this.status = status;
        this.userType = userType;
        this.userId = userId;
        this.userName = userName;
    }

    public int getStatus()
    {
        return status;
    }

    public int getUserType()
    {
        return userType;
    }

    public int getUserId()
    {
        return userId;
    }

    public String getUserName()
    {
        return userName;
    }
}
