/**
 * Copyright (C) 2015 Red Hat, Inc. (jbrazdil@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.test;

import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.jboss.pnc.causeway.test.spi.CausewayDriver;
import org.jboss.pnc.causeway.test.util.HttpCommands;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ServiceLoader;

/**
 * Created by jdcasey on 2/11/16.
 */
public class AbstractTest
{
    private static CausewayDriver driver;

    @BeforeClass
    public static void startup()
            throws Exception
    {
        ServiceLoader<CausewayDriver> loader = ServiceLoader.load( CausewayDriver.class );
        driver = loader.iterator().next();

        driver.start();
    }

    @AfterClass
    public static void shutdown()
            throws Exception
    {
        if ( driver != null )
        {
            driver.stop();
        }
    }

    protected int getPort()
    {
        return driver.getPort();
    }

    protected String formatUrl( String... pathParts )
            throws MalformedURLException
    {
        return driver.formatUrl( pathParts );
    }

    protected HttpFactory getHttpFactory()
            throws Exception
    {
        return driver.getHttpFactory();
    }

    protected SiteConfig getSiteConfig()
            throws Exception
    {
        return driver.getSiteConfig();
    }

    protected PasswordManager getPasswordManager()
            throws Exception
    {
        return driver.getPasswordManager();
    }

    protected void withNewHttpClient( HttpCommands commands )
            throws Exception
    {
        driver.withNewHttpClient( commands );
    }
}
