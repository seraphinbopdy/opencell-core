/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. This program is
 * not suitable for any direct or indirect application in MILITARY industry See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.meveo.api.endpoint.impl;

import java.net.URI;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.endpoint.EndpointDto;
import org.meveo.api.dto.response.GenericSearchResponse;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.api.endpoint.resource.EndpointResource;
import org.meveo.api.endpoint.service.EndpointApi;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.model.endpoint.Endpoint;

/**
 * Rest endpoint for managing service endpoints
 *
 * @author clement.bareth
 * @author Edward P. Legaspi | <czetsuya@gmail.com>
 * @since 04.02.2019
 * @version 6.9.0
 */
@RequestScoped
@Interceptors({ WsRestApiInterceptor.class })
public class EndpointResourceImpl extends BaseRs implements EndpointResource {

    @Inject
    private EndpointApi endpointApi;

    @Context
    private UriInfo uriContextInfo;

    @Override
    public Response create(EndpointDto endpointDto) throws BusinessException {

        final Endpoint endpoint = endpointApi.create(endpointDto);
        ResponseBuilder responseBuilder = Response.status(201);
        return responseBuilder.entity(endpoint.getId()).build();

    }

    @Override
    public Response update(EndpointDto endpointDto) throws BusinessException {

        final Endpoint endpoint = endpointApi.update(endpointDto);
        return Response.status(201).entity(endpoint.getId()).build();

    }

    @Override
    public Response createOrUpdate(EndpointDto endpointDto) throws BusinessException {
        final Endpoint endpoint = endpointApi.createOrUpdate(endpointDto);
        return Response.status(201).entity(endpoint.getId()).build();
    }

    @Override
    public Response list(PagingAndFiltering pagingAndFiltering) {
        GenericSearchResponse<EndpointDto> results = endpointApi.search(pagingAndFiltering);
        return Response.ok(results).status(200).build();
    }

    /**
     * Delete a {@link Endpoint}
     *
     * @param code Code of the {@link Endpoint} to delete
     */
    @Override
    public Response remove(String code) throws BusinessException, EntityDoesNotExistsException {
        endpointApi.remove(code);
        return Response.noContent().build();
    }

    /**
     * Find a {@link Endpoint} by code
     *
     * @param code Code of the {@link Endpoint} to find
     */
    @Override
    public Response find(String code) {
        final EndpointDto endpointDto = endpointApi.find(code);
        if (endpointDto != null) {
            return Response.ok(endpointDto).build();
        }
        return Response.status(404).build();
    }

    /**
     * Check exist a {@link Endpoint}
     *
     * @param code Code of the {@link Endpoint} to check
     */
    @Override
    public Response exists(String code) {
        final EndpointDto endpointDto = endpointApi.find(code);
        if (endpointDto != null) {
            return Response.noContent().status(200).build();
        }
        return Response.status(404).build();
    }

    /**
     * Generate open api json of a {@link Endpoint}
     *
     * @param code Code of the {@link Endpoint} to generate open api json
     */
    @Override
    public Response generateOpenApiJson(String code) {
        final URI contextUri = URI.create(uriContextInfo.getAbsolutePath().toString()).resolve(httpServletRequest.getContextPath());
        String basePath = contextUri.toString() + "/api/rest/custom/";
        return endpointApi.generateOpenApiJson(basePath, code);
    }
}