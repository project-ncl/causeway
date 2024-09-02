/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.jboss.pnc.common.http.PNCHttpClientConfig;

import java.net.URL;
import java.util.Set;

@ConfigMapping(prefix = "causeway")
public interface CausewayConfig {

    Koji koji();

    PNC pnc();

    PNCHttpClientConfig httpClientConfig();

    interface Koji {

        String url();

        String clientKeyCertificateFile(); // was koji.client.pem.file

        String clientCertificatePassword(); // was koji.client.pem.password

        // String serverCertificateFile(); // was koji.server.pem.file

        @WithDefault("false")
        Boolean trustSelfSigned(); // was koji.trust.self-signed

        @WithDefault("900")
        Integer timeout(); // was koji.timeout.secs

        @WithDefault("1200")
        Integer connectionPoolTimeout(); // was koji.connectionPoolTimeout.secs

        @WithDefault("10")
        Integer connections(); // was koji.connections

        String webURL(); // was koji.weburl
    }

    interface PNC {
        String buildsURL(); // was pncl.url

        String systemVersion(); // was pnc.system.version

        @WithDefault("[]")
        Set<String> ignoredTools(); // pnc.tools.ignored

        URL url();

        @WithDefault("200")
        int pageSize();
    }

}
