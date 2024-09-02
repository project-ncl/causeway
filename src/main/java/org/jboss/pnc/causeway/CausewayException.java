/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway;

/**
 * This class indicates an error, which signifies problem in this or related services.
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class CausewayException extends RuntimeException {

    public CausewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public CausewayException(String message) {
        super(message);
    }

}
