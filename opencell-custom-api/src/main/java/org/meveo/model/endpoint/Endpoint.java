/*
 * (C) Copyright 2018-2020 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.meveo.model.EnableBusinessEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.ObservableEntity;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.validation.constraint.nointersection.NoIntersectionBetween;

/**
 * Configuration of a REST endpoint implemented as a script
 *
 * @author clement.bareth
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 */
@Entity
@Cacheable
@Table(name = "adm_endpoint")
@GenericGenerator(name = "ID_GENERATOR", strategy = "increment")
@NoIntersectionBetween(firstCollection = "pathParameters.scriptParameter", secondCollection = "parametersMapping.scriptParameter")
@ExportIdentifier({ "code" })
@ObservableEntity
public class Endpoint extends EnableBusinessEntity {

    private static final long serialVersionUID = 6561905332917884613L;

    public static final Pattern pathParamPattern = Pattern.compile("\\{[a-zA-Z0-9_\\-./]+\\}");

    /**
     * Whether endpoint is accessible without authorization
     */
    @Column(name = "secured", nullable = false)
    @Type(type = "numeric_boolean")
    private boolean isSecured = true;

    /**
     * Script associated to the endpoint
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id", updatable = true, nullable = false)
    private ScriptInstance service;

    /**
     * Whether the execution of the service will be synchronous. If asynchronous, and id of execution will be returned to the user.
     */
    @Type(type = "numeric_boolean")
    @Column(name = "synchronous", nullable = false)
    private boolean synchronous = true;

    /**
     * Method used to access the endpoint. Conditionates the input format of the endpoint.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 10)
    private EndpointHttpMethod method;

    /**
     * Parameters that will be exposed in the endpoint path
     */
    @ElementCollection(fetch = FetchType.LAZY) // Left lazy as two collections can not be fetched eagerly
    @CollectionTable(name = "adm_endpoint_path_parameter", joinColumns = { @JoinColumn(name = "endpoint_id") })
    @AttributeOverrides(value = { @AttributeOverride(name = "scriptParameter", column = @Column(name = "script_parameter", length = 50)), @AttributeOverride(name = "position", column = @Column(name = "position")) })
    private List<EndpointPathParameter> pathParameters;

    /**
     * Mapping of the query or request body parameters that are not defined as path parameters
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "adm_endpoint_parameters", joinColumns = { @JoinColumn(name = "endpoint_id") })
    @AttributeOverrides(value = { @AttributeOverride(name = "scriptParameter", column = @Column(name = "script_parameter", length = 50)),
            @AttributeOverride(name = "parameterName", column = @Column(name = "parameter_name", length = 50)),
            @AttributeOverride(name = "multivalued", column = @Column(name = "multivalued", nullable = false, columnDefinition = "int default 0")),
            @AttributeOverride(name = "defaultValue", column = @Column(name = "default_value", length = 255)),
            @AttributeOverride(name = "valueRequired", column = @Column(name = "value_required", nullable = false, columnDefinition = "int default 0")),
            @AttributeOverride(name = "description", column = @Column(name = "description", length = 255)), @AttributeOverride(name = "example", column = @Column(name = "example", length = 500)) })
    private List<EndpointParameterMapping> parametersMapping;

    /**
     * JSONata query used to transform the result
     */
    @Column(name = "jsonata_transformer", length = 255)
    @Size(max = 255)
    private String jsonataTransformer;

    /**
     * Context variable to be returned by the endpoint
     */
    @Column(name = "returned_variable_name", length = 50)
    @Size(max = 50)
    private String returnedVariableName;

    /**
     * An example of returned data
     */
    @Column(name = "returned_value_example", length = 500)
    @Size(max = 500)
    private String returnedValueExample;

    /**
     * Context variable to be returned by the endpoint
     */
    @Type(type = "numeric_boolean")
    @Column(name = "serialize_result", nullable = false)
    private boolean serializeResult;

    /**
     * Content type of the response
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 50, nullable = false)
    private MimeContentTypeEnum contentType = MimeContentTypeEnum.APPLICATION_JSON;

    @Column(name = "base_path", length = 30, nullable = false)
    @Size(max = 30)
    private String basePath;

    /**
     * The path in swagger form like /organizations/{orgId}/members/{memberId} to be added to the base path to form the relative URL of the endpoint
     */
    @Column(name = "path", length = 255)
    @Size(max = 255)
    private String path;

    @Transient
    Pattern pathRegex;

    @PrePersist
    @PreUpdate
    private void prePersist() {

        if (pathParameters != null) {
            for (int i = 0; i < pathParameters.size(); i++) {
                pathParameters.get(i).setPosition(i);
            }
        }

    }

    public void setCode(String code) {
        this.code = code;
    }

    public MimeContentTypeEnum getContentType() {
        return contentType;
    }

    public void setContentType(MimeContentTypeEnum contentType) {
        this.contentType = contentType;
    }

    public void setSerializeResult(boolean serializeResult) {
        this.serializeResult = serializeResult;
    }

    public boolean isSerializeResult() {
        return serializeResult;
    }

    public String getReturnedVariableName() {
        return returnedVariableName;
    }

    public void setReturnedVariableName(String returnedVariableName) {
        this.returnedVariableName = returnedVariableName;
    }

    public String getJsonataTransformer() {
        return jsonataTransformer;
    }

    public void setJsonataTransformer(String jsonataTransformer) {
        this.jsonataTransformer = jsonataTransformer;
    }

    public ScriptInstance getService() {
        return service;
    }

    public void setService(ScriptInstance service) {
        this.service = service;
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    public EndpointHttpMethod getMethod() {
        return method;
    }

    public void setMethod(EndpointHttpMethod method) {
        this.method = method;
    }

    public List<EndpointPathParameter> getPathParameters() {
        return pathParameters;
    }

    public void setPathParameters(List<EndpointPathParameter> pathParameters) {
        this.pathParameters = pathParameters;
    }

    public List<EndpointParameterMapping> getParametersMapping() {
        return parametersMapping;
    }

    public void setParametersMapping(List<EndpointParameterMapping> parametersMapping) {
        this.parametersMapping = parametersMapping;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public void setSecured(boolean isSecured) {
        this.isSecured = isSecured;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public static String getPath(List<EndpointPathParameter> pathParameters) {
        String sep = "";
        final StringBuilder endpointPath = new StringBuilder("/");
        if (pathParameters != null) {
            for (EndpointPathParameter endpointPathParameter : pathParameters) {
                endpointPath.append(sep).append("{").append(endpointPathParameter).append("}");
                sep = "/";
            }
        }
        return endpointPath.toString();
    }

    public String getPath() {
        if (path == null) {
            path = Endpoint.getPath(pathParameters);
            pathRegex = null;
        }
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        pathRegex = null;
        getPath();
    }

    @Transient
    /*
     * returns the endpoint url relative to the meveo base url
     */
    public String getEndpointUrl() {
        return getBasePath() + getPath();
    }

    @Transient
    public Pattern getPathRegex() {
        if (pathRegex == null) {
            String pattern = "/" + getBasePath() + getPath().replaceAll("\\{", "(?<").replaceAll("\\}", ">[^/]+)");
            if (pattern.endsWith("/")) {
                pattern += "?";
            }
            pathRegex = Pattern.compile(pattern);
        }
        return pathRegex;
    }

    public void addPathParameter(EndpointPathParameter endpointPathParameter) {
        if (pathParameters == null) {
            pathParameters = new ArrayList<EndpointPathParameter>();
        }

        pathParameters.add(endpointPathParameter);
    }

    public void addParametersMapping(EndpointParameterMapping e) {
        if (parametersMapping == null) {
            parametersMapping = new ArrayList<EndpointParameterMapping>();
        }

        parametersMapping.add(e);
    }

    /**
     * Get a role name required to access endpoint
     * 
     * @return A role name required to access endpoint
     */
    public String getRoleName() {
        return Endpoint.getRoleName(code);
    }

    /**
     * Get a role name required to access endpoint
     * 
     * @param code Endpoint code
     * @return A role name required to access endpoint
     */
    public static String getRoleName(String code) {
        return "Custom_API_" + code + "-access";
    }

    /**
     * @return An example of returned data
     */
    public String getReturnedValueExample() {
        return returnedValueExample;
    }

    /**
     * @param returnedValueExample An example of returned data
     */
    public void setReturnedValueExample(String returnedValueExample) {
        this.returnedValueExample = returnedValueExample;
    }
}