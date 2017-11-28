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
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts.PncArtifact;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.model.Build;
import org.jboss.pnc.causeway.rest.model.BuiltArtifact;
import org.jboss.pnc.causeway.rest.model.Dependency;
import org.jboss.pnc.causeway.rest.model.Logfile;
import org.jboss.pnc.causeway.rest.model.MavenBuild;
import org.jboss.pnc.causeway.rest.model.MavenBuiltArtifact;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.redhat.red.build.koji.model.json.BuildDescription;
import com.redhat.red.build.koji.model.json.StandardOutputType;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@ApplicationScoped
public class BuildTranslatorImpl implements BuildTranslator {
    private static final String MAVEN = "maven";
    private static final String CONTENT_GENERATOR_NAME = "Project Newcastle";
    static final String PNC = "PNC";

    private final CausewayConfig config;

    @Inject
    public BuildTranslatorImpl(CausewayConfig config) {
        this.config = config;
        config.configurationDone();
    }


    @Override
    public KojiImport translate(BrewNVR nvr,
            BuildRecordRest build,
            BuildArtifacts artifacts,
            String log) throws CausewayException {
        String externalBuildId = String.valueOf(build.getId());
        String externalBuildUrl = null;
        String externalBuildsUrl = config.getPnclBuildsURL();
        if (externalBuildsUrl != null) {
            externalBuildUrl = externalBuildsUrl + externalBuildId;
        }
        KojiImport.Builder builder = new KojiImport.Builder()
                .withNewBuildDescription(nvr.getKojiName(), nvr.getVersion(), nvr.getRelease())
                .withStartTime(build.getStartTime())
                .withEndTime(build.getEndTime())
                .withBuildSource(normalizeScmUrl(build.getScmRepoURL()), build.getScmRevision())
                .withExternalBuildId(externalBuildId)
                .withExternalBuildUrl(externalBuildUrl)
                .withBuildSystem(PNC)
                .withMavenInfoAndType(buildRootToGAV(build))
                .parent();

        int buildRootId = 42;
        BuildRoot.Builder buildRootBuilder = builder.withNewBuildRoot(buildRootId)
                .withContentGenerator(CONTENT_GENERATOR_NAME, config.getPNCSystemVersion())
                .withContainer(getContainer(build))
                .withHost(build.getBuildConfigurationAudited().getEnvironment()
                        .getAttributes().get("OS"), StandardArchitecture.noarch)
                .withTool("JDK", build.getBuildConfigurationAudited().getEnvironment()
                        .getAttributes().get("JDK"));

        addDependencies(artifacts.dependencies, buildRootBuilder);
        addBuiltArtifacts(artifacts.buildArtifacts, builder, buildRootId);
        addLog(log, builder, buildRootId);

        try {
            return builder.build();
        } catch (VerificationException ex) {
            throw new CausewayException("Failure while building Koji Import JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    public KojiImport translate(BrewNVR nvr, Build build) throws CausewayException {
        KojiImport.Builder builder = new KojiImport.Builder();

        BuildDescription.Builder descriptionBuilder = builder
                .withNewBuildDescription(nvr.getKojiName(), nvr.getVersion(), nvr.getRelease())
                .withStartTime(build.getStartTime())
                .withEndTime(build.getEndTime())
                .withBuildSource(normalizeScmUrl(build.getScmURL()), build.getScmRevision())
                .withExternalBuildId(String.valueOf(build.getExternalBuildID()))
                .withExternalBuildUrl(build.getExternalBuildURL())
                .withBuildSystem(PNC);
        setBuildType(descriptionBuilder, build);

        int buildRootId = 42;
        BuildRoot.Builder buildRootBuilder = builder.withNewBuildRoot(buildRootId)
                .withContentGenerator(CONTENT_GENERATOR_NAME, config.getPNCSystemVersion())
                .withContainer(getContainer(build.getBuildRoot()))
                .withHost(build.getBuildRoot().getHost(), build.getBuildRoot().getHostArchitecture());
        addTools(buildRootBuilder, build.getBuildRoot().getTools());

        addDependencies(build.getDependencies(), buildRootBuilder);
        addBuiltArtifacts(build.getBuiltArtifacts(), builder, buildRootId);
        addLogs(build, builder, buildRootId);

        try {
            return builder.build();
        } catch (VerificationException ex) {
            throw new CausewayException("Failure while building Koji Import JSON: " + ex.getMessage(), ex);
        }
    }

    private String normalizeScmUrl(final String url) {
        if(url.startsWith("http")) {
            return "git+"+url;
        }
        return url;
    }

    private void addLog(String log, KojiImport.Builder builder, int buildRootId) throws CausewayException {
        try {
            byte[] logBytes = log.getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            BigInteger bi = new BigInteger(1, md.digest(logBytes));
            String logHash = bi.toString(16);
            builder.withNewOutput(buildRootId, "build.log")
                    .withOutputType(StandardOutputType.log)
                    .withFileSize(logBytes.length)
                    .withArch(StandardArchitecture.noarch)
                    .withChecksum("MD5", logHash);
        } catch (NoSuchAlgorithmException ex) {
            throw new CausewayException("Failed to compute md5 sum of build log: " + ex.getMessage(), ex);
        }
    }

    private void addLogs(Build build, KojiImport.Builder builder, int buildRootId) {
        for (Logfile logfile : build.getLogs()) {
            builder.withNewOutput(buildRootId, logfile.getFilename())
                    .withOutputType(StandardOutputType.log)
                    .withFileSize(logfile.getSize())
                    .withArch(StandardArchitecture.noarch)
                    .withChecksum("MD5", logfile.getMd5());
        }
    }

    private void addDependencies(List<PncArtifact> dependencies, BuildRoot.Builder buildRootBuilder) throws CausewayException {
        for (PncArtifact artifact : dependencies) {
            FileBuildComponent.Builder componentBuilder = buildRootBuilder
                    .withFileComponent(artifact.deployPath);
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

    private void addDependencies(Set<Dependency> dependencies, BuildRoot.Builder buildRootBuilder) throws CausewayException {
        for (Dependency dependency : dependencies) {
            buildRootBuilder
                    .withFileComponent(dependency.getFilename())
                    .withChecksum("md5", dependency.getMd5())
                    .withFileSize(dependency.getSize());
        }
    }

    private void addBuiltArtifacts(List<PncArtifact> buildArtifacts, KojiImport.Builder builder,
            int buildRootId) throws CausewayException {
        for (BuildArtifacts.PncArtifact artifact : buildArtifacts) {
            BuildOutput.Builder outputBuilder = builder
                    .withNewOutput(buildRootId, artifact.deployPath)
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

    private void addBuiltArtifacts(Set<BuiltArtifact> builtArtifacts, KojiImport.Builder builder,
            int buildRootId) throws CausewayException {
        for (BuiltArtifact artifact : builtArtifacts) {
            BuildOutput.Builder outputBuilder = builder
                    .withNewOutput(buildRootId, artifact.getDeployPath())
                    .withArch(artifact.getArchitecture())
                    .withChecksum("md5", artifact.getMd5())
                    .withFileSize((int) artifact.getSize());

            if (artifact.getClass().equals(MavenBuiltArtifact.class)) {
                outputBuilder.withMavenInfoAndType(mavenArtifactToGAV((MavenBuiltArtifact) artifact));
            } else {
                throw new IllegalArgumentException("Unknown artifact type.");
            }
        }
    }

    private BuildContainer getContainer(org.jboss.pnc.causeway.rest.model.BuildRoot buildRoot) {
        return new BuildContainer(buildRoot.getContainer(), buildRoot.getContainerArchitecture());
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
    public ImportFileGenerator getImportFiles(BuildArtifacts build, String log) throws CausewayException {
        try{
            StringLogImportFileGenerator ret = new StringLogImportFileGenerator(log);
            for(PncArtifact artifact : build.buildArtifacts){
                ret.addUrl(artifact.id, artifact.deployUrl, artifact.deployPath);
            }
            return ret;
        }catch(MalformedURLException ex){
            throw new CausewayException("Failed to parse artifact url: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ImportFileGenerator getImportFiles(Build build) throws CausewayException {
        try{
            ExternalLogImportFileGenerator ret = new ExternalLogImportFileGenerator();
            for(Logfile logfile : build.getLogs()){
                String url = config.getLogStorage() + stripSlash(logfile.getDeployPath());
                ret.addLog(url, logfile.getFilename(), logfile.getSize());
            }
            for(BuiltArtifact artifact : build.getBuiltArtifacts()){
                String url = config.getArtifactStorage() + stripSlash(artifact.getDeployPath());
                ret.addUrl(artifact.getId(), url, artifact.getDeployPath());
            }
            return ret;
        }catch(MalformedURLException ex){
            throw new CausewayException("Failed to parse artifact url: " + ex.getMessage(), ex);
        }
    }

    private String stripSlash(String url) {
        if (url.startsWith("/")) {
            return url.substring(1);
        }
        return url;
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

    private void addTools(BuildRoot.Builder buildRootBuilder, Map<String, String> tools) {
        for (Map.Entry<String, String> e : tools.entrySet()) {
            buildRootBuilder.withTool(e.getKey(), e.getValue());
        }
    }

    private void setBuildType(BuildDescription.Builder buildDescription, Build build) {
        if (build.getClass().equals(MavenBuild.class)) {
            buildDescription.withMavenInfoAndType(mavenBuildToGAV((MavenBuild) build));
        } else {
            throw new IllegalArgumentException("Unsupported build type.");
        }
    }

    private ProjectVersionRef mavenBuildToGAV(MavenBuild mb) {
        return new SimpleProjectVersionRef(mb.getGroupId(), mb.getArtifactId(), mb.getVersion());
    }

    private ProjectVersionRef mavenArtifactToGAV(MavenBuiltArtifact mba) {
        return new SimpleProjectVersionRef(mba.getGroupId(), mba.getArtifactId(), mba.getVersion());
    }
}
