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

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.model.BuildRecordRest;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.model.Build;

import com.redhat.red.build.koji.model.json.KojiImport;
import org.jboss.pnc.causeway.rest.model.MavenBuiltArtifact;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface BuildTranslator {

    @Deprecated
    ImportFileGenerator getImportFiles(BuildArtifacts build, String log) throws CausewayException;

    @Deprecated
    KojiImport translate(BrewNVR nvr, BuildRecordRest build, BuildArtifacts artifacts, String log, String username) throws CausewayException;

    public ImportFileGenerator getImportFiles(Build build) throws CausewayException;

    public KojiImport translate(BrewNVR nvr, Build build, String username) throws CausewayException;

    public static String guessVersion(Build build) throws CausewayException {
        return build.getBuiltArtifacts().stream()
                .filter(a -> a instanceof MavenBuiltArtifact)
                .map(a -> ((MavenBuiltArtifact)a).getVersion())
                .filter(v -> v != null)
                .findAny()
                .orElseThrow(() -> new CausewayException("Build version not specified and couldn't determine any from artifacts."));
    }

    @Deprecated
    public static String guessVersion(BuildRecordRest build, BuildArtifacts artifacts) throws CausewayException {
        return artifacts.buildArtifacts.stream()
                .map(a -> a.identifier.split(":"))
                .filter(i -> i.length >= 4)
                .map(i -> i[3])
                .findAny()
                .orElseThrow(() -> new CausewayException("Build version not specified and couldn't determine any from artifacts."));
    }
}
