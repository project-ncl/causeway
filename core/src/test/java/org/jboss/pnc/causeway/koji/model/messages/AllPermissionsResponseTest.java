package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.estream.model.Event;
import org.commonjava.rwx.impl.estream.EventStreamGeneratorImpl;
import org.commonjava.rwx.impl.estream.EventStreamParserImpl;
import org.jboss.pnc.causeway.koji.model.KojiPermission;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jdcasey on 12/3/15.
 */
public class AllPermissionsResponseTest
        extends AbstractKojiMessageTest
{

    @Test
    public void verifyVsCapturedHttpRequest()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, newResponse() );

        List<Event<?>> objectEvents = eventParser.getEvents();
        eventParser.clearEvents();

        List<Event<?>> capturedEvents = parseEvents( "getAllPerms-response.xml" );

        assertEquals( objectEvents, capturedEvents );
    }

    private AllPermissionsResponse newResponse()
    {
        return new AllPermissionsResponse( new HashSet<>(
                Arrays.asList( new KojiPermission( 1, "admin" ), new KojiPermission( 2, "build" ),
                               new KojiPermission( 3, "repo" ) ) ) );
    }

    @Test
    public void roundTrip()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, newResponse() );

        List<Event<?>> objectEvents = eventParser.getEvents();
        EventStreamGeneratorImpl generator = new EventStreamGeneratorImpl( objectEvents );

        AllPermissionsRequest parsed = bindery.parse( generator, AllPermissionsRequest.class );
        assertNotNull( parsed );
    }
}
