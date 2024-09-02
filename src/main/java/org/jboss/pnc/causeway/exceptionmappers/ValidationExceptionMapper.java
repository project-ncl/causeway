/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.exceptionmappers;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.api.dto.ErrorResponse;

@Provider
@Slf4j
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        log.debug("Bad format of the request: {}", exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(exception))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
