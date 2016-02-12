package org.jboss.pnc.causeway.test.util;

import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.pnc.causeway.test.spi.CausewayDriver;

/**
 * Created by jdcasey on 2/11/16.
 */
public interface HttpCommands
{
    HttpCommandResult execute( CausewayDriver driver, CloseableHttpClient client );
}
