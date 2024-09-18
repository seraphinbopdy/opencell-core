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
package org.meveo.service.endpoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.endpoint.service.EndpointExecution;
import org.meveo.cache.endpoint.EndpointResult;
import org.meveo.cache.endpoint.EndpointResult.EndpointResponseStatusEnum;
import org.meveo.cache.endpoint.PendingResult;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.event.monitoring.ClusterEventDto.ClusterEventActionEnum;
import org.meveo.event.monitoring.ClusterEventPublisher;
import org.meveo.event.qualifier.Processed;
import org.meveo.model.endpoint.Endpoint;
import org.meveo.model.endpoint.EndpointExecutionResult;
import org.meveo.model.endpoint.EndpointParameterMapping;
import org.meveo.model.endpoint.EndpointPathParameter;
import org.meveo.model.endpoint.EndpointVariables;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.model.scripts.ScriptPool;
import org.meveo.model.security.Role;
import org.meveo.service.admin.impl.RoleService;
import org.meveo.service.base.BusinessService;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

import com.api.jsonata4java.Expression;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * EJB for managing technical services endpoints
 *
 * @author clement.bareth
 * @author Edward P. Legaspi | czetsuya@gmail.com
 */
@Stateless
public class EndpointService extends BusinessService<Endpoint> {

    /**
     * A Keycloak role that groups all roles to access custom API endpoints
     */
    public static final String ROLE_NAME_ACCESS_ALL_CUSTOM_API = "Custom_API-AccessAll";

    /**
     * A parameter name, indicating asynchronous operation ID, to pass as JMS message attribute
     */
    public static final String CLUSTER_MQ_PARAMETER_ASYNC_ID = "asyncId";

    /**
     * A parameter name, indicating if endpoint execution should be canceled, to pass as JMS message attribute
     */
    public static final String CLUSTER_MQ_PARAMETER_IS_CANCEL = "isCancel";

    /**
     * A parameter name, indicating if endpoint execution results should ve kept in cache even they were provided to a client earlier, to pass as JMS message attribute
     */
    public static final String CLUSTER_MQ_PARAMETER_IS_KEEP = "isKeep";

    /**
     * A parameter name, indicating if request should wait for endpoint execution to finish, to pass as JMS message attribute
     */
    public static final String CLUSTER_MQ_PARAMETER_IS_WAIT = "isWait";

    /**
     * A parameter name, indicating time to wait if wait was requested, to pass as JMS message attribute
     */
    public static final String CLUSTER_MQ_PARAMETER_DELAY_MAX = "delayMax";

    /**
     * A parameter name, indicating the unit of time to wait if wait was requested, to pass as JMS message attribute
     */
    public static final String CLUSTER_MQ_PARAMETER_DELAY_UNIT = "delayUnit";

    @Inject
    private RoleService roleService;

    @Inject
    private ClusterEventPublisher clusterEventPublisher;

    @Inject
    private EndpointCacheContainerProvider endpointCacheContainerProvider;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    @Processed
    private Event<EndpointExecutionResult> endpointExecuted;

    /**
     * Create a new endpoint in database. Also create associated client and roles in keycloak.
     *
     * @param endpoint Endpoint to create
     */
    @Override
    public void create(Endpoint endpoint) throws BusinessException {
        validatePath(endpoint);
        super.create(endpoint);

        ParamBean paramBean = paramBeanFactory.getInstance();

        if (endpoint.isSecured()) {
            roleService.findOrCreateRole(endpoint.getRoleName(), new Role(paramBean.getProperty("role.accessAllCustomAPI", ROLE_NAME_ACCESS_ALL_CUSTOM_API), null, true, null));
        }

        endpointCacheContainerProvider.addEndpoint(endpoint);
        clusterEventPublisher.publishEvent(endpoint, ClusterEventActionEnum.create);
    }

    @Override
    public Endpoint update(Endpoint endpoint) throws BusinessException {
        validatePath(endpoint);
        endpoint = super.update(endpoint);

        ParamBean paramBean = paramBeanFactory.getInstance();

        if (endpoint.isSecured()) {
            roleService.findOrCreateRole(endpoint.getRoleName(), new Role(paramBean.getProperty("role.accessAllCustomAPI", ROLE_NAME_ACCESS_ALL_CUSTOM_API), null, true, null));
        } else {
            roleService.remove(new Role(endpoint.getRoleName(), null, true, null));
        }

        endpointCacheContainerProvider.updateEndpoint(endpoint);
        clusterEventPublisher.publishEvent(endpoint, ClusterEventActionEnum.update);

        return endpoint;
    }

    /**
     * Remove an endpoint from database. Also remove associated role in keycloak.
     *
     * @param endpoint Endpoint to remove
     */
    @Override
    public void remove(Endpoint endpoint) throws BusinessException {
        super.remove(endpoint);
        roleService.remove(new Role(endpoint.getRoleName(), null, true, null));

        endpointCacheContainerProvider.removeEndpoint(endpoint);
        clusterEventPublisher.publishEvent(endpoint, ClusterEventActionEnum.remove);
    }

    private void validatePath(Endpoint endpoint) throws BusinessException {
        /* check that the path is valid */
        if (endpoint.getPath() != null) {
            Matcher matcher = Endpoint.pathParamPattern.matcher(endpoint.getPath());
            int i = 0;
            while (matcher.find()) {
                String param = matcher.group();
                String paramName = param.substring(1, param.length() - 1);
                if (endpoint.getPathParameters().size() > i) {
                    String actualParam = endpoint.getPathParameters().get(i).toString();
                    if (!paramName.equals(actualParam)) {
                        throw new BusinessException(
                            endpoint.getCode() + " endpoint is invalid. " + (i + 1) + "the path param is expected to be " + endpoint.getPathParameters().get(i) + " while actual value is " + paramName);
                    }
                    i++;

                } else {
                    throw new BusinessException(endpoint.getCode() + " endpoint is invalid. Unexpected param " + paramName);
                }
            }

            if (endpoint.getPathParameters() != null && endpoint.getPathParameters().size() > i) {
                throw new BusinessException(endpoint.getCode() + " endpoint is invalid. Missing param " + endpoint.getPathParameters().get(i));
            }
        }
    }

    /**
     * Execute in asynchronous way a script associated to the endpoint
     *
     * @param execution Parameters of the execution
     * @return The pending result of the execution
     * @throws BusinessException if error occurs while execution
     */
    public String executeAsync(EndpointExecution endpointExecution) throws BusinessException, ExecutionException, InterruptedException {

        Endpoint endpoint = endpointExecution.getEndpoint();

        final String asyncId = UUID.randomUUID().toString();

        CompletableFuture<EndpointResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                EndpointResult result = execute(endpointExecution);
                endpointCacheContainerProvider.setExecutionResult(asyncId, result);

                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Initialize endpoint execution result
        endpointCacheContainerProvider.initializeExecutionResult(asyncId, future, endpoint.getId());
        log.info("Added pending execution number {} for endpoint {}", asyncId, endpoint.getCode());

        return asyncId;
    }

    /**
     * Execute a script associated to the endpoint
     *
     * @param endpointExecution Parameters of the execution
     * @return The result of the execution
     * @throws BusinessException if error occurs while execution
     */
    public EndpointResult execute(EndpointExecution endpointExecution) throws BusinessException, ExecutionException, InterruptedException {

        Map<String, Object> parameterMap = getScriptParameters(endpointExecution);
        EndpointExecutionResult executionResult = executeEndpointScript(endpointExecution, parameterMap);

        // Raise an event
        endpointExecuted.fire(executionResult);
        if (executionResult.getResults() != null) {
            String data = transformData(endpointExecution.getEndpoint(), executionResult.getResults());
            return new EndpointResult(data, endpointExecution.getEndpoint().getContentType().getValue());
        } else {
            return new EndpointResult(executionResult.getError());
        }
    }

    /**
     * Execute a script associated to the endpoint
     *
     * @param executionInformation Execution information
     * @return The result of the execution
     * @throws BusinessException if error occurs while execution
     */
    private EndpointExecutionResult executeEndpointScript(EndpointExecution executionInformation, Map<String, Object> parameterMap) throws InterruptedException, ExecutionException, BusinessException {

        EndpointExecutionResult executionResult = new EndpointExecutionResult(executionInformation.getEndpoint(), parameterMap);
        ScriptInstance scriptInstance = scriptInstanceService.findById(executionInformation.getEndpoint().getService().getId());
        ScriptPool poolConfig = scriptInstance.getPool();
        boolean usePool = poolConfig != null && poolConfig.isUsePool();

        // Start endpoint script with maximum execution timeout if one was set. Execution will be canceled if time has exceeded.
        if (executionInformation.getDelayMax() != null) {
            CompletableFuture<Map<String, Object>> resultFuture = null;
            try {
                resultFuture = CompletableFuture.supplyAsync(() -> {
                    try {

                        if (usePool) {
                            return scriptInstanceService.executePooled(scriptInstance.getCode(), parameterMap);
                        } else {
                            return scriptInstanceService.executeWInitAndFinalize(scriptInstance.getCode(), parameterMap);
                        }

                    } catch (CancellationException e) {
                        log.error("Endpoint execution was canceled as execution has timedout");
                        throw new RuntimeException(e);

                    } catch (BusinessException e) {
                        throw new RuntimeException(e);
                    }
                });
                Map<String, Object> scriptResult = resultFuture.get(executionInformation.getDelayMax(), executionInformation.getDelayUnit());
                executionResult.setResults(scriptResult);

            } catch (TimeoutException e) {
                executionResult.setError(new BusinessException("Timedout while waiting for an endpoint execution result"));
                log.info("Endpoint execution will be canceled as execution has exceeded {} {}", executionInformation.getDelayMax(), executionInformation.getDelayUnit());
                resultFuture.cancel(true);

            } catch (Exception e) {
                executionResult.setError(e);
            }

        } else {

            try {
                Map<String, Object> scriptResult = null;

                if (usePool) {
                    scriptResult = scriptInstanceService.executePooled(scriptInstance.getCode(), parameterMap);
                } else {
                    scriptResult = scriptInstanceService.executeWInitAndFinalize(scriptInstance.getCode(), parameterMap);
                }
                executionResult.setResults(scriptResult);

            } catch (Exception e) {
                executionResult.setError(e);
            }
        }

        return executionResult;
    }

    /**
     * Get script parameters from endpoint request and runtime context
     * 
     * @param executionInformation Endpoint execution information
     * @return A map of script parameters with their values
     */
    private Map<String, Object> getScriptParameters(EndpointExecution executionInformation) {

        Endpoint endpoint = executionInformation.getEndpoint();

        // Start with parameters passed to the endpoint
        Map<String, Object> parameterMap = new HashMap<>(executionInformation.getParameters());

        // Add parameters from the path
        if (endpoint.getPathParameters() != null && !endpoint.getPathParameters().isEmpty()) {

            Matcher matcher = endpoint.getPathRegex().matcher(executionInformation.getPathInfo());
            matcher.find();
            for (EndpointPathParameter pathParameter : endpoint.getPathParameters()) {
                try {
                    String val = matcher.group(pathParameter.toString());
                    parameterMap.put(pathParameter.getScriptParameter(), val);
                } catch (Exception e) {
                    throw new IllegalArgumentException("cannot find param " + pathParameter + " in " + executionInformation.getPathInfo());
                }
            }
        }

        // Assign query or request body parameters
        if (endpoint.getParametersMapping() != null && !endpoint.getParametersMapping().isEmpty()) {

            for (EndpointParameterMapping paramMapping : endpoint.getParametersMapping()) {

                String paramName = paramMapping.getParameterName() != null ? paramMapping.getParameterName() : paramMapping.getScriptParameter();

                Object parameterValue = executionInformation.getParameters().get(paramName);

                // Use default value if parameter not provided
                if (parameterValue == null) {
                    parameterValue = paramMapping.getDefaultValue();
                } else {
                    // Handle cases where parameter is multivalued
                    if (parameterValue instanceof String[]) {
                        String[] arrValue = (String[]) parameterValue;
                        if (paramMapping.isMultivaluedAsBoolean()) {
                            parameterValue = new ArrayList<>(Arrays.asList(arrValue));
                        } else if (arrValue.length == 1) {
                            parameterValue = arrValue[0];
                        } else {
                            throw new IllegalArgumentException("Parameter " + paramName + " should not be multivalued");
                        }
                    } else if (parameterValue instanceof Collection) {
                        @SuppressWarnings("rawtypes")
                        Collection colValue = (Collection) parameterValue;
                        if (!paramMapping.isMultivaluedAsBoolean() && colValue.size() == 1) {
                            parameterValue = colValue.iterator().next();
                        } else if (!paramMapping.isMultivaluedAsBoolean()) {
                            throw new IllegalArgumentException("Parameter " + paramName + " should not be multivalued");
                        }
                    }
                }
                // Replace parameter name with script parameter name
                parameterMap.remove(paramName);
                parameterMap.put(paramMapping.getScriptParameter(), parameterValue);
            }
        }

        // Set budget variables
        parameterMap.put(EndpointVariables.MAX_BUDGET, executionInformation.getBudgetMax());
        parameterMap.put(EndpointVariables.BUDGET_UNIT, executionInformation.getBudgetUnit());
        parameterMap.put(EndpointVariables.MAX_DELAY, executionInformation.getDelayMax());
        parameterMap.put(EndpointVariables.DELAY_UNIT, executionInformation.getDelayUnit());

        // Implicitly pass the request and response information to the script
        parameterMap.put(EndpointVariables.REQUEST, executionInformation.getRequest());
        if (endpoint.isSynchronous()) {
            parameterMap.put(EndpointVariables.RESPONSE, executionInformation.getResponse());
        }

        return parameterMap;
    }

    /**
     * Extract variable pointed by returned variable name and apply JSONata query if defined If endpoint is not configured to serialize the result and that returned variable name is set, do not serialize result.
     * Otherwise serialize it.
     *
     * @param endpoint Endpoint endpoxecuted
     * @param result Result of the endpoint execution
     * @return the transformed JSON result if JSONata query was defined or the serialized result if query was not defined.
     */
    public String transformData(Endpoint endpoint, Map<String, Object> result) {
        final boolean returnedVarNameDefined = !StringUtils.isBlank(endpoint.getReturnedVariableName());
        boolean shouldSerialize = !returnedVarNameDefined || endpoint.isSerializeResult();

        Object returnValue = "";

        @SuppressWarnings("unchecked")
        Map<String, Object> scriptGetterValues = (Map<String, Object>) result.get(Script.CONTEXT_RESULT_GETTER_VALUES);

        // Get a concrete value from response
        if (returnedVarNameDefined) {
            Object extractedValue = null;
            // Give a priority to getter values and if not found - to context parameters
            if (scriptGetterValues != null && scriptGetterValues.containsKey(endpoint.getReturnedVariableName())) {
                extractedValue = scriptGetterValues.get(endpoint.getReturnedVariableName());
            } else {
                extractedValue = result.get(endpoint.getReturnedVariableName());
            }
            if (extractedValue != null) {
                returnValue = extractedValue;
            } else {
                log.warn("[Endpoint {}] Variable {} cannot be extracted from context", endpoint.getCode(), endpoint.getReturnedVariableName());
            }

            // Or use all returned values, that can be serialized
        } else {
            List<String> paramsToRemove = Arrays.asList(EndpointVariables.MAX_BUDGET, EndpointVariables.BUDGET_UNIT, EndpointVariables.MAX_DELAY, EndpointVariables.DELAY_UNIT, EndpointVariables.REQUEST,
                EndpointVariables.RESPONSE, Script.CONTEXT_APP_PROVIDER, Script.CONTEXT_CURRENT_USER, Script.CONTEXT_ENTITY);
            Map<String, Object> serializableResult = new HashMap<String, Object>();
            for (Entry<String, Object> entry : result.entrySet()) {

                if (entry.getValue() instanceof Serializable && !paramsToRemove.contains(entry.getKey())) {
                    serializableResult.put(entry.getKey(), entry.getValue());
                }
            }
            returnValue = serializableResult;
        }

        // Use JSONata transporter if applicable
        if (!StringUtils.isBlank(endpoint.getJsonataTransformer())) {
            try {
                return Expression.jsonata(endpoint.getJsonataTransformer()).evaluate(JacksonUtil.toJsonNode(returnValue)).toPrettyString();
            } catch (Exception e) {
                throw new BusinessException("Unable to transform result " + returnValue + " with expression " + endpoint.getJsonataTransformer(), e);
            }
        }

        // Serialize the value or return it as is
        if (shouldSerialize) {
            return JacksonUtil.toStringPrettyPrinted(returnValue);
        } else {
            return returnValue.toString();
        }
    }

    /**
     * Get endpoint execution result given the asynchronous operation ID
     * 
     * @param asyncId Asynchronous operation ID
     * @param isCancel Shall asynchronous operation be canceled
     * @param isKeep Should a response be preserved in cache even if it was retrieved already
     * @param isWait Shall wait for an execution to finish
     * @param delayMax Maximum time to wait if requested so
     * @param delayUnit Time unit to wait if requested so
     * @return Endpoint result
     */
    public EndpointResult getOrWaitForEndpointExecutionResult(String asyncId, boolean isCancel, boolean isKeep, boolean isWait, Long delayMax, TimeUnit delayUnit) {

        CompletableFuture<EndpointResult> future = endpointCacheContainerProvider.getExecutionFuture(asyncId);
        PendingResult pendingResult = endpointCacheContainerProvider.getExecutionResult(asyncId);

        // Endpoint result is already available, just return it
        if (pendingResult.getExecutionResult() != null) {
            EndpointResult result = pendingResult.getExecutionResult();
            if (!isKeep) {
                removeExecutionResult(asyncId);
            }
            return result;

            // Endpoint script in not being executed on this cluster node, so nothing to return
        } else if (future == null) {
            return null;
        }

        try {

            if (future.isCancelled()) {
                return new EndpointResult(EndpointResponseStatusEnum.CANCELED);
            } else if (isCancel) {
                future.cancel(true);
                return new EndpointResult(EndpointResponseStatusEnum.CANCELED);
            }

            EndpointResult endpointResult = null;

            // Wait for max delay if defined
            if (delayMax != null) {
                endpointResult = future.get(delayMax, delayUnit);

                // Asynchronous execution was completed or was requested to wait indefinitely
            } else if (future.isDone() || isWait) {
                endpointResult = future.get();
            }

            // Return execution result and remove it from cache unless asked to preserve it
            if (endpointResult != null) {
                if (!isKeep) {
                    log.info("Removing execution results for asyncronous request id {}", asyncId);
                    removeExecutionResult(asyncId);
                }
                return endpointResult;

                // Execution was not finished
            } else {
                return new EndpointResult(EndpointResponseStatusEnum.IN_PROGRESS);
            }

        } catch (TimeoutException e) {
            return new EndpointResult(EndpointResponseStatusEnum.TIMED_OUT);

        } catch (Exception e) {
            log.error("Error while executing request {}", asyncId, e);
            return new EndpointResult(e);
        }
    }

    /**
     * Remove execution result
     * 
     * @param asyncId Asynchronous operation id
     */
    public void removeExecutionResult(String asyncId) {
        endpointCacheContainerProvider.removeExecutionResult(asyncId);
    }
}