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

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.meveo.api.endpoint.impl.EndpointResourceImpl;
import org.meveo.api.rest.filter.RESTCorsRequestFilter;
import org.meveo.api.rest.filter.RESTCorsResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;

/**
 * @author Edward P. Legaspi
 **/
@ApplicationPath("/api/rest/customization")
@OpenAPIDefinition()
public class JaxRsActivatorCustomApi extends Application {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Set<Class<?>> getClasses() {

        Logger log = LoggerFactory.getLogger(JaxRsActivatorCustomApi.class);
        log.debug("Documenting rest services for path /api/rest/customization");

        Set<Class<?>> resources = new HashSet();
        resources.add(EndpointResourceImpl.class);

        resources.add(RESTCorsRequestFilter.class);
        resources.add(RESTCorsResponseFilter.class);
        resources.add(JaxRsExceptionMapper.class);
        resources.add(JacksonProvider.class);
        resources.add(OpenApiResource.class);

        return resources;
    }
}