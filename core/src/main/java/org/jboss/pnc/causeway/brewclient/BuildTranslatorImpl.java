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

import com.github.zafarkhaja.semver.Version;
import com.redhat.red.build.koji.model.json.BuildContainer;
import com.redhat.red.build.koji.model.json.BuildDescription;
import com.redhat.red.build.koji.model.json.BuildOutput;
import com.redhat.red.build.koji.model.json.BuildRoot;
import com.redhat.red.build.koji.model.json.FileBuildComponent;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.StandardArchitecture;
import com.redhat.red.build.koji.model.json.StandardOutputType;
import com.redhat.red.build.koji.model.json.VerificationException;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;
import org.commonjava.atlas.npm.ident.util.NpmVersionUtils;
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
import org.jboss.pnc.causeway.rest.model.NpmBuild;
import org.jboss.pnc.causeway.rest.model.NpmBuiltArtifact;
import org.jboss.pnc.enums.BuildType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@ApplicationScoped
public class BuildTranslatorImpl implements BuildTranslator {
    private static final String CONTENT_GENERATOR_NAME = "Project Newcastle";
    static final String PNC = "PNC";
    private static final String MD5 = "md5";
    private static final String BREW_BUILD_NAME = "BrewBuildName";
    private static final String BREW_BUILD_VERSION = "BrewBuildVersion";

    private final CausewayConfig config;

    @Inject
    public BuildTranslatorImpl(CausewayConfig config) {
        this.config = config;
        config.configurationDone();
    }


    @Override
    public KojiImport translate(BrewNVR nvr,
            org.jboss.pnc.dto.Build build,
            BuildArtifacts artifacts,
            String log,
            String username) throws CausewayException {
        String externalBuildId = String.valueOf(build.getId());
        String externalBuildUrl = null;
        String externalBuildsUrl = config.getPnclBuildsURL();
        if (externalBuildsUrl != null) {
            externalBuildUrl = externalBuildsUrl + externalBuildId;
        }
        KojiImport.Builder builder = new KojiImport.Builder();
        BuildDescription.Builder descriptionBuilder = builder
                .withNewBuildDescription(nvr.getKojiName(), nvr.getVersion(), nvr.getRelease())
                .withStartTime(Date.from(build.getStartTime()))
                .withEndTime(Date.from(build.getEndTime()))
                .withBuildSource(normalizeScmUrl(build.getScmRepositoryURL()), build.getBuildConfigurationRevision().getScmRevision())
                .withExternalBuildId(externalBuildId)
                .withExternalBuildUrl(externalBuildUrl)
                .withBuildSystem(PNC);
        setBuildType(descriptionBuilder, build, artifacts);

        int buildRootId = 42;
        BuildRoot.Builder buildRootBuilder = builder.withNewBuildRoot(buildRootId)
                .withContentGenerator(CONTENT_GENERATOR_NAME, config.getPNCSystemVersion())
                .withContainer(getContainer(build))
                .withHost(build.getEnvironment()
                        .getAttributes().get("OS"), StandardArchitecture.noarch);

        addTool(buildRootBuilder, build);
        addDependencies(artifacts.dependencies, buildRootBuilder, build.getBuildConfigurationRevision().getBuildType());
        addBuiltArtifacts(artifacts.buildArtifacts, builder, buildRootId, build.getBuildConfigurationRevision().getBuildType());
        addLog(log, builder, buildRootId);

        KojiImport translatedBuild = buildTranslatedBuild(builder);
        translatedBuild.getBuild().getExtraInfo().setImportInitiator(username);
        return translatedBuild;
    }

    @Override
    public KojiImport translate(BrewNVR nvr, Build build, String username) throws CausewayException {
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

        KojiImport translatedBuild = buildTranslatedBuild(builder);
        translatedBuild.getBuild().getExtraInfo().setImportInitiator(username);
        return translatedBuild;
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
            String logHash = String.format("%032x", bi);
            builder.withNewOutput(buildRootId, "build.log")
                    .withOutputType(StandardOutputType.log)
                    .withFileSize(logBytes.length)
                    .withArch(StandardArchitecture.noarch)
                    .withChecksum(MD5, logHash);
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
                    .withChecksum(MD5, logfile.getMd5());
        }
    }

    private void addDependencies(List<PncArtifact> dependencies, BuildRoot.Builder buildRootBuilder, BuildType buildType) throws CausewayException {
        for (PncArtifact artifact : dependencies) {
            FileBuildComponent.Builder componentBuilder = buildRootBuilder
                    .withFileComponent(artifact.deployPath);
            componentBuilder.withChecksum(MD5, artifact.checksum);

            switch (buildType) {
                case GRADLE:
                case NPM:
                case MVN: {
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
                    .withChecksum(MD5, dependency.getMd5())
                    .withFileSize(dependency.getSize());
        }
    }

    private void addBuiltArtifacts(List<PncArtifact> buildArtifacts, KojiImport.Builder builder, int buildRootId,
            BuildType buildType) throws CausewayException {
        for (BuildArtifacts.PncArtifact artifact : buildArtifacts) {
            BuildOutput.Builder outputBuilder = builder
                    .withNewOutput(buildRootId, artifact.deployPath)
                    .withArch(StandardArchitecture.noarch)
                    .withChecksum(MD5, artifact.checksum);

            switch (buildType) {
                case GRADLE:
                case MVN: {
                    SimpleArtifactRef ref = SimpleArtifactRef.parse(artifact.identifier);
                    outputBuilder.withFileSize((int) artifact.size);
                    outputBuilder.withMavenInfoAndType(ref);
                    break;
                }
                case NPM: {
                    NpmPackageRef ref = NpmPackageRef.parse(artifact.identifier);
                    outputBuilder.withFileSize((int) artifact.size);
                    outputBuilder.withNpmInfoAndType(ref);
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
                    .withNewOutput(buildRootId, stripSlash(artifact.getArtifactPath()))
                    .withArch(artifact.getArchitecture())
                    .withChecksum(MD5, artifact.getMd5())
                    .withFileSize((int) artifact.getSize());

            if (artifact.getClass().equals(MavenBuiltArtifact.class)) {
                outputBuilder.withMavenInfoAndType(mavenArtifactToGAV((MavenBuiltArtifact) artifact));
            } else if (artifact.getClass().equals(NpmBuiltArtifact.class)) {
                outputBuilder.withNpmInfoAndType(npmArtifactToNV((NpmBuiltArtifact) artifact));
            } else {
                throw new IllegalArgumentException("Unknown artifact type.");
            }
        }
    }

    private BuildContainer getContainer(org.jboss.pnc.causeway.rest.model.BuildRoot buildRoot) {
        return new BuildContainer(buildRoot.getContainer(), buildRoot.getContainerArchitecture());
    }

    private BuildContainer getContainer(org.jboss.pnc.dto.Build buildRecord) {
        switch (buildRecord.getEnvironment().getSystemImageType()) {
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
                String url = config.getArtifactStorage() + stripSlash(artifact.getRepositoryPath()) + "/" + stripSlash(artifact.getArtifactPath());
                ret.addUrl(artifact.getId(), url, stripSlash(artifact.getArtifactPath()));
            }
            return ret;
        }catch(MalformedURLException ex){
            throw new CausewayException("Failed to parse artifact url: " + ex.getMessage(), ex);
        }
    }

    private KojiImport buildTranslatedBuild(KojiImport.Builder builder) throws CausewayException {
        final KojiImport translatedBuild;
        try {
            translatedBuild = builder.build();
        } catch (VerificationException ex) {
            throw new CausewayException("Failure while building Koji Import JSON: " + ex.getMessage(), ex);
        }
        return translatedBuild;
    }

    private String stripSlash(String url) {
        if (url.startsWith("/")) {
            return url.substring(1);
        }
        return url;
    }


    private ProjectVersionRef buildRootToGAV(org.jboss.pnc.dto.Build build, BuildArtifacts artifacts) throws CausewayException{
        if (!build.getAttributes().containsKey(BREW_BUILD_NAME)) {
            throw new CausewayException("Build attribute " + BREW_BUILD_NAME + " can't be missing");
        }
        String[] splittedName = build.getAttributes().get(BREW_BUILD_NAME).split(":");
        if(splittedName.length != 2)
            throw new IllegalArgumentException(BREW_BUILD_NAME + " attribute '" + build.getAttributes().get(BREW_BUILD_NAME) + "' doesn't seem to be maven G:A.");
        String version = build.getAttributes().get(BREW_BUILD_VERSION);
        if(version == null){
            version = BuildTranslator.guessVersion(build, artifacts);
        }
        return new SimpleProjectVersionRef(
                        splittedName[0],
                        splittedName.length < 2 ? null : splittedName[1],
                        version);
    }

    private NpmPackageRef buildRootToNV(org.jboss.pnc.dto.Build build, BuildArtifacts artifacts) throws CausewayException {
        if (!build.getAttributes().containsKey(BREW_BUILD_NAME)) {
            throw new CausewayException("Build attribute " + BREW_BUILD_NAME + " can't be missing");
        }
        String name = build.getAttributes().get(BREW_BUILD_NAME);
        String version = build.getAttributes().get(BREW_BUILD_VERSION);
        if (version == null) {
            version = BuildTranslator.guessVersion(build, artifacts);
        }
        return new NpmPackageRef(name, Version.valueOf(version));
    }

    private void addTools(BuildRoot.Builder buildRootBuilder, Map<String, String> tools) {
        for (Map.Entry<String, String> e : tools.entrySet()) {
            buildRootBuilder.withTool(e.getKey(), e.getValue());
        }
    }

    private void setBuildType(BuildDescription.Builder buildDescription, Build build) throws CausewayException {
        if (build.getClass().equals(MavenBuild.class)) {
            buildDescription.withMavenInfoAndType(mavenBuildToGAV((MavenBuild) build));
        } else if (build.getClass().equals(NpmBuild.class)) {
            buildDescription.withNpmInfoAndType(npmBuildToNV((NpmBuild) build));
        } else {
            throw new IllegalArgumentException("Unsupported build type.");
        }
    }

    private void setBuildType(BuildDescription.Builder buildDescription, org.jboss.pnc.dto.Build build, BuildArtifacts artifacts) throws CausewayException {
        BuildType buildType = build.getBuildConfigurationRevision().getBuildType();
        switch (buildType) {
            case MVN:
            case GRADLE:
                buildDescription.withMavenInfoAndType(buildRootToGAV(build, artifacts));
                break;
            case NPM:
                buildDescription.withNpmInfoAndType(buildRootToNV(build, artifacts));
                break;
            default:
                throw new IllegalArgumentException("Unsupported build type.");
        }
    }

    private void addTool(BuildRoot.Builder buildRootBuilder, org.jboss.pnc.dto.Build build) throws CausewayException {
        BuildType buildType = build.getBuildConfigurationRevision().getBuildType();
        switch (buildType) {
            case MVN:
                buildRootBuilder.withTool("JDK", build.getEnvironment().getAttributes().get("JDK"));
                break;
            case GRADLE:
                buildRootBuilder.withTool("GRADLE", build.getEnvironment().getAttributes().get("GRADLE"));
                break;
            case NPM:
                buildRootBuilder.withTool("NPM", build.getEnvironment().getAttributes().get("NPM"));
                break;
            default:
                throw new IllegalArgumentException("Unsupported build type.");
        }
    }

    private ProjectVersionRef mavenBuildToGAV(MavenBuild mb) throws CausewayException {
        String version = mb.getVersion();
        if(version == null){
            version = BuildTranslator.guessVersion(mb);
        }
        return new SimpleProjectVersionRef(mb.getGroupId(), mb.getArtifactId(), version);
    }

    private NpmPackageRef npmBuildToNV(NpmBuild npmBuild) throws CausewayException{
        String version = npmBuild.getVersion();
        if(version == null){
            version = BuildTranslator.guessVersion(npmBuild);
        }
        return new NpmPackageRef(npmBuild.getName(), NpmVersionUtils.valueOf(version));
    }

    private ProjectVersionRef mavenArtifactToGAV(MavenBuiltArtifact mba) {
        return new SimpleProjectVersionRef(mba.getGroupId(), mba.getArtifactId(), mba.getVersion());
    }

    private NpmPackageRef npmArtifactToNV(NpmBuiltArtifact npmBuiltArtifact) {
        return new NpmPackageRef(npmBuiltArtifact.getName(), NpmVersionUtils.valueOf(npmBuiltArtifact.getVersion()));
    }
}
