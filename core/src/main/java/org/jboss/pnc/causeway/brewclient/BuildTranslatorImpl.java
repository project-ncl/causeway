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

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts.PncArtifact;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.redhat.red.build.koji.model.json.BuildContainer;
import com.redhat.red.build.koji.model.json.BuildOutput;
import com.redhat.red.build.koji.model.json.BuildRoot;
import com.redhat.red.build.koji.model.json.FileBuildComponent;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.StandardArchitecture;
import com.redhat.red.build.koji.model.json.StandardBuildType;
import com.redhat.red.build.koji.model.json.VerificationException;

/**
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
@ApplicationScoped
public class BuildTranslatorImpl implements BuildTranslator {
    private static final String MAVEN = "maven";
    private static final String CONTENT_GENERATOR_VERSION = "0.10";
    private static final String CONTENT_GENERATOR_NAME = "Project Newcastle";

    private final Indy indy;

    @Inject
    public BuildTranslatorImpl(Indy indy) {
        this.indy = indy;
    }

    @Override
    public KojiImport translate(BrewNVR nvr, BuildRecordRest build, BuildArtifacts artifacts) throws CausewayException {
        StoreKey store = new StoreKey(StoreType.hosted, build.getBuildContentId());

        KojiImport.Builder builder = new KojiImport.Builder()
                .withNewBuildDescription(nvr.getName(), nvr.getVersion(), nvr.getRelease())
                .withStartTime(build.getStartTime())
                .withEndTime(build.getEndTime())
                .withBuildType(StandardBuildType.maven)
                .withBuildSource(build.getBuildConfigurationAudited().getScmRepoURL(),
                        build.getBuildConfigurationAudited().getScmRevision())
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
        addBuiltArtifacts(artifacts.buildArtifacts, builder, buildRootId, store);

        try {
            return builder.build();
        } catch (VerificationException ex) {
            throw new CausewayException("Failure while building Koji Import JSON", ex);
        }
    }

    private void addDependencies(List<PncArtifact> dependencies, BuildRoot.Builder buildRootBuilder) throws CausewayException {
        for (PncArtifact artifact : dependencies) {
            FileBuildComponent.Builder componentBuilder = buildRootBuilder
                    .withFileComponent(artifact.filename);
            componentBuilder.withChecksum("md5", artifact.checksum);

            switch (artifact.type) {
                case MAVEN: {
                    PathInfo info = getIndyInfo(artifact.deployUrl);
                    componentBuilder.withFileSize(info.getContentLength());
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown artifact type");
                }
            }
        }
    }

    private void addBuiltArtifacts(List<PncArtifact> buildArtifacts, KojiImport.Builder builder,
            int buildRootId, StoreKey store) throws CausewayException {
        for (BuildArtifacts.PncArtifact artifact : buildArtifacts) {
            BuildOutput.Builder outputBuilder = builder
                    .withNewOutput(buildRootId, artifact.filename)
                    .withArch(StandardArchitecture.noarch)
                    .withChecksum("md5", artifact.checksum);

            switch (artifact.type) {
                case MAVEN: {
                    SimpleArtifactRef ref = SimpleArtifactRef.parse(artifact.identifier);
                    final String path = ref.getGroupId().replace('.', '/')
                            + '/' + ref.getArtifactId()
                            + '/' + ref.getVersionStringRaw()
                            + '/' + artifact.filename;
                    try {
                        PathInfo info = indy.content().getInfo(store, path);
                        outputBuilder.withFileSize(info.getContentLength());
                        outputBuilder.withMavenInfoAndType(ref);
                    } catch (IndyClientException ex) {
                        throw new CausewayException("Failed to get info from Indy for path '%s'", ex, path);
                    }
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown artifact type.");
                }
            }
        }
    }

    private PathInfo getIndyInfo(String indyUrl) throws CausewayException {
        try {
            URL url = new URL(indyUrl);
            String deployPath = url.getPath();
            String[] parts = deployPath.split("/", 5);
            if (!"api".equals(parts[1])) {
                throw new IllegalArgumentException("Url " + indyUrl
                        + " doesn't seem to be Indy url. Path needs to start with \"api/\".");
            }
            
            StoreType st = StoreType.valueOf(parts[2]);
            String name = parts[3];
            String path = parts[4];

            try (Indy indy2 = new Indy("http://" + url.getAuthority() + "/api").connect()) {
                return indy2.content().getInfo(st, name, path);
            }
        }   catch (MalformedURLException | IndyClientException ex) {
            throw new CausewayException("Failed to get info from Indy for url '%s'", ex, indyUrl);
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
    public ImportFileGenerator getImportFiles(BuildArtifacts build) {
        Set<String> urls = new HashSet<>();
        for(PncArtifact artifact : build.buildArtifacts){
            urls.add(artifact.deployUrl);
        }
        for(PncArtifact artifact : build.dependencies){
            urls.add(artifact.deployUrl);
        }
        return new ImportFileGenerator(urls);
    }
}
