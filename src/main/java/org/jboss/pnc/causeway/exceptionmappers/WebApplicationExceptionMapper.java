/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.exceptionmappers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.pnc.api.dto.ErrorResponse;

import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        if (exception.getResponse().getStatus() == 400) {
            Throwable cause = exception.getCause();
            if (cause instanceof JsonMappingException) {
                log.debug("Bad format of the request: {}", cause.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(cause))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build();
            }
        }
        return exception.getResponse();
    }
}