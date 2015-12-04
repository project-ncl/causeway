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
public class LoggedInUserResponseTest
        extends AbstractKojiModelTest
{

    private static final String USER_NAME = "newcastle";

    private static final int USER_ID = 2982;

    private static final int STATUS = 0;

    private static final int USER_TYPE = 0;

    private static final String CAPTURED_XML = "<?xml version='1.0'?>\n"
            + "<methodResponse><params><param><value><struct><member><name>status</name><value><int>" + STATUS
            + "</int></value></member><member><name>usertype</name><value><int>" + USER_TYPE
            + "</int></value></member><member><name>krb_principal</name><value><nil/></value></member><member><name>id</name><value><int>"
            + USER_ID + "</int></value></member><member><name>name</name><value><string>" + USER_NAME
            + "</string></value></member></struct></value></param></params></methodResponse>";

    @Test
    public void verifyVsCapturedHttpRequest()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, newResponse() );

        List<Event<?>> objectEvents = eventParser.getEvents();
        eventParser.clearEvents();

        StaxParser parser = new StaxParser( CAPTURED_XML );
        parser.parse( eventParser );

        List<Event<?>> capturedEvents = eventParser.getEvents();
        eventParser.clearEvents();

        assertEquals( objectEvents, capturedEvents );
    }

    private LoggedInUserResponse newResponse()
    {
        return new LoggedInUserResponse( new KojiUserInfo( STATUS, USER_TYPE, USER_ID, USER_NAME ) );
    }

    @Test
    public void roundTrip()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, newResponse() );

        List<Event<?>> objectEvents = eventParser.getEvents();
        EventStreamGeneratorImpl generator = new EventStreamGeneratorImpl( objectEvents );

        LoggedInUserResponse parsed = bindery.parse( generator, LoggedInUserResponse.class );
        assertNotNull( parsed );
    }
}
