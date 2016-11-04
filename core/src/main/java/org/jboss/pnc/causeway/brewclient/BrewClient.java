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
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.rest.restmodel.causeway.BuildImportResultRest;

import com.redhat.red.build.koji.model.json.KojiImport;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface BrewClient {

    BrewBuild findBrewBuildOfNVR(BrewNVR nvr) throws CausewayException;

    BuildImportResultRest importBuild(BrewNVR nvr, int buildRecordId, KojiImport kojiImport, ImportFileGenerator importFiles) throws CausewayException;

    public String getBuildUrl(int id);

    void tagBuild(String tag, BrewNVR nvr) throws CausewayException;

}
