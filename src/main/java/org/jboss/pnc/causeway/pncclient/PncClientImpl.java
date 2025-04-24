/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.pncclient;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.causeway.CausewayConfig;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.ErrorMessages;
import org.jboss.pnc.causeway.impl.BurnAfterReadingFile;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Optional;

/**
 * Created by jdcasey on 2/9/16.
 */
@ApplicationScoped
@Slf4j
public class PncClientImpl implements PncClient {

    private final BuildClient buildClient;

    @Inject
    public PncClientImpl(CausewayConfig config) {
        URL url = config.pnc().url();
        Configuration clientConfig = Configuration.builder()
                .host(url.getHost())
                .protocol(url.getProtocol())
                .pageSize(config.pnc().pageSize())
                .addDefaultMdcToHeadersMappings()
                .port((url.getPort() != -1) ? url.getPort() : url.getDefaultPort())
                .build();

        log.debug("Client config: " + clientConfig);
        this.buildClient = new BuildClient(clientConfig);
    }

    @Retry
    @ExponentialBackoff
    @Override
    public Build findBuild(String buildId) throws CausewayException {
        try {
            log.debug("Getting build with id {} from PNC", buildId);
            Build build = buildClient.getSpecific(buildId);
            if (build == null) {
                throw new CausewayException(ErrorMessages.pncBuildNotFound(buildId));
            }
            return build;
        } catch (RemoteResourceException ex) {
            throw new CausewayException(ErrorMessages.errorReadingBuild(buildId, ex), ex);
        }
    }

    @Retry
    @ExponentialBackoff
    @Override
    public BurnAfterReadingFile getBuildLog(String buildId) throws CausewayException {
        Optional<InputStream> buildLog;
        try {
            log.debug("Getting build logs of build with id {} from PNC", buildId);
            buildLog = buildClient.getBuildLogs(String.valueOf(buildId));
            InputStream logInput = buildLog
                    .orElseThrow(() -> new CausewayException(ErrorMessages.buildLogIsEmpty(buildId)));
            return BurnAfterReadingFile.fromInputStream("build.log", logInput);
        } catch (RemoteResourceException ex) {
            throw new CausewayException(ErrorMessages.errorReadingBuildLog(buildId, ex), ex);
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new CausewayException(ErrorMessages.errorStoringBuildLog(buildId, ex), ex);
        }
    }

    @Retry
    @ExponentialBackoff
    @Override
    public BurnAfterReadingFile getAlignLog(String buildId) throws CausewayException {
        Optional<InputStream> buildLog;
        try {
            log.debug("Getting align logs of build with id {} from PNC", buildId);
            buildLog = buildClient.getAlignLogs(String.valueOf(buildId));
            InputStream logInput = buildLog
                    .orElseThrow(() -> new CausewayException(ErrorMessages.buildLogIsEmpty(buildId)));
            return BurnAfterReadingFile.fromInputStream("align.log", logInput);
        } catch (RemoteResourceException ex) {
            throw new CausewayException(ErrorMessages.errorReadingAlignLog(buildId, ex), ex);
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new CausewayException(ErrorMessages.errorStoringAlignLog(buildId, ex), ex);
        }
    }

    @Retry
    @ExponentialBackoff
    @Override
    public InputStream getSources(String id) throws CausewayException {
        try {
            log.debug("Getting sources of build with id {} from PNC", id);
            Response response = buildClient.getInternalScmArchiveLink(id);
            try {
                if (response.getStatus() >= 400) {
                    throw new CausewayException(
                            ErrorMessages.errorReadingBuildSources(
                                    id,
                                    response.getStatus(),
                                    response.readEntity(String.class)));
                }
                return response.readEntity(InputStream.class);
            } catch (RuntimeException ex) {
                response.close();
                throw new CausewayException(ErrorMessages.errorReadingBuildSources(id, ex), ex);
            }
        } catch (RemoteResourceException ex) {
            throw new CausewayException(ErrorMessages.errorReadingBuildSources(id, ex), ex);
        }
    }

    @Retry
    @ExponentialBackoff
    @Override
    public BuildArtifacts findBuildArtifacts(String buildId) throws CausewayException {
        try {
            Collection<Artifact> builtArtifacts = buildClient.getBuiltArtifacts(buildId).getAll();
            Collection<Artifact> dependantArtifact = buildClient.getDependencyArtifacts(buildId).getAll();
            BuildArtifacts build = new BuildArtifacts();

            build.getBuildArtifacts().addAll(builtArtifacts);
            build.getDependencies().addAll(dependantArtifact);

            return build;
        } catch (RemoteResourceException ex) {
            throw new CausewayException(ErrorMessages.errorReadingBuildArtifacts(buildId, ex), ex);
        }
    }

}
