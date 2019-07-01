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

import java.util.function.Function;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface BuildTranslator {

    @Deprecated
    ImportFileGenerator getImportFiles(BuildArtifacts build, String log) throws CausewayException;

    @Deprecated
    KojiImport translate(BrewNVR nvr, org.jboss.pnc.dto.Build build, BuildArtifacts artifacts, String log, String username) throws CausewayException;

    public ImportFileGenerator getImportFiles(Build build) throws CausewayException;

    public KojiImport translate(BrewNVR nvr, Build build, String username) throws CausewayException;

    public static String guessVersion(Build build) throws CausewayException {
        //If we have Maven builds, this filters out non MavenArtifacts. Same thing for NPM. Default option to not allow any Artifacts.
        Function<BuiltArtifact, Boolean> filter = (build instanceof MavenBuild) ? (artifact -> artifact instanceof MavenBuiltArtifact) :
                ((build instanceof NpmBuild) ? (artifact -> artifact instanceof NpmBuiltArtifact) : (artifact -> false));

        //This function specifies the method to get Version from each type of Artifact. Default option is null.
        Function<BuiltArtifact, String> getVersion = artifact -> (artifact instanceof MavenBuiltArtifact) ? ((MavenBuiltArtifact) artifact).getVersion() :
                ((artifact instanceof  NpmBuiltArtifact) ? ((NpmBuiltArtifact) artifact).getVersion() : null);

        return build.getBuiltArtifacts().stream()
                .filter( artifact -> filter.apply(artifact))
                .map(a -> getVersion.apply(a))
                .filter(v -> v != null)
                .findAny()
                .orElseThrow(() -> new CausewayException("Build version or BuildType (MVN,NPM...) not specified and couldn't determine any from artifacts."));
    }

    @Deprecated
    public static String guessVersion(org.jboss.pnc.dto.Build build, BuildArtifacts artifacts) throws CausewayException {
        String delim = ":";
        BuildType buildType = build.getBuildConfigurationRevision().getBuildType();

        // Maven and Gradle artifacts identifiers have 4 parts (G:A:P:V = org.jboss.pnc.causeway:causeway-web:war:2.0.0) and Npm 2 (N:V = async:3.1.0)
        // Last part for each is the version.
        int parts = (buildType.equals(BuildType.MVN) || buildType.equals(BuildType.GRADLE)) ? 4 :
                ((buildType.equals(BuildType.NPM) ? 2 : 0));

        return artifacts.buildArtifacts.stream()
                .map(a -> a.identifier.split(delim))
                .filter(i -> i.length >= parts)
                .map(i -> i[parts-1])
                .findAny()
                .orElseThrow(() -> new CausewayException("Build version or BuildType (MVN,NPM...) not specified and couldn't determine any from artifacts."));
    }
}
