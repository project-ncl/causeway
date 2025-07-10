/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class BrewNVR {

    private final String name;
    private final String version;
    private final String release;

    public BrewNVR(String name, String version, String release) {
        this.name = name;
        this.version = version.replace('-', '_');
        this.release = release.replace('-', '_');
    }

    public String getKojiName() {
        return name.replace(':', '-');
    }

    @JsonIgnore
    public String getNVR() {
        return getKojiName() + "-" + version + "-" + release;
    }
}
