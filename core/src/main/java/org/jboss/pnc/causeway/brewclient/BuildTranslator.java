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
import org.jboss.pnc.causeway.pncclient.PncBuild;
import org.jboss.pnc.causeway.pncclient.PncBuild.PncArtifact;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;

import javax.inject.Inject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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
public class BuildTranslator {
    private static final String MAVEN = "maven";
    private static final String CONTENT_GENERATOR_VERSION = "0.10";
    private static final String CONTENT_GENERATOR_NAME = "Project Newcastle";

    private final PncClient pncClient;

    private final Indy indy;

    @Inject
    public BuildTranslator(PncClient pncClient, Indy indy) {
        this.pncClient = pncClient;
        this.indy = indy;
    }

    public KojiImport translate(BrewNVR nvr, PncBuild build) throws CausewayException,
            VerificationException, IndyClientException, MalformedURLException {
        int buildId = build.getId();

        BuildRecordRest buildRecord = pncClient.findBuild(buildId);

        StoreKey store = new StoreKey(StoreType.hosted, buildRecord.getBuildContentId());

        KojiImport.Builder builder = new KojiImport.Builder()
                .withNewBuildDescription(nvr.getName(), nvr.getVersion(), nvr.getRelease())
                .withStartTime(buildRecord.getStartTime())
                .withEndTime(buildRecord.getEndTime())
                .withBuildType(StandardBuildType.maven)
                .withBuildSource(buildRecord.getBuildConfigurationAudited().getScmRepoURL(),
                        buildRecord.getBuildConfigurationAudited().getScmRevision())
                .parent();

        int buildRootId = 42;
        BuildRoot.Builder buildRootBuilder = builder.withNewBuildRoot(buildRootId)
                .withContentGenerator(CONTENT_GENERATOR_NAME, CONTENT_GENERATOR_VERSION)
                .withContainer(getContainer(buildRecord))
                .withHost(buildRecord.getBuildConfigurationAudited().getEnvironment()
                        .getAttributes().get("OS"), StandardArchitecture.noarch)
                .withTool("JDK", buildRecord.getBuildConfigurationAudited().getEnvironment()
                        .getAttributes().get("JDK"));

        addDependencies(build.dependencies, buildRootBuilder);
        addBuiltArtifacts(build.buildArtifacts, builder, buildRootId, store);

        return builder.build();
    }

    private void addDependencies(List<PncArtifact> dependencies, BuildRoot.Builder buildRootBuilder)
            throws IndyClientException, MalformedURLException, IllegalArgumentException {
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
            int buildRootId, StoreKey store) throws IllegalArgumentException, IndyClientException {
        for (PncBuild.PncArtifact artifact : buildArtifacts) {
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
                    PathInfo info = indy.content().getInfo(store, path);
                    outputBuilder.withFileSize(info.getContentLength());
                    outputBuilder.withMavenInfoAndType(ref);

                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown artifact type.");
                }
            }
        }
    }

    private PathInfo getIndyInfo(String indyUrl) throws MalformedURLException, IndyClientException {
        URL url = new URL(indyUrl);
        String deployPath = url.getPath();
        String[] parts = deployPath.split("/", 5);
        if (!"api".equals(parts[1])) {
            throw new IllegalArgumentException("Url " + indyUrl
                    + " doesn't seem to be Indy url. It needs to start with api.");
        }

        StoreType st = StoreType.valueOf(parts[2]);
        String name = parts[3];
        String path = parts[4];

        try (Indy indy2 = new Indy("http://" + url.getAuthority() + "/api").connect()) {
            return indy2.content().getInfo(st, name, path);
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
}
