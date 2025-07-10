/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import java.io.InputStream;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.source.RenamedSources;
import org.jboss.pnc.dto.ArtifactRef;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.BuildType;
import org.jboss.pnc.restclient.util.ArtifactUtil;

import com.redhat.red.build.koji.model.json.KojiImport;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface BuildTranslator {

    ImportFileGenerator getImportFiles(
            BuildArtifacts artifacts,
            BurnAfterReadingFile sources,
            BurnAfterReadingFile buildLog,
            BurnAfterReadingFile alignLog) throws CausewayException;

    KojiImport translate(
            BrewNVR nvr,
            Build build,
            BuildArtifacts artifacts,
            RenamedSources sources,
            BurnAfterReadingFile buildLog,
            BurnAfterReadingFile alignLog,
            String username) throws CausewayException;

    RenamedSources getSources(Build build, BuildArtifacts artifacts, InputStream sourcesStream)
            throws CausewayException;

    String getSourcesDeployPath(Build build, BuildArtifacts artifacts) throws CausewayException;

    static String guessVersion(Build build, BuildArtifacts artifacts) throws CausewayException {
        BuildType buildType = build.getBuildConfigRevision().getBuildType();

        return artifacts.getBuildArtifacts()
                .stream()
                .map(artifact -> extractVersion(buildType, artifact))
                .findAny()
                .orElseThrow(
                        () -> new CausewayException(
                                "Build version or BuildType (MVN,NPM...) not specified and couldn't determine any from artifacts."));
    }

    private static String extractVersion(BuildType buildType, ArtifactRef artifact) {
        return switch (buildType) {
            case MVN, SBT, GRADLE -> ArtifactUtil.parseMavenCoordinates(artifact).getVersionString();
            case NPM -> ArtifactUtil.parseNPMCoordinates(artifact).getVersion().toString();
        };
    }

}
