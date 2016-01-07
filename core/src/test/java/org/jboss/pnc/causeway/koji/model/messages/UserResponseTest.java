package org.jboss.pnc.causeway.koji.model.messages;

import org.commonjava.rwx.estream.model.Event;
import org.commonjava.rwx.impl.estream.EventStreamGeneratorImpl;
import org.commonjava.rwx.impl.estream.EventStreamParserImpl;
import org.commonjava.rwx.impl.stax.StaxParser;
import org.jboss.pnc.causeway.koji.model.KojiUserInfo;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jdcasey on 12/3/15.
 */
public class UserResponseTest
        extends AbstractKojiMessageTest
{

    private static final String USER_NAME = "newcastle";

    private static final int USER_ID = 2982;

    private static final int STATUS = 0;

    private static final int USER_TYPE = 0;

    private static final String KRB_PRINCIPAL = "me@MYCO.COM";

    @Test
    public void verifyVsCapturedHttpRequest()
            throws Exception
    {
        bindery.render( eventParser, newResponse( true ) );

        List<Event<?>> objectEvents = eventParser.getEvents();
        List<Event<?>> xmlEvents = parseEvents( "user-krbPrincipal-response.xml" );
        assertEquals( objectEvents, xmlEvents );
    }

    @Test
    public void verifyVsCapturedHttpRequest_NilKerberosPrincipal()
            throws Exception
    {
        bindery.render( eventParser, newResponse( false ) );

        List<Event<?>> objectEvents = eventParser.getEvents();
        List<Event<?>> xmlEvents = parseEvents( "user-krbPrincipal-response.xml" );
        assertEquals( objectEvents, xmlEvents );
    }

    private UserResponse newResponse( boolean enableKerberosPrincipal )
    {
        return new UserResponse( new KojiUserInfo( STATUS, USER_TYPE, USER_ID, USER_NAME,
                                                   enableKerberosPrincipal ? KRB_PRINCIPAL : null ) );
    }

    @Test
    public void roundTrip()
            throws Exception
    {
        EventStreamParserImpl eventParser = new EventStreamParserImpl();
        bindery.render( eventParser, newResponse( true ) );

        List<Event<?>> objectEvents = eventParser.getEvents();
        EventStreamGeneratorImpl generator = new EventStreamGeneratorImpl( objectEvents );

        UserResponse parsed = bindery.parse( generator, UserResponse.class );
        assertNotNull( parsed );
    }
}
