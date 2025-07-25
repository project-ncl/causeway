/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.pncclient;

import java.util.ArrayList;
import java.util.List;

import org.jboss.pnc.dto.ArtifactRef;

import lombok.Getter;

@Getter
public final class BuildArtifacts {

    private final List<ArtifactRef> buildArtifacts;
    private final List<ArtifactRef> dependencies;

    public BuildArtifacts() {
        buildArtifacts = new ArrayList<>();
        dependencies = new ArrayList<>();
    }

}
