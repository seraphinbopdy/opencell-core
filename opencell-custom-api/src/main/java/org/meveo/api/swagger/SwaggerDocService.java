package org.meveo.api.swagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.endpoint.Endpoint;
import org.meveo.model.endpoint.EndpointHttpMethod;
import org.meveo.model.endpoint.EndpointParameterMapping;
import org.meveo.model.endpoint.EndpointPathParameter;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.script.ScriptUtils;
import org.meveo.util.Version;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.inject.Inject;

/**
 * Service class for generating swagger documentation on the fly.
 * 
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @since 6.8.0
 * @version 6.10
 */

public class SwaggerDocService {

    @Inject
    private CustomEntityTemplateService customEntityTemplateService;

    @Inject
    private SwaggerHelperService swaggerHelperService;

    /**
     * Generate a OpenAPI v3 JSON schema for a single Endpoint
     * 
     * @param baseUrl Base Endpoint URL
     * @param endpoint Endpoint to generate schema for
     * @return OpenApi v3 JSON schema definition
     */
    @SuppressWarnings("rawtypes")
    public OpenAPI generateOpenApiJson(String baseUrl, Endpoint endpoint) {

        OpenAPI openApi = new OpenAPI();

        // openApi.setBasePath(baseUrl);
        // openApi.setSchemes(Arrays.asList(Scheme.HTTPS));
        // openApi.setProduces(Collections.singletonList(endpoint.getContentType().getValue()));
        if (endpoint.getMethod() == EndpointHttpMethod.POST) {
            // openApi.setConsumes(Arrays.asList("application/json", "application/xml"));
        }

        Info info = new Info();
        openApi.setInfo(info);
        info.setTitle(endpoint.getCode());
        info.setDescription(endpoint.getDescription());
        info.setVersion(Version.appVersion);

        Paths paths = new Paths();
        openApi.setPaths(paths);
        PathItem path = new PathItem();
        paths.addPathItem(endpoint.getEndpointUrl(), path);

        Operation operation = new Operation();
        boolean isHeadMethod = false;

        switch (endpoint.getMethod()) {
        case DELETE:
            path.setDelete(operation);
            break;
        case GET:
            path.setGet(operation);
            break;
        case HEAD:
            path.setHead(operation);
            isHeadMethod = true;
            break;
        case POST:
            path.setPost(operation);
            break;
        case PUT:
            path.setPut(operation);
            break;
        default:
            break;

        }

        if (endpoint.getPathParameters() != null) {
            for (EndpointPathParameter endpointPathParameter : endpoint.getPathParameters()) {
                Parameter parameter = new PathParameter();
                parameter.setName(endpointPathParameter.getScriptParameter());
                path.addParametersItem(parameter);
            }
        }

        if (endpoint.getParametersMapping() != null) {
            List<Parameter> operationParameters = new ArrayList<>();
            operation.setParameters(operationParameters);

            for (EndpointParameterMapping parameterMapping : endpoint.getParametersMapping()) {

                if (endpoint.getMethod().equals(EndpointHttpMethod.GET)) {
                    QueryParameter queryParameter = new QueryParameter();
                    queryParameter.setName(parameterMapping.getParameterName());
                    queryParameter.setDescription(parameterMapping.getDescription());

                    if (parameterMapping.getExample() != null) {
                        queryParameter.setExample(parameterMapping.getExample());
                    }

                    Schema parameterSchema = new Schema();
                    parameterSchema.setDefault(parameterMapping.getDefaultValue());
                    parameterSchema.setFormat(ScriptUtils.findScriptVariableType(endpoint.getService(), parameterMapping.getScriptParameter()));
                    queryParameter.setSchema(parameterSchema);

                    operationParameters.add(queryParameter);

                } else if (endpoint.getMethod().equals(EndpointHttpMethod.POST) || endpoint.getMethod().equals(EndpointHttpMethod.PUT)) {
                    RequestBody requestBody = new RequestBody();
                    Content bodyContent = new Content();
                    MediaType mediaType = new MediaType();

                    bodyContent.addMediaType(parameterMapping.getParameterName() != null ? parameterMapping.getParameterName() : parameterMapping.getScriptParameter(), mediaType);
                    requestBody.setContent(bodyContent);
                    Schema requestSchema = buildBodyParameterSchema(endpoint.getService(), parameterMapping);
                    mediaType.setSchema(requestSchema);

                    if (parameterMapping.getExample() != null) {
                        Example example = new Example();
                        example.setValue(parameterMapping.getExample());
                        mediaType.addExamples(endpoint.getContentType().getValue(), example);
                    }
                    operation.setRequestBody(requestBody);
                }
            }

        }

        ApiResponse operationResponse = new ApiResponse();
        ApiResponses operationResponses = new ApiResponses();
        operation.setResponses(operationResponses);
        operationResponses.addApiResponse("" + HttpStatus.SC_OK, operationResponse);

        Content content = new Content();
        operationResponse.setContent(content);
        MediaType responseMediaType = new MediaType();
        content.addMediaType("", responseMediaType);

        if (!isHeadMethod && !StringUtils.isBlank(endpoint.getReturnedValueExample())) {
            String mediaType = endpoint.getContentType().getValue();
            Example example = new Example();
            example.setValue(endpoint.getReturnedValueExample());
            responseMediaType.addExamples(mediaType, example);
        }

        if (!isHeadMethod) {
            Schema responseSchema = buildResponseSchema(endpoint);
            if (responseSchema != null) {
                responseMediaType.setSchema(responseSchema);
            }
        }

        return openApi;
    }

    @SuppressWarnings("rawtypes")
    private Schema buildBodyParameterSchema(ScriptInstance service, EndpointParameterMapping parameterMapping) {

        Schema returnModelSchema ;
        String scriptParameter = parameterMapping.getScriptParameter();
        String parameterDataType = ScriptUtils.findScriptVariableType(service, parameterMapping.getScriptParameter());

        if (ReflectionUtils.isPrimitiveOrWrapperType(parameterDataType)) {
            returnModelSchema = swaggerHelperService.buildSchemaForPrimitiveTypeValue(parameterMapping.getParameterName(), parameterDataType, parameterMapping.isValueRequiredAsBoolean(), parameterMapping.getDefaultValue());

        } else {

            CustomEntityTemplate returnedCet = customEntityTemplateService.findByDbTablename(scriptParameter);
            if (returnedCet != null) {
                returnModelSchema = swaggerHelperService.buildSchemaForCetTypeValue(returnedCet);

            } else {
                returnModelSchema = swaggerHelperService.buildSchemaForObjectTypeValue(parameterMapping.getParameterName());
            }
        }

        return returnModelSchema;
    }

    @SuppressWarnings("rawtypes")
    private Schema buildResponseSchema(Endpoint endpoint) {

        if (!StringUtils.isBlank(endpoint.getReturnedVariableName())) {

            Schema returnModelSchema ;
            String returnedVariableType = ScriptUtils.findScriptVariableType(endpoint.getService(), endpoint.getReturnedVariableName());

            if (ReflectionUtils.isPrimitiveOrWrapperType(returnedVariableType)) {
                returnModelSchema = swaggerHelperService.buildSchemaForPrimitiveTypeValue(endpoint.getReturnedVariableName(), returnedVariableType, false, null);

            } else {

                CustomEntityTemplate returnedCet = customEntityTemplateService.findByDbTablename(returnedVariableType);
                if (returnedCet != null) {
                    returnModelSchema = swaggerHelperService.buildSchemaForCetTypeValue(returnedCet);

                } else {
                    returnModelSchema = swaggerHelperService.buildSchemaForObjectTypeValue(endpoint.getReturnedVariableName());
                }
            }

            return returnModelSchema;
        }
        return null;
    }
}
