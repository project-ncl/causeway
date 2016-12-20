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
