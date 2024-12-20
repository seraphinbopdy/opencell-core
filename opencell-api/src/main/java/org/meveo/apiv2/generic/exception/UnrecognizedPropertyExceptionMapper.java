package org.meveo.apiv2.generic.exception;

import org.jboss.resteasy.api.validation.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class UnrecognizedPropertyExceptionMapper implements ExceptionMapper<UnrecognizedPropertyException> {
    private final static Logger log = LoggerFactory.getLogger(UnrecognizedPropertyExceptionMapper.class);
    private final ExceptionSerializer exceptionSerializer = new ExceptionSerializer(Response.Status.BAD_REQUEST);

    @Override
    public Response toResponse(UnrecognizedPropertyException exception) {
        log.error("A not found exception occurred ", exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(exceptionSerializer.toApiError(exception)).type(MediaType.APPLICATION_JSON).header(Validation.VALIDATION_HEADER, "true").build();
    }
}
