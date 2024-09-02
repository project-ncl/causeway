/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.causeway.dto.push.BuildPushRequest;
import org.jboss.pnc.api.causeway.dto.push.PushResult;
import org.jboss.pnc.api.causeway.dto.untag.UntagRequest;
import org.jboss.pnc.api.causeway.rest.Causeway;
import org.jboss.pnc.api.dto.ComponentVersion;
import org.jboss.pnc.api.dto.HeartbeatConfig;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.causeway.constants.BuildInformationConstants;
import org.jboss.pnc.causeway.ctl.ImportController;
import org.jboss.pnc.common.concurrent.HeartbeatScheduler;
import org.jboss.pnc.common.http.PNCHttpClient;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

@ApplicationScoped
@Slf4j
public class CausewayResource implements Causeway {

    @Inject
    ImportController controller;

    @Inject
    ManagedExecutor executor;

    @Inject
    HeartbeatScheduler heartbeat;

    @Inject
    PNCHttpClient httpClient;

    @ConfigProperty(name = "quarkus.application.name")
    String name;

    @Override
    @Consumes(MediaType.APPLICATION_JSON)
    public void importBuild(@Valid BuildPushRequest buildPushRequest) {
        HeartbeatConfig heartbeatConf = buildPushRequest.getHeartbeat();
        if (heartbeatConf != null) {
            heartbeat.subscribeRequest(buildPushRequest.getId(), heartbeatConf);
        }

        executor.supplyAsync(
                () -> controller.importBuild(
                        buildPushRequest.getBuildId(),
                        buildPushRequest.getTagPrefix(),
                        buildPushRequest.isReimport(),
                        buildPushRequest.getUsername()))
                .exceptionally(e -> handleError(e, buildPushRequest.getBuildId()))
                .thenAccept(sendCallback(buildPushRequest.getCallback()))
                .whenComplete((_r, e) -> {
                    heartbeat.unsubscribeRequest(buildPushRequest.getId());
                });
        throw new WebApplicationException(Response.Status.ACCEPTED); // return 202 status, quarkus/issues/30170
    }

    private Consumer<PushResult> sendCallback(Request callback) {
        if (callback == null) {
            return (pushResult -> {});
        } else {
            return (pushResult -> {
                try {
                    httpClient.sendRequest(callback, pushResult);
                } catch (RuntimeException ex) {
                    log.error("Error when sending callback " + callback + " with " + pushResult + ".");
                }
            });
        }
    }

    private static PushResult handleError(Throwable e, String buildId) {
        PushResult.PushResultBuilder resultBuilder = PushResult.builder().buildId(buildId);
        if (e instanceof CompletionException) {
            e = e.getCause();
        }
        if (e instanceof CausewayFailure) {
            log.info("Failure: " + e.getMessage());
            resultBuilder.result(ResultStatus.FAILED);
        } else if (e instanceof CausewayException) {
            log.error(e.getMessage(), e);
            resultBuilder.result(ResultStatus.SYSTEM_ERROR);
        } else {
            log.error("Unexpected error while pushing to Brew.", e);
            resultBuilder.result(ResultStatus.SYSTEM_ERROR);
        }

        return resultBuilder.build();
    }

    @Override
    public void untagBuild(@Valid UntagRequest request) {
        controller.untagBuild(request.getBuild().getBrewBuildId(), request.getBuild().getTagPrefix());
    }

    @Override
    public ComponentVersion getVersion() {
        return ComponentVersion.builder()
                .name(name)
                .version(BuildInformationConstants.VERSION)
                .commit(BuildInformationConstants.COMMIT_HASH)
                .builtOn(ZonedDateTime.parse(BuildInformationConstants.BUILD_TIME))
                .build();
    }
}
