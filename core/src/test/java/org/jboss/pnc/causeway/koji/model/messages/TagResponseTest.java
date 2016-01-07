package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.estream.model.Event;
import org.commonjava.rwx.impl.estream.EventStreamGeneratorImpl;
import org.commonjava.rwx.impl.estream.EventStreamParserImpl;
import org.jboss.pnc.causeway.koji.model.KojiTagInfo;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 12/3/15.
 */
public class TagResponseTest
        extends AbstractKojiMessageTest
{

    private static final String TAG = "test-tag";

    @Test
    public void verifyVsCapturedHttpRequest()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, newResponse() );

        List<Event<?>> objectEvents = eventParser.getEvents();
        eventParser.clearEvents();

        List<Event<?>> capturedEvents = parseEvents( "getTag-response.xml" );

        assertEquals( objectEvents, capturedEvents );
    }

    @Test
    public void roundTrip()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, newResponse() );

        List<Event<?>> objectEvents = eventParser.getEvents();
        EventStreamGeneratorImpl generator = new EventStreamGeneratorImpl( objectEvents );

        TagResponse parsed = bindery.parse( generator, TagResponse.class );
        assertNotNull( parsed );

        KojiTagInfo tagInfo = parsed.getTagInfo();

        assertThat( tagInfo.getName(), equalTo( TAG ) );
    }

    private TagResponse newResponse()
    {
        return new TagResponse( new KojiTagInfo( 1001, "test-tag", "admin", 1, "x86_64", true, true, true ) );
    }
}
