/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.api.rest;

import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.meveo.api.MeveoApiErrorCodeEnum;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.jaxb.AttributeTypeDeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Provider
public class JacksonProvider extends ResteasyJackson2Provider {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    
    public JacksonProvider() {
        log.info("JacksonProvider initialized");
    }
    
    @Override
    public ObjectMapper locateMapper(Class<?> arg0, MediaType arg1) {
        ObjectMapper mapper = super.locateMapper(arg0, arg1);
        try {
             mapper.setDateFormat(new StdDefaultDateFormat());
             mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
             mapper.registerModule(new JakartaXmlBindAnnotationModule());
             
             mapper.enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
             
        } catch (Exception e) {
            log.error(" error setting ObjectMapper DateFormat ", e);
        }
        return mapper;
    }

    @Override
    public Object readFrom(Class<Object> type, java.lang.reflect.Type genericType, Annotation[] annotations, 
                          MediaType mediaType, MultivaluedMap<String, String> httpHeaders, 
                          InputStream entityStream) throws IOException {
        try {
            return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        } catch (IOException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalStateException) {
                handleValidationError((IllegalStateException) cause);
            }else if (cause instanceof AttributeTypeDeserializationException) {
				throw new WebApplicationException(Response
					.status(Response.Status.BAD_REQUEST)
					.entity(new ActionStatus(
						ActionStatusEnum.FAIL,
						MeveoApiErrorCodeEnum.INVALID_PARAMETER,
						cause.getMessage()
					))
					.type(MediaType.APPLICATION_JSON)
					.build());
			}
            throw e;
        }
    }

    private void handleValidationError(IllegalStateException e) {
        String message = e.getMessage();
        if (message != null) {
            int startIndex = message.indexOf("[");
            int endIndex = message.indexOf("]");
            
            if (startIndex != -1 && endIndex != -1) {
                String missingFields = message.substring(startIndex + 1, endIndex);
                throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ActionStatus(
                        ActionStatusEnum.FAIL,
                        MeveoApiErrorCodeEnum.MISSING_PARAMETER,
                        "Required fields are missing: " + missingFields
                    ))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
            }
        }
        
        throw new WebApplicationException(Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new ActionStatus(
                ActionStatusEnum.FAIL,
                MeveoApiErrorCodeEnum.INVALID_PARAMETER,
                "Invalid request parameters"
            ))
            .type(MediaType.APPLICATION_JSON)
            .build());
    }
}

