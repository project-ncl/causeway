package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.IndexRefs;
import org.commonjava.rwx.binding.anno.Response;
import org.jboss.pnc.causeway.koji.model.KojiTagInfo;

/**
 * Created by jdcasey on 1/6/16.
 */
@Response
public class TagResponse
{
    @DataIndex( 0 )
    private KojiTagInfo tagInfo;

    @IndexRefs( 0 )
    public TagResponse( KojiTagInfo tagInfo )
    {
        this.tagInfo = tagInfo;
    }

    public KojiTagInfo getTagInfo()
    {
        return tagInfo;
    }
}
