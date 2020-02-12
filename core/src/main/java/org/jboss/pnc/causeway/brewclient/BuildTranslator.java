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
package org.jboss.pnc.causeway.brewclient;

import com.redhat.red.build.koji.model.json.KojiImport;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.model.Build;
import org.jboss.pnc.causeway.rest.model.BuiltArtifact;
import org.jboss.pnc.causeway.rest.model.MavenBuild;
import org.jboss.pnc.causeway.rest.model.MavenBuiltArtifact;
import org.jboss.pnc.causeway.rest.model.NpmBuild;
import org.jboss.pnc.causeway.rest.model.NpmBuiltArtifact;
import org.jboss.pnc.enums.BuildType;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface BuildTranslator {

    @Deprecated
    ImportFileGenerator getImportFiles(BuildArtifacts build, String log) throws CausewayException;

    @Deprecated
    KojiImport translate(BrewNVR nvr, org.jboss.pnc.dto.Build build, BuildArtifacts artifacts, String log, String username)
            throws CausewayException;

    public ImportFileGenerator getImportFiles(Build build) throws CausewayException;

    public KojiImport translate(BrewNVR nvr, Build build, String username) throws CausewayException;

    public static String guessVersion(Build build) throws CausewayException {
        final Predicate<BuiltArtifact> filter;
        final Function<BuiltArtifact, String> getVersion;
        if (build instanceof MavenBuild) {
            filter = (artifact -> artifact instanceof MavenBuiltArtifact);
            getVersion = (artifact -> ((MavenBuiltArtifact) artifact).getVersion());
        } else if (build instanceof NpmBuild) {
            filter = (artifact -> artifact instanceof NpmBuiltArtifact);
            getVersion = (artifact -> ((NpmBuiltArtifact) artifact).getVersion());
        } else {
            filter = (artifact -> false);
            getVersion = (artifact -> null);
        }

        return build.getBuiltArtifacts()
                    .stream()
                    .filter(filter)
                    .map(getVersion)
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElseThrow(() -> new CausewayException(
                            "Build version or BuildType (MVN,NPM...) not specified and couldn't determine any from artifacts."));
    }

    @Deprecated
    public static String guessVersion(org.jboss.pnc.dto.Build build, BuildArtifacts artifacts) throws CausewayException {
        String delim = ":";
        BuildType buildType = build.getBuildConfigRevision().getBuildType();

        // Maven and Gradle artifacts identifiers have 4 parts (G:A:P:V = org.jboss.pnc.causeway:causeway-web:war:2.0.0) and Npm
        // 2 (N:V = async:3.1.0)
        // Last part for each is the version.
        final int parts;
        switch (buildType) {
            case MVN:
            case GRADLE:
                parts = 4;
                break;
            case NPM:
                parts = 2;
                break;
            default:
                parts = 0;
                break;
        }

        return artifacts.buildArtifacts.stream()
                                       .map(artifact -> artifact.identifier.split(delim))
                                       .filter(i -> i.length >= parts)
                                       .map(i -> i[parts - 1])
                                       .findAny()
                                       .orElseThrow(() -> new CausewayException(
                                               "Build version or BuildType (MVN,NPM...) not specified and couldn't determine any from artifacts."));
    }
}
