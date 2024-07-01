package org.meveo.api.swagger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

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

import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;

/**
 * Service class for generating swagger documentation on the fly.
 * 
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @since 6.8.0
 * @version 6.10
 */
@Stateless
public class SwaggerDocService {

    @Inject
    private CustomEntityTemplateService customEntityTemplateService;

    @Inject
    private SwaggerHelperService swaggerHelperService;

    public Swagger generateOpenApiJson(String baseUrl, Endpoint endpoint) {

        Info info = new Info();
        info.setTitle(endpoint.getCode());
        info.setDescription(endpoint.getDescription());
        info.setVersion(Version.appVersion);

        Map<String, Path> paths = new HashMap<>();
        Path path = new Path();

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
                path.addParameter(parameter);
            }
        }

        paths.put(endpoint.getEndpointUrl(), path);

        if (endpoint.getParametersMapping() != null) {
            List<Parameter> operationParameter = new ArrayList<>();

            for (EndpointParameterMapping parameterMapping : endpoint.getParametersMapping()) {

                if (endpoint.getMethod().equals(EndpointHttpMethod.GET)) {
                    QueryParameter queryParameter = new QueryParameter();
                    queryParameter.setName(parameterMapping.getParameterName());
                    queryParameter.setDefaultValue(parameterMapping.getDefaultValue());
                    queryParameter.setDescription(parameterMapping.getDescription());
                    queryParameter.setFormat(ScriptUtils.findScriptVariableType(endpoint.getService(), parameterMapping.getScriptParameter()));

                    if (parameterMapping.getExample() != null) {
                        queryParameter.setExample(parameterMapping.getExample());
                    }

                    operationParameter.add(queryParameter);

                } else if (endpoint.getMethod().equals(EndpointHttpMethod.POST)) {
                    BodyParameter bodyParameter = new BodyParameter();
                    bodyParameter.setName(parameterMapping.getParameterName() != null ? parameterMapping.getParameterName() : parameterMapping.getScriptParameter());
                    bodyParameter.setSchema(buildBodyParameterSchema(endpoint.getService(), parameterMapping));

                    if (parameterMapping.getExample() != null) {
                        bodyParameter.addExample(endpoint.getContentType().getValue(), parameterMapping.getExample());
                    }
                    operationParameter.add(bodyParameter);
                }
            }

            operation.setParameters(operationParameter);
        }

        Map<String, io.swagger.models.Response> responses = new HashMap<>();
        io.swagger.models.Response response = new io.swagger.models.Response();

        if (!isHeadMethod && !StringUtils.isBlank(endpoint.getReturnedValueExample())) {
            String mediaType = endpoint.getContentType().getValue();
            response.example(mediaType, endpoint.getReturnedValueExample());
        }

        if (!isHeadMethod) {
            buildResponseSchema(endpoint, response);
        }

        responses.put("" + HttpStatus.SC_OK, response);

        Swagger swagger = new Swagger();
        swagger.setInfo(info);
        swagger.setBasePath(baseUrl);
        swagger.setSchemes(Arrays.asList(Scheme.HTTPS));
        swagger.setProduces(Collections.singletonList(endpoint.getContentType().getValue()));
        if (endpoint.getMethod() == EndpointHttpMethod.POST) {
            swagger.setConsumes(Arrays.asList("application/json", "application/xml"));
        }
        swagger.setPaths(paths);
        swagger.setResponses(responses);

        return swagger;
    }

    private Model buildBodyParameterSchema(ScriptInstance service, EndpointParameterMapping parameterMapping) {

        Model returnModelSchema;
        String scriptParameter = parameterMapping.getScriptParameter();
        String parameterDataType = ScriptUtils.findScriptVariableType(service, parameterMapping.getScriptParameter());

        if (ReflectionUtils.isPrimitiveOrWrapperType(parameterDataType)) {
            returnModelSchema = swaggerHelperService.buildPrimitiveResponse(parameterMapping.getParameterName(), parameterDataType, parameterMapping.isValueRequiredAsBoolean(), parameterMapping.getDefaultValue());
            returnModelSchema.setReference("primitive");

        } else {

            CustomEntityTemplate returnedCet = customEntityTemplateService.findByDbTablename(scriptParameter);
            if (returnedCet != null) {
                returnModelSchema = swaggerHelperService.cetToModel(returnedCet);

            } else {
                returnModelSchema = swaggerHelperService.buildObjectResponse(parameterMapping.getParameterName());
            }
        }

        return returnModelSchema;
    }

    private void buildResponseSchema(Endpoint endpoint, io.swagger.models.Response response) {

        if (!StringUtils.isBlank(endpoint.getReturnedVariableName())) {

            Model returnModelSchema;
            String returnedVariableType = ScriptUtils.findScriptVariableType(endpoint.getService(), endpoint.getReturnedVariableName());

            if (ReflectionUtils.isPrimitiveOrWrapperType(returnedVariableType)) {
                returnModelSchema = swaggerHelperService.buildPrimitiveResponse(endpoint.getReturnedVariableName(), returnedVariableType, false, null);
                returnModelSchema.setReference("primitive");

            } else {

                CustomEntityTemplate returnedCet = customEntityTemplateService.findByDbTablename(returnedVariableType);
                if (returnedCet != null) {
                    returnModelSchema = swaggerHelperService.cetToModel(returnedCet);

                } else {
                    returnModelSchema = swaggerHelperService.buildObjectResponse(endpoint.getReturnedVariableName());
                }
            }

            response.setResponseSchema(returnModelSchema);
        }
    }
}
