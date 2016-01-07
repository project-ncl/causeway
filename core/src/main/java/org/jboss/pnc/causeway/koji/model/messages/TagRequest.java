package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.binding.anno.DataIndex;
import org.commonjava.rwx.binding.anno.IndexRefs;
import org.commonjava.rwx.binding.anno.Request;

/**
 * Created by jdcasey on 1/6/16.
 */
@Request( method="getTag" )
public class TagRequest
{
    @DataIndex( 0 )
    private String tagName;

    @IndexRefs( 0 )
    public TagRequest( String tagName )
    {
        this.tagName = tagName;
    }

    public String getTagName()
    {
        return tagName;
    }
}
