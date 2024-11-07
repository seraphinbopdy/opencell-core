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

package org.meveo.api.rest.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author Edward P. Legaspi
 **/
@Provider
public class RESTCorsResponseFilter implements ContainerResponseFilter {
    private final static Logger log = LoggerFactory.getLogger(RESTCorsResponseFilter.class.getName());

    private static final List<String> ALLOWED_ORIGINS = Arrays.stream(ParamBean.getInstance()
            .getProperty("cors.config.allowed.origin", "").split(","))
            .filter(StringUtils::isNotBlank).collect(Collectors.toList());

    @Override
    public void filter(ContainerRequestContext requestCtx, ContainerResponseContext responseCtx) throws IOException {

        // Check if the request is a CORS request by looking for the Origin header
        String originHeader = requestCtx.getHeaderString("Origin");

        if (originHeader != null && !originHeader.isEmpty()) {
            if (ALLOWED_ORIGINS.isEmpty()) {
                // If no cors config is defined in properties then allow all origins
            responseCtx.getHeaders().add("Access-Control-Allow-Origin", "*");
                // add others necessary CORS headers to the response
                addOthersCORSHeaders(responseCtx);

            } else if (ALLOWED_ORIGINS.contains(originHeader)) {
                // If the origin is allowed,
                responseCtx.getHeaders().add("Access-Control-Allow-Origin", originHeader);
                responseCtx.getHeaders().add("Vary", "Origin");
                // add others necessary CORS headers to the response
                addOthersCORSHeaders(responseCtx);

            } else {
                log.warn("Request CORS Origin {} doesn't exist in the configured CORS white list!", originHeader);
            }
        }
    }

    private void addOthersCORSHeaders(ContainerResponseContext responseCtx) {
            responseCtx.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH");
            responseCtx.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            responseCtx.getHeaders().add("Access-Control-Allow-Credentials", true);
        }
    }
