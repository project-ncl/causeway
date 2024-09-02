/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import lombok.Getter;

@Getter
public class BrewBuild extends BrewNVR {

    private final Integer id;

    public BrewBuild(Integer id, String name, String version, String release) {
        super(name, version, release);
        this.id = id;
    }

    public BrewBuild(Integer id, BrewNVR nvr) {
        this(id, nvr.getName(), nvr.getVersion(), nvr.getRelease());
    }

}
