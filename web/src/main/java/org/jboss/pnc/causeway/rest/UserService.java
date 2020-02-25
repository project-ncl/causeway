package org.jboss.pnc.causeway.rest;

import org.jboss.pnc.causeway.CausewayException;
import org.keycloak.KeycloakSecurityContext;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
class UserService {

    @Inject
    private HttpServletRequest sr;

    public String getUsername() {
        KeycloakSecurityContext ksc = (KeycloakSecurityContext) sr
                .getAttribute(KeycloakSecurityContext.class.getName());

        if (ksc == null) {
            throw new IllegalStateException("No user information. Is user logged in?");
        }

        return ksc.getToken().getPreferredUsername();
    }
}
