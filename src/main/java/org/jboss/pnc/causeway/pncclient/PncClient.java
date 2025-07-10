/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.pncclient;

import java.io.InputStream;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;
import org.jboss.pnc.dto.Build;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface PncClient {

    BuildArtifacts findBuildArtifacts(String buildId) throws CausewayException;

    BurnAfterReadingFile getBuildLog(String buildId) throws CausewayException;

    BurnAfterReadingFile getAlignLog(String buildId) throws CausewayException;

    InputStream getSources(String id) throws CausewayException;

    Build findBuild(String buildId);

}
