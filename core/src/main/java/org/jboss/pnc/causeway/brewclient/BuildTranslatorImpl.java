/*
 * Copyright 2016 Honza Brázdil <jbrazdil@redhat.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.brewclient;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.redhat.red.build.koji.model.json.BuildContainer;
import com.redhat.red.build.koji.model.json.BuildOutput;
import com.redhat.red.build.koji.model.json.BuildRoot;
import com.redhat.red.build.koji.model.json.FileBuildComponent;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.StandardArchitecture;
import com.redhat.red.build.koji.model.json.VerificationException;

import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts.PncArtifact;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;

import javax.enterprise.context.ApplicationScoped;

import java.net.MalformedURLException;
import java.util.List;

/**
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
@ApplicationScoped
public class BuildTranslatorImpl implements BuildTranslator {
    private static final String MAVEN = "maven";
    private static final String CONTENT_GENERATOR_VERSION = "0.10";
    private static final String CONTENT_GENERATOR_NAME = "Project Newcastle";
    private static final String PNC = "PNC";

    @Override
    public KojiImport translate(BrewNVR nvr,
                                BuildRecordRest build,
                                BuildArtifacts artifacts) throws CausewayException {
        String externalBuildId = String.valueOf(build.getId());
        String externalBuildUrl = null;
        String externalBuildsUrl = System.getProperty("pncl.builds.url");
        if (externalBuildsUrl != null) {
            externalBuildUrl = externalBuildsUrl + externalBuildId;
        }
        KojiImport.Builder builder = new KojiImport.Builder()
                .withNewBuildDescription(nvr.getKojiName(), nvr.getVersion(), nvr.getRelease())
                .withStartTime(build.getStartTime())
                .withEndTime(build.getEndTime())
                .withBuildSource(build.getBuildConfigurationAudited().getScmRepoURL(),
                        build.getBuildConfigurationAudited().getScmRevision())
                .withExternalBuildId(externalBuildId)
                .withExternalBuildUrl(externalBuildUrl)
                .withBuildSystem(PNC)
                .withMavenInfoAndType(buildRootToGAV(build))
                .parent();

        int buildRootId = 42;
        BuildRoot.Builder buildRootBuilder = builder.withNewBuildRoot(buildRootId)
                .withContentGenerator(CONTENT_GENERATOR_NAME, CONTENT_GENERATOR_VERSION)
                .withContainer(getContainer(build))
                .withHost(build.getBuildConfigurationAudited().getEnvironment()
                        .getAttributes().get("OS"), StandardArchitecture.noarch)
                .withTool("JDK", build.getBuildConfigurationAudited().getEnvironment()
                        .getAttributes().get("JDK"));

        addDependencies(artifacts.dependencies, buildRootBuilder);
        addBuiltArtifacts(artifacts.buildArtifacts, builder, buildRootId);

        try {
            return builder.build();
        } catch (VerificationException ex) {
            throw new CausewayException("Failure while building Koji Import JSON: " + ex.getMessage(), ex);
        }
    }

    private void addDependencies(List<PncArtifact> dependencies, BuildRoot.Builder buildRootBuilder) throws CausewayException {
        for (PncArtifact artifact : dependencies) {
            FileBuildComponent.Builder componentBuilder = buildRootBuilder
                    .withFileComponent(artifact.filename);
            componentBuilder.withChecksum("md5", artifact.checksum);

            switch (artifact.type) {
                case MAVEN: {
                    componentBuilder.withFileSize(artifact.size);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown artifact type");
                }
            }
        }
    }

    private void addBuiltArtifacts(List<PncArtifact> buildArtifacts, KojiImport.Builder builder,
            int buildRootId) throws CausewayException {
        for (BuildArtifacts.PncArtifact artifact : buildArtifacts) {
            BuildOutput.Builder outputBuilder = builder
                    .withNewOutput(buildRootId, artifact.filename)
                    .withArch(StandardArchitecture.noarch)
                    .withChecksum("md5", artifact.checksum);

            switch (artifact.type) {
                case MAVEN: {
                    SimpleArtifactRef ref = SimpleArtifactRef.parse(artifact.identifier);
                    outputBuilder.withFileSize((int) artifact.size);
                    outputBuilder.withMavenInfoAndType(ref);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown artifact type.");
                }
            }
        }
    }

    private BuildContainer getContainer(BuildRecordRest buildRecord) {
        switch (buildRecord.getBuildConfigurationAudited().getEnvironment().getSystemImageType()) {
            case DOCKER_IMAGE:
                return new BuildContainer("docker", "noarch");
            default:
                throw new IllegalArgumentException("Unknown system image type.");
        }
    }

    @Override
    public ImportFileGenerator getImportFiles(BuildArtifacts build) throws CausewayException {
        try{
            ImportFileGenerator ret = new ImportFileGenerator();
            for(PncArtifact artifact : build.buildArtifacts){
                ret.addUrl(artifact.id, artifact.deployUrl);
            }
            return ret;
        }catch(MalformedURLException ex){
            throw new CausewayException("Failed to parse artifact url: " + ex.getMessage(), ex);
        }
    }

    private ProjectVersionRef buildRootToGAV(BuildRecordRest build) {
        String[] splittedName = build.getExecutionRootName().split(":");
        if(splittedName.length != 2)
            throw new IllegalArgumentException("Execution root '"+build.getExecutionRootName()+"' doesnt seem to be maven G:A.");

        return new SimpleProjectVersionRef(
                        splittedName[0],
                        splittedName.length < 2 ? null : splittedName[1],
                        build.getExecutionRootVersion());
    }
}
