package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.estream.model.Event;
import org.commonjava.rwx.impl.estream.EventStreamGeneratorImpl;
import org.commonjava.rwx.impl.estream.EventStreamParserImpl;
import org.commonjava.rwx.impl.stax.StaxParser;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jdcasey on 12/3/15.
 */
public class ApiVersionResponseTest
        extends AbstractKojiMessageTest
{

    @Test
    public void verifyVsCapturedHttpRequest()
            throws Exception
    {
        bindery.render( eventParser, new ApiVersionResponse( 1 ) );

        List<Event<?>> objectEvents = eventParser.getEvents();
        eventParser.clearEvents();

        List<Event<?>> xmlEvents = parseEvents( "getApiVersion-response.xml" );
        assertEquals( objectEvents, xmlEvents );
    }

    @Test
    public void roundTrip()
            throws Exception
    {
        bindery.render( eventParser, new ApiVersionResponse( 1 ) );

        List<Event<?>> objectEvents = eventParser.getEvents();
        EventStreamGeneratorImpl generator = new EventStreamGeneratorImpl( objectEvents );

        ApiVersionResponse parsed = bindery.parse( generator, ApiVersionResponse.class );
        assertNotNull( parsed );
    }
}
