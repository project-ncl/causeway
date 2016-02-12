package org.jboss.pnc.causeway.test.spi;

import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.jboss.pnc.causeway.test.util.HttpCommands;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jdcasey on 2/11/16.
 */
public interface CausewayDriver
{
    void start() throws Exception;

    void stop() throws Exception;

    int getPort();

    String formatUrl( String...pathParts ) throws MalformedURLException;

    HttpFactory getHttpFactory() throws Exception;

    SiteConfig getSiteConfig() throws Exception;

    PasswordManager getPasswordManager() throws Exception;

    void withNewHttpClient( HttpCommands commands ) throws Exception;
}
