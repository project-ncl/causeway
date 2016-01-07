package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.binding.anno.Contains;
import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.IndexRefs;
import org.commonjava.rwx.binding.anno.Response;
import org.jboss.pnc.causeway.koji.model.KojiPermission;

import java.util.Set;

/**
 * Created by jdcasey on 1/6/16.
 */
@Response
public class AllPermissionsResponse
{
    @DataIndex( 0 )
    @Contains( KojiPermission.class )
    private Set<KojiPermission> permissions;

    @IndexRefs( 0 )
    public AllPermissionsResponse( Set<KojiPermission> permissions )
    {
        this.permissions = permissions;
    }

    public Set<KojiPermission> getPermissions()
    {
        return permissions;
    }
}
