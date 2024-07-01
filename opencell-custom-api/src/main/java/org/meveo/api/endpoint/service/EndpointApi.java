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
package org.meveo.api.endpoint.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseCrudApi;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.api.dto.endpoint.EndpointDto;
import org.meveo.api.dto.endpoint.EndpointParameterMappingDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.swagger.SwaggerDocService;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.endpoint.Endpoint;
import org.meveo.model.endpoint.EndpointParameterMapping;
import org.meveo.model.endpoint.EndpointPathParameter;
import org.meveo.model.endpoint.MimeContentTypeEnum;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.endpoint.EndpointService;
import org.meveo.service.script.ScriptInstanceService;

import io.swagger.v3.core.util.Json;

/**
 * API for managing technical services endpoints
 *
 * @author clement.bareth
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 */
@Stateless
public class EndpointApi extends BaseCrudApi<Endpoint, EndpointDto> {

    @Inject
    private EndpointService endpointService;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    private SwaggerDocService swaggerDocService;

    /**
     * Create an endpoint
     *
     * @param endpointDto Configuration of the endpoint
     * @return the created Endpoint
     */
    public Endpoint create(EndpointDto endpointDto) throws MeveoApiException, BusinessException {

        Endpoint endpoint = endpointService.findByCode(endpointDto.getCode());
        if (endpoint != null) {
            throw new EntityAlreadyExistsException(Endpoint.class, endpointDto.getCode());
        }

        endpoint = fromDto(endpointDto, null);
        endpointService.create(endpoint);
        return endpoint;

    }

    @Override
    public Endpoint update(EndpointDto endpointDto) throws MeveoApiException, BusinessException {

        Endpoint endpoint = endpointService.findByCode(endpointDto.getCode());
        if (endpoint == null) {
            throw new EntityDoesNotExistsException(Endpoint.class, endpointDto.getCode());
        }

        endpoint = fromDto(endpointDto, endpoint);
        endpoint = endpointService.update(endpoint);

        return endpoint;
    }

    /**
     * Convert JPA entity to DTO
     * 
     * @param endpoint JPA entity to convert
     * @return DTO representation of endpoint
     */
    public EndpointDto toDto(Endpoint endpoint) {
        EndpointDto endpointDto = new EndpointDto();
        endpointDto.setId(endpoint.getId());
        endpointDto.setCode(endpoint.getCode());
        endpointDto.setDescription(endpoint.getDescription());
        endpointDto.setMethod(endpoint.getMethod());
        endpointDto.setServiceCode(endpoint.getService().getCode());
        endpointDto.setSynchronous(endpoint.isSynchronous());
        endpointDto.setSecured(endpoint.isSecured());
        endpointDto.setReturnedVariableName(endpoint.getReturnedVariableName());
        endpointDto.setReturnedValueExample(endpoint.getReturnedValueExample());
        endpointDto.setSerializeResult(endpoint.isSerializeResult());
        List<String> pathParameterDtos = new ArrayList<>();
        endpointDto.setPathParameters(pathParameterDtos);
        if (endpoint.getPathParameters() != null) {
            for (EndpointPathParameter pathParameter : endpoint.getPathParameters()) {
                pathParameterDtos.add(pathParameter.getScriptParameter());
            }
        }
        List<EndpointParameterMappingDto> mappingDtos = new ArrayList<>();
        endpointDto.setParameterMappings(mappingDtos);
        if (endpoint.getParametersMapping() != null) {
            for (EndpointParameterMapping tsParameterMapping : endpoint.getParametersMapping()) {
                EndpointParameterMappingDto mappingDto = new EndpointParameterMappingDto();
                mappingDto.setDefaultValue(tsParameterMapping.getDefaultValue());
                mappingDto.setValueRequired(tsParameterMapping.isValueRequiredAsBoolean());
                mappingDto.setParameterName(tsParameterMapping.getParameterName());
                mappingDto.setScriptParameter(tsParameterMapping.getScriptParameter());
                mappingDto.setMultivalued(tsParameterMapping.isMultivaluedAsBoolean());
                mappingDto.setDescription(tsParameterMapping.getDescription());
                mappingDto.setExample(tsParameterMapping.getExample());
                mappingDtos.add(mappingDto);
            }
        }
        endpointDto.setJsonataTransformer(endpoint.getJsonataTransformer());
        endpointDto.setContentType(endpoint.getContentType().getValue());
        endpointDto.setBasePath(endpoint.getBasePath());
        endpointDto.setPath(endpoint.getPath());
        return endpointDto;
    }

    /**
     * Convert dto to an entity, or update existing entity
     * 
     * @param endpointDto DTO
     * @param endpoint JPA entity
     * @return A JPA entity
     * @throws EntityDoesNotExistsException
     */
    private Endpoint fromDto(EndpointDto endpointDto, Endpoint endpoint) throws EntityDoesNotExistsException {

        boolean create = false;
        if (endpoint == null) {
            endpoint = new Endpoint();
            create = true;
        }

        // Code
        if (endpointDto.getCode() != null) {
            endpoint.setCode(endpointDto.getCode());
        }
        // Description
        if (endpointDto.getDescription() != null) {
            endpoint.setDescription(endpointDto.getDescription());
        }

        // Method
        if (endpointDto.getMethod() != null) {
            endpoint.setMethod(endpointDto.getMethod());
        }

        // Synchronous
        if (endpointDto.getSynchronous() != null) {
            endpoint.setSynchronous(endpointDto.getSynchronous());
        }

        // Secured
        if (endpointDto.getSecured() != null) {
            endpoint.setSecured(endpointDto.getSecured());
        }

        // JSONata query
        if (endpointDto.getJsonataTransformer() != null) {
            if (StringUtils.isBlank(endpointDto.getJsonataTransformer())) {
                endpoint.setJsonataTransformer(null);
            } else {
                endpoint.setJsonataTransformer(endpointDto.getJsonataTransformer());
            }
        }

        // Returned variable name
        if (endpointDto.getReturnedVariableName() != null) {
            if (StringUtils.isBlank(endpointDto.getReturnedVariableName())) {
                endpoint.setReturnedVariableName(null);
            } else {
                endpoint.setReturnedVariableName(endpointDto.getReturnedVariableName());
            }
        }

        // Returned value example
        if (endpointDto.getReturnedValueExample() != null) {
            if (StringUtils.isBlank(endpointDto.getReturnedValueExample())) {
                endpoint.setReturnedValueExample(null);
            } else {
                endpoint.setReturnedValueExample(endpointDto.getReturnedValueExample());
            }
        }

        // Technical Service
        if (!StringUtils.isBlank(endpointDto.getServiceCode())) {
            ScriptInstance scriptInstance = scriptInstanceService.findByCode(endpointDto.getServiceCode());
            if (scriptInstance == null) {
                throw new EntityDoesNotExistsException(ScriptInstance.class, endpointDto.getServiceCode());
            }
            endpoint.setService(scriptInstance);
        }

        if ((endpointDto.getParameterMappings() != null && !endpointDto.getParameterMappings().isEmpty())) {
            // Parameters mappings
            List<EndpointParameterMapping> tsParameterMappings = getParameterMappings(endpointDto);
            endpoint.setParametersMapping(tsParameterMappings);
        }
        if ((endpointDto.getPathParameters() != null && !endpointDto.getPathParameters().isEmpty())) {
            // Path parameters
            List<EndpointPathParameter> endpointPathParameters = getEndpointPathParameters(endpointDto);
            endpoint.setPathParameters(endpointPathParameters);
        }

        if (endpointDto.getSerializeResult() != null) {
            endpoint.setSerializeResult(endpointDto.getSerializeResult());
        }
        if (endpointDto.getContentType() != null) {
            endpoint.setContentType(MimeContentTypeEnum.fromMimeType(endpointDto.getContentType()));
        }

        if (endpointDto.getBasePath() != null) {
            endpoint.setBasePath(endpointDto.getBasePath());
        } else if (create) {
            endpoint.setBasePath(StringUtils.cleanupSpecialCharactersAndSpaces(endpoint.getCode()).toLowerCase());
        }

        if (endpointDto.getPath() != null) {
            endpoint.setPath(endpointDto.getPath());

        } else if (create && endpoint.getParametersMapping() != null && !endpoint.getParametersMapping().isEmpty()) {
            endpoint.setPath(Endpoint.getPath(endpoint.getPathParameters()));
        }

        return endpoint;
    }

    /**
     * Extract and convert path parameter mapping DTO to entities
     * 
     * @param endpointDto Endpoint DTO
     * @return A list of endpoint path parameters
     */
    private List<EndpointPathParameter> getEndpointPathParameters(EndpointDto endpointDto) {
        List<EndpointPathParameter> endpointPathParameters = new ArrayList<>();
        for (String pathParameter : endpointDto.getPathParameters()) {
            EndpointPathParameter endpointPathParameter = new EndpointPathParameter();
            endpointPathParameter.setScriptParameter(pathParameter);
            endpointPathParameters.add(endpointPathParameter);
        }
        return endpointPathParameters;
    }

    /**
     * Extract and convert arameter mapping DTO to entities
     * 
     * @param endpointDto Endpoint DTO
     * @return A list of endpoint parameter mappings
     */
    private List<EndpointParameterMapping> getParameterMappings(EndpointDto endpointDto) {
        List<EndpointParameterMapping> tsParameterMappings = new ArrayList<>();
        for (EndpointParameterMappingDto parameterMappingDto : endpointDto.getParameterMappings()) {
            EndpointParameterMapping tsParameterMapping = new EndpointParameterMapping();
            tsParameterMapping.setDefaultValue(parameterMappingDto.getDefaultValue());
            tsParameterMapping.setParameterName(parameterMappingDto.getParameterName());
            tsParameterMapping.setValueRequiredAsBoolean(parameterMappingDto.getValueRequired());
            tsParameterMapping.setScriptParameter(parameterMappingDto.getScriptParameter());
            tsParameterMapping.setMultivaluedAsBoolean(parameterMappingDto.getMultivalued());
            tsParameterMapping.setDescription(parameterMappingDto.getDescription());
            tsParameterMapping.setExample(parameterMappingDto.getExample());

            tsParameterMappings.add(tsParameterMapping);
        }
        return tsParameterMappings;
    }

    /**
     * Check if current user is authorized to access the endpoint
     * 
     * @param endpoint Endpoint to check
     * @return True if user is authorized to access the endpoint
     */
    public boolean isUserAuthorized(Endpoint endpoint) {
        if (!endpoint.isSecured()) {
            return true;
        }

        return currentUser.hasRole(endpoint.getRoleName());
    }

    public Response generateOpenApiJson(@NotNull String baseUrl, @NotNull String code) {

        Endpoint endpoint = endpointService.findByCode(code);
        if (endpoint == null) {
            return Response.noContent().build();
        }

        if (!isUserAuthorized(endpoint)) {
            return Response.status(403).entity("You are not authorized to access this endpoint").build();
        }

        return Response.ok(Json.pretty(swaggerDocService.generateOpenApiJson(baseUrl, endpoint))).build();
    }

    @Override
    protected BiFunction<Endpoint, CustomFieldsDto, EndpointDto> getEntityToDtoFunction() {
        return (entity, customFieldInstances) -> toDto(entity);
    }
}