import com.redhat.red.build.koji.model.xmlrpc.KojiXmlRpcBindery;
import com.redhat.red.build.koji.model.xmlrpc.messages.LoginResponse;
import org.commonjava.rwx.error.XmlRpcException;
import org.commonjava.rwx.estream.model.Event;
import org.commonjava.rwx.impl.estream.EventStreamParserImpl;
import org.commonjava.rwx.impl.stax.StaxParser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

/**
 * mstodo: remove
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 9/28/16
 * Time: 3:28 PM
 */
public class LoginResponseTest {

    protected static final String MESSAGES_BASE = "messages/";

    protected static KojiXmlRpcBindery bindery;

    protected EventStreamParserImpl eventParser;

    @BeforeClass
    public static void setupClass()
            throws Exception
    {
        bindery = new KojiXmlRpcBindery();
    }

    @org.junit.Before
    public void setup()
            throws Exception
    {
        eventParser = new EventStreamParserImpl();
    }

    protected List<Event<?>> parseEvents(String resourceFile )
            throws Exception
    {
        String resource = MESSAGES_BASE + resourceFile;
        try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ))
        {
            if ( is == null )
            {
                Assert.fail( "Cannot find message XML file on classpath: " + resource );
            }

            eventParser.clearEvents();

            StaxParser parser = new StaxParser( is );
            parser.parse( eventParser );

            List<Event<?>> events = eventParser.getEvents();

            return events;
        }
        finally
        {
            eventParser.clearEvents();
        }
    }

    @Test
    public void shouldParse() throws XmlRpcException {
        String responseAsString = "<?xml version='1.0'?>\n" +
                "<methodResponse>\n" +
                "<params>\n" +
                "<param>\n" +
                "<value><struct>\n" +
                "<member>\n" +
                "<name>session-id</name>\n" +
                "<value><int>15468078</int></value>\n" +
                "</member>\n" +
                "<member>\n" +
                "<name>session-key</name>\n" +
                "<value><string>3489-7wSOLpIaVL2CvWtCJuy</string></value>\n" +
                "</member>\n" +
                "</struct></value>\n" +
                "</param>\n" +
                "</params>\n" +
                "</methodResponse>";
        LoginResponse response = bindery.parse(responseAsString, LoginResponse.class);

        System.out.println(response);
    }

}