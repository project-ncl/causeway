package org.jboss.pnc.causeway.koji.model;

import org.commonjava.rwx.binding.anno.DataKey;
import org.commonjava.rwx.binding.anno.KeyRefs;
import org.commonjava.rwx.binding.anno.StructPart;

/**
 * Created by jdcasey on 1/6/16.
 */
@StructPart
public class KojiPermission
{
    @DataKey( "name" )
    private String name;

    @DataKey( "id" )
    private int id;

    @KeyRefs( {"id", "name"} )
    public KojiPermission( int id, String name )
    {
        this.name = name;
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public int getId()
    {
        return id;
    }
}
