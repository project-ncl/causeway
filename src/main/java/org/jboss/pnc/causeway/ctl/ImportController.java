/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.ctl;

import org.jboss.pnc.api.causeway.dto.push.PushResult;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface ImportController {

    PushResult importBuild(String buildId, String tagPrefix, boolean reimport, String username);

    void untagBuild(int brewBuildId, String tagPrefix);
}
