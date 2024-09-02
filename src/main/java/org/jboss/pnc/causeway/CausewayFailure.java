/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway;

/**
 * This class indicates a failure, which can usually be solved by user.
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class CausewayFailure extends CausewayException {

    public CausewayFailure(String format, Throwable cause) {
        super(format, cause);
    }

    public CausewayFailure(String message) {
        super(message);
    }

}
