package org.meveo.apiv2.generic.exception;

import org.jboss.resteasy.api.validation.Validation;
import org.meveo.admin.exception.ModeRollBackRatingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ModeRollBackRatingExceptionMapper implements ExceptionMapper<ModeRollBackRatingException> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public Response toResponse(ModeRollBackRatingException exception) {
        log.error("A roll back mode exception occurred ", exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getProcessCdrListResult()).type(MediaType.APPLICATION_JSON)
                .header(Validation.VALIDATION_HEADER, "true").build();
    }

}
