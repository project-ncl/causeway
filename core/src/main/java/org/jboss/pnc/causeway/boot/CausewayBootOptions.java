/**
 * Copyright (C) 2015 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.jboss.pnc.causeway.boot;

import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.deploy.undertow.UndertowBootOptions;
import org.kohsuke.args4j.Option;

import java.io.File;

/**
 * Created by jdcasey on 11/10/15.
 */
public class CausewayBootOptions
        extends BootOptions
        implements UndertowBootOptions
{
    public static final String CAUSEWAY_CONFIG_SYSPROP = "causeway.config";

    private static final String CAUSEWAY_HOME_SYSPROP = "causeway.home";

    public static final String DEFAULT_CONTEXT_PATH = "/";

    public static final int DEFAULT_PORT = 8080;

    public static final String DEFAULT_BIND = "0.0.0.0";

    @Option( name = "-C", aliases = { "--context" }, usage = "Specify the context path (default: '/')")
    private String contextPath;

    @Option(name = "-p", aliases = { "--port" }, usage = "Specify the HTTP port (default: 8080)")
    private Integer port;

    @Option(name = "-b", aliases = { "--bind" }, usage = "Specify the network interface to bind to (default: all or '0.0.0.0')")
    private String bind;

    @Override
    public String getApplicationName()
    {
        return "Causeway Newcastle-Koji Bridge";
    }

    @Override
    public String getHomeSystemProperty()
    {
        return CAUSEWAY_HOME_SYSPROP;
    }

    @Override
    public String getConfigSystemProperty()
    {
        return CAUSEWAY_CONFIG_SYSPROP;
    }

    @Override
    public String getHomeEnvar()
    {
        return "CAUSEWAY_HOME";
    }

    public String getConfig()
    {
        String config = getSpecifiedConfig();
        return config == null ? new File( getHomeDir(), "etc/main.conf" ).getPath() : config;
    }

    @Override
    public String getContextPath()
    {
        return contextPath == null ? DEFAULT_CONTEXT_PATH : contextPath;
    }

    @Override
    public String getDeploymentName()
    {
        return getApplicationName();
    }

    @Override
    public int getPort()
    {
        return port == null ? DEFAULT_PORT : port;
    }

    @Override
    public String getBind()
    {
        return bind == null ? DEFAULT_BIND : bind;
    }

    public void setContextPath( String contextPath )
    {
        this.contextPath = contextPath;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    public void setBind( String bind )
    {
        this.bind = bind;
    }
}
