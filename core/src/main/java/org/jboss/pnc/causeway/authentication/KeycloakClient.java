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
package org.jboss.pnc.causeway.authentication;

import org.apache.http.impl.client.HttpClients;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;

/**
 * Helper object to obtain a fresh access token from Keycloak
 */
@ApplicationScoped
public class KeycloakClient {

    @Inject
    CausewayConfig config;

    /**
     * Get a fresh access token from the OIDC server
     * 
     * @return access token
     * @throws KeycloakClientException if there is an issue with the configuration
     */
    public String getAccessToken() throws KeycloakClientException {
        try {
            final Configuration configuration = new Configuration(
                    config.getOidcClientUrl(),
                    config.getOidcClientRealm(),
                    config.getOidcClientClientId(),
                    Collections.singletonMap("secret", config.getOidcClientSecret()),
                    HttpClients.createDefault());

            return AuthzClient.create(configuration).obtainAccessToken().getToken();
        } catch (IOException e) {
            throw new KeycloakClientException("Exception trying to obtain an auth token", e);
        }
    }
}
