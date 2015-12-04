package org.jboss.pnc.causeway.koji.model;

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
public class LoginResponseTest
    extends AbstractKojiModelTest
{

    private static final String CAPTURED_XML = "<?xml version='1.0'?>\n"
            + "<methodResponse><params><param><value><struct><member><name>session-id</name><value><int>12716309</int></value></member><member><name>session-key</name><value><string>2982-CTP0Zv6YcYqRAF1uLKs</string></value></member></struct></value></param></params></methodResponse>";

    @Test
    public void verifyVsCapturedHttpRequest()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, new LoginResponse( new KojiSessionInfo( 12716309, "2982-CTP0Zv6YcYqRAF1uLKs" ) ) );

        List<Event<?>> objectEvents = eventParser.getEvents();
        eventParser.clearEvents();

        StaxParser parser = new StaxParser(CAPTURED_XML);
        parser.parse(eventParser);

        List<Event<?>> capturedEvents = eventParser.getEvents();
        eventParser.clearEvents();

        assertEquals( objectEvents, capturedEvents );
    }

    @Test
    public void roundTrip()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, new LoginResponse( new KojiSessionInfo( 12716309, "2982-CTP0Zv6YcYqRAF1uLKs" ) ) );

        List<Event<?>> objectEvents = eventParser.getEvents();
        EventStreamGeneratorImpl generator = new EventStreamGeneratorImpl( objectEvents );

        LoginResponse parsed = bindery.parse( generator, LoginResponse.class );
        assertNotNull( parsed );
    }
}
