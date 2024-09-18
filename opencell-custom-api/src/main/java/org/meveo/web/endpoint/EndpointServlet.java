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

package org.meveo.web.endpoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.endpoint.resource.Headers;
import org.meveo.api.endpoint.service.EndpointApi;
import org.meveo.api.endpoint.service.EndpointExecution;
import org.meveo.api.endpoint.service.EndpointExecutionBuilder;
import org.meveo.api.endpoint.service.EndpointRequest;
import org.meveo.api.endpoint.service.EndpointResponse;
import org.meveo.cache.endpoint.EndpointResult;
import org.meveo.cache.endpoint.EndpointResult.EndpointResponseStatusEnum;
import org.meveo.cache.endpoint.PendingResult;
import org.meveo.commons.utils.EjbUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.event.monitoring.ClusterEventDto.ClusterEventActionEnum;
import org.meveo.event.monitoring.ClusterEventPublisher;
import org.meveo.model.endpoint.Endpoint;
import org.meveo.model.endpoint.EndpointHttpMethod;
import org.meveo.model.endpoint.EndpointParameterMapping;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.service.endpoint.EndpointCacheContainerProvider;
import org.meveo.service.endpoint.EndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.core.MediaType;

/**
 * Servlet that allows to execute technical services through configured endpoints.<br>
 * The first part of Uri after "/rest/" corresponds either to the first part of the path of the endpoint, or an id of a previous asynchronous execution.<br>
 * The last part or the Uri corresponds to the path parameters of the endpoint.<br>
 * If the endpoint is configured as GET, it should be called via GET resquests and parameters should be in query.<br>
 * If the endpoint is configured as POST/PUT, it should be called via POST/PUT requests and parameters should be in body as a JSON map.<br>
 * Header "Keep-data" indicates we don't want to remove the execution result from cache.<br>
 * Header "Wait-For-Finish" indicates that we want to wait until one exuction finishes and get results after. (Otherwise returns status 102).<br>
 * Header "Persistence-Context-Id" indiciates the id of the persistence context we want to save the result
 * 
 * @author clement.bareth
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.10
 */
@WebServlet("/api/rest/custom/*")
@MultipartConfig
public class EndpointServlet extends HttpServlet {

    private static final long serialVersionUID = -8425320629325242067L;

    /**
     * A REST path to access custom endpoints
     */
    public static final String REST_PATH = "/api/rest/custom/";

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {
    };

    private static Logger log = LoggerFactory.getLogger(EndpointServlet.class);

    @Inject
    private EndpointApi endpointApi;

    @Inject
    private EndpointCacheContainerProvider endpointCacheContainer;

    @Inject
    private EndpointService endpointService;

    @Inject
    private ClusterEventPublisher clusterEventPublisher;

    private void returnError(HttpServletResponse resp, int status, String error, String message, String details) {
        resp.setStatus(status);
        resp.setContentType(MediaType.APPLICATION_JSON);
        JSONObject jsonObject = new JSONObject();
        if (error != null) {
            jsonObject.put("error", error);
        }
        if (message != null) {
            jsonObject.put("message", message);
        }
        if (details != null) {
            jsonObject.put("details", details);
        }
        try {
            resp.getWriter().println(jsonObject.toString());
        } catch (IOException e) {
            log.error("Error while writing error response", e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            doRequest(req, resp, null, EndpointHttpMethod.DELETE);

        } catch (Exception e) {
            returnError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), e.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPutPost(req, resp, EndpointHttpMethod.POST);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPutPost(req, resp, EndpointHttpMethod.PUT);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String method = req.getMethod();

        if (method.equals(EndpointHttpMethod.PATCH.name())) {
            this.doPatch(req, resp);

        } else {
            super.service(req, resp);
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doRequest(req, resp, null, EndpointHttpMethod.PATCH);

        } catch (Exception e) {
            returnError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), e.toString());
        }
    }

    protected void doPutPost(HttpServletRequest req, HttpServletResponse resp, EndpointHttpMethod method) throws ServletException, IOException {
        Map<String, Object> parameters = new HashMap<>();
        String contentType = req.getHeader("Content-Type");
        try {
            if (contentType != null && contentType.startsWith(MediaType.MULTIPART_FORM_DATA)) {
                Collection<Part> parts = req.getParts();
                for (var part : parts) {
                    Object partValue;
                    if (part.getContentType() != null && part.getContentType().startsWith(MediaType.APPLICATION_JSON)) {
                        try {
                            partValue = JacksonUtil.read(part.getInputStream(), MAP_TYPE_REFERENCE);
                        } catch (JsonParseException e) {
                            returnError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot parse part '" + part.getName() + "'' is not a well formed json", e.getMessage(), e.getRequestPayloadAsString());
                            return;
                        } catch (Exception e) {
                            returnError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error parsing part '" + part.getName() + "'", e.getMessage(), e.toString());
                            return;
                        }
                    } else if (part.getContentType() != null && part.getContentType().startsWith(MediaType.APPLICATION_XML)) {
                        XmlMapper xmlMapper = new XmlMapper();
                        try {
                            partValue = xmlMapper.readValue(part.getInputStream(), MAP_TYPE_REFERENCE);
                        } catch (JsonParseException e) {
                            returnError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot parse part '" + part.getName() + "'' is not a well formed xml", e.getMessage(), e.getRequestPayloadAsString());
                            return;
                        } catch (Exception e) {
                            returnError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error parsing part '" + part.getName() + "'", e.getMessage(), e.toString());
                            return;
                        }
                    } else if (part.getSubmittedFileName() != null) {
                        partValue = part.getInputStream();
                    } else {
                        partValue = IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8);
                    }
                    parameters.put(part.getName(), partValue);
                }

            } else {
                String requestBody = StringUtils.readBuffer(req.getReader());
                parameters.put("REQUEST_BODY", requestBody);
                if (!StringUtils.isBlank(requestBody) && contentType != null) {
                    if (contentType.startsWith(MediaType.APPLICATION_JSON)) {
                        try {
                            parameters = JacksonUtil.read(requestBody, MAP_TYPE_REFERENCE);
                        } catch (JsonParseException e) {
                            returnError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot parse body, it is not a well formed json", e.getMessage(), e.getRequestPayloadAsString());
                            return;
                        } catch (Exception e) {
                            returnError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error parsing body", e.getMessage(), e.toString());
                            return;
                        }
                    } else if (contentType.startsWith(MediaType.APPLICATION_XML) || contentType.startsWith(MediaType.TEXT_XML)) {
                        XmlMapper xmlMapper = new XmlMapper();
                        try {
                            parameters = xmlMapper.readValue(requestBody, MAP_TYPE_REFERENCE);
                        } catch (JsonParseException e) {
                            returnError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot parse body, it is not a well formed xml", e.getMessage(), e.getRequestPayloadAsString());
                            return;
                        } catch (Exception e) {
                            returnError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error parsing body", e.getMessage(), e.toString());
                            return;
                        }
                    }
                }
            }

            doRequest(req, resp, parameters, method);

        } catch (Exception e) {
            returnError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing query", e.getMessage(), e.toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doRequest(req, resp, null, EndpointHttpMethod.GET);

        } catch (Exception e) {
            returnError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing query", e.getMessage(), e.toString());
        }
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doRequest(req, resp, null, EndpointHttpMethod.HEAD);

        } catch (Exception e) {
            returnError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing query", e.getMessage(), e.toString());
        }
    }

    /**
     * Process the request. Either launch processing of a script or retrieve a result of a previous asynchronous operation
     * 
     * @param req Http request
     * @param resp Http response
     * @param parameters Parameters passed in POST/PUT request body
     * @param method Http request method
     * @throws IOException IO exception
     * @throws ServletException Unclear URL - no path
     */
    private void doRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> parameters, EndpointHttpMethod method) throws IOException, ServletException {

        EndpointExecution endpointExecution = getExecutionBuilder(req, resp).setParameters(parameters).setMethod(method).createEndpointExecution();

        // Retrieve endpoint
        final Endpoint endpoint = endpointExecution.getEndpoint();

        try {
            if (endpoint == null) {
                endpointExecution.getResp().setStatus(404);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "Endpoint or asynchronous result was not found");
                endpointExecution.getResp().getWriter().print(jsonObject.toString());
                return;
            }

            // If endpoint security is enabled, check if user has right to access that particular endpoint
            boolean endpointSecurityEnabled = Boolean.parseBoolean(ParamBean.getInstance().getProperty("endpointSecurityEnabled", "true"));
            if (endpointSecurityEnabled && !endpointApi.isUserAuthorized(endpoint)) {
                endpointExecution.getResp().setStatus(403);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "You are not authorized to access this endpoint");
                endpointExecution.getResp().getWriter().print(jsonObject.toString());
                return;
            }

            // Return asynchronous result if thats what was requested
            if (endpointExecution.getPendingResult() != null) {
                returnAsyncronousResults(endpointExecution);
                return;
            }

            // Proceed to execute api call

            // Check if a required parameter is missing at endpoint execution
            if (CollectionUtils.isNotEmpty(endpoint.getParametersMapping())) {
                List<EndpointParameterMapping> requiredParameters = new ArrayList<>();
                for (EndpointParameterMapping tsParameterMapping : endpoint.getParametersMapping()) {
                    if (tsParameterMapping.isValueRequiredAsBoolean() && tsParameterMapping.getDefaultValue() == null) {
                        requiredParameters.add(tsParameterMapping);
                    }
                }

                if (CollectionUtils.isNotEmpty(requiredParameters)) {
                    for (EndpointParameterMapping param : requiredParameters) {
                        String parameterName = param.getScriptParameter();
                        // if there's an exposed parameter name :
                        if (param.getParameterName() != null) {
                            parameterName = param.getParameterName();
                        }
                        if (!endpointExecution.getParameters().containsKey(parameterName)) {
                            endpointExecution.getResp().setStatus(400);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("error", "Parameter '" + parameterName + "' is missing");
                            endpointExecution.getResp().getWriter().println(jsonObject.toString());
                            return;
                        }
                    }
                }
            }

            launchEndpoint(endpointExecution);

        } catch (Exception e) {
            log.error("Error while executing request", e);
            returnError(endpointExecution.getResp(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while executing request", e.getMessage(), e.toString());

        } finally {
            if (endpointExecution.getResponse().getInputStream() == null) {
                endpointExecution.getResp().getWriter().flush();
                endpointExecution.getResp().getWriter().close();
            }
        }
    }

    /**
     * Execute API either in synchronous or asynchronous mode
     * 
     * @param endpointExecution Endpoint execution, including related variables, information
     * @throws BusinessException Exception while executing the request
     * @throws ExecutionException Concurrency exception
     * @throws InterruptedException Asynchronous execution interrupted
     * @throws IOException IO exception
     */
    private void launchEndpoint(EndpointExecution endpointExecution) throws BusinessException, ExecutionException, InterruptedException, IOException {

        final Endpoint endpoint = endpointExecution.getEndpoint();

        // If endpoint is synchronous or asynchronous but request indicated to wait, execute the script straight and return the response
        if (endpoint.isSynchronous() || endpointExecution.isWait()) {
            EndpointResult endpointResult = endpointService.execute(endpointExecution);
            handleResponse(endpointResult, endpointExecution);
            return;

        } else {
            launchEndpointAsync(endpointExecution);
        }
    }

    /**
     * Handle respose - error or result
     * 
     * @param endpointResult Endpoint execution result
     * @param endpointExecution Endpoint execution
     * @throws IOException Failed to write response
     */
    private void handleResponse(EndpointResult endpointResult, EndpointExecution endpointExecution) throws IOException {

        if (endpointResult == null) {
            returnError(endpointExecution.getResp(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Asynchronous response is no longer available", null, null);

        } else {
            if (endpointResult.getExecutionResponseStatus() == EndpointResponseStatusEnum.IN_PROGRESS) {
                endpointExecution.getResp().getWriter().print("In progress");
                endpointExecution.getResp().setStatus(202);

            } else if (endpointResult.getExecutionResponseStatus() == EndpointResponseStatusEnum.CANCELED) {
                endpointExecution.getResp().getWriter().print("Canceled");
                endpointExecution.getResp().setStatus(202);

            } else if (endpointResult.getExecutionResponseStatus() == EndpointResponseStatusEnum.TIMED_OUT) {
                returnError(endpointExecution.getResp(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Timeout while waiting for asynchronous result", null, null);

            } else if (endpointResult.getError() != null) {
                returnError(endpointExecution.getResp(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while executing request", endpointResult.getError().getMessage(), endpointResult.getError().toString());

            } else {
                setReponse(endpointResult.getResult(), endpointExecution);
            }
        }
        if (endpointExecution.getResponse().getInputStream() == null) {
            endpointExecution.getResp().getWriter().flush();
            endpointExecution.getResp().getWriter().close();
        }
    }

    /**
     * Execute API either in asynchronous mode and return an ID of operation, so it can be used to retrieve the result later
     * 
     * @param endpointExecution Endpoint execution, including related variables, information
     * @throws BusinessException Exception while executing the request
     * @throws ExecutionException Concurrency exception
     * @throws InterruptedException Asynchronous execution interrupted
     * @throws IOException IO exception
     */
    private void launchEndpointAsync(EndpointExecution endpointExecution) throws BusinessException, ExecutionException, InterruptedException, IOException {

        // Execute the endpoint asynchronously
        String asyncId = endpointService.executeAsync(endpointExecution);

        // Return the id of the execution so the user can retrieve it later
        endpointExecution.getResp().getWriter().println(asyncId);
        endpointExecution.getResp().setStatus(202); // Accepted
        return;
    }

    private void setReponse(String transformedResult, EndpointExecution endpointExecution) throws IOException {
        // HTTP Status

        EndpointResponse response = endpointExecution.getResponse();
        HttpServletResponse servletResponse = endpointExecution.getResp();
        Integer status = response.getStatus();
        if (status != null) {
            servletResponse.setStatus(status);
        } else {
            servletResponse.setStatus(200); // OK
        }

        // Content type
        String contentType = response.getContentType();
        if (!StringUtils.isBlank(contentType)) {
            servletResponse.setContentType(contentType);
        } else {
            servletResponse.setContentType(endpointExecution.getEndpoint().getContentType().getValue());
        }

        // Buffer size
        Integer bufferSize = response.getBufferSize();
        if (bufferSize != null) {
            servletResponse.setBufferSize(bufferSize);
        }

        // Headers
        Map<String, String> headers = response.getHeaders();
        if (headers != null) {
            headers.forEach(servletResponse::setHeader);
        }

        // Date Headers
        Map<String, Long> dateHeaders = response.getDateHeaders();
        if (dateHeaders != null) {
            dateHeaders.forEach(servletResponse::setDateHeader);
        }

        // Body of the response
        String errorMessage = response.getErrorMessage();
        if (!StringUtils.isBlank(errorMessage)) { // Priority to error message
            servletResponse.getWriter().print(errorMessage);
        } else if (response.getInputStream() != null) { // Output has been set
            ServletOutputStream out = servletResponse.getOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = response.getInputStream().read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
            out.close();
        } else { // Use the endpoint script's result
            servletResponse.getWriter().print(transformedResult);
        }
    }

    private EndpointExecutionBuilder getExecutionBuilder(HttpServletRequest req, HttpServletResponse resp) throws ServletException {

        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo.length() == 0) {
            throw new ServletException("Incomplete URL");
        }

        String[] pathInfoParts = pathInfo.split("/");

        PendingResult pendingExecution = null;
        Endpoint endpoint = null;

        // Check if the parameter is a UUID of a asynchronous response
        if (pathInfoParts.length == 2) {
            String asyncId = pathInfoParts[1];
            pendingExecution = endpointCacheContainer.getExecutionResult(asyncId);
            if (pendingExecution != null && pendingExecution.getEndpointId() != null) {
                endpoint = endpointService.findById(pendingExecution.getEndpointId(), Arrays.asList("pathParameters"));
            }
        }

        // If not async response lookup, retrieve endpoint by best match of path and method
        if (endpoint == null) {
            endpoint = endpointCacheContainer.getEndpointForPath(req.getPathInfo(), req.getMethod());
        }

        return new EndpointExecutionBuilder().setRequest(new EndpointRequest(req, endpoint)).setResponse(resp).setEndpoint(endpoint).setPathInfo(pathInfo).setPendingResult(pendingExecution)
            .setKeep(Headers.KEEP_DATA.getValue(req, Boolean.class, false)).setWait(Headers.WAIT_FOR_FINISH.getValue(req, Boolean.class, false)).setBudgetUnit(Headers.BUDGET_UNIT.getValue(req, String.class))
            .setBugetMax(Headers.BUDGET_MAX_VALUE.getValue(req, Double.class)).setDelayUnit(Headers.DELAY_UNIT.getValue(req, TimeUnit.class, TimeUnit.SECONDS))
            .setDelayValue(Headers.DELAY_MAX_VALUE.getValue(req, Long.class)).setPersistenceContextId(Headers.PERSISTENCE_CONTEXT_ID.getValue(req));
    }

    /**
     * Respond with asynchronous execution results
     * 
     * @param endpointExecution Endpoint execution information
     * @throws IOException IO related exceptions
     */
    private void returnAsyncronousResults(EndpointExecution endpointExecution) throws IOException {

        PendingResult pendingExecution = endpointExecution.getPendingResult();

        // Result is already available
        EndpointResult endpointResult = pendingExecution.getExecutionResult();
        if (endpointResult != null) {
            if (!endpointExecution.isKeep()) {
                endpointService.removeExecutionResult(pendingExecution.getAsyncId());
            }
            // Not available yet, so get/wait for it
        } else {
            if (EjbUtils.isRunningInClusterMode()) {

                Map<String, Object> resultLookUpParams = new HashMap<String, Object>();
                resultLookUpParams.put(EndpointService.CLUSTER_MQ_PARAMETER_ASYNC_ID, pendingExecution.getAsyncId());
                resultLookUpParams.put(EndpointService.CLUSTER_MQ_PARAMETER_IS_CANCEL, endpointExecution.isCancel());
                resultLookUpParams.put(EndpointService.CLUSTER_MQ_PARAMETER_IS_KEEP, endpointExecution.isKeep());
                resultLookUpParams.put(EndpointService.CLUSTER_MQ_PARAMETER_IS_WAIT, endpointExecution.isWait());
                resultLookUpParams.put(EndpointService.CLUSTER_MQ_PARAMETER_DELAY_MAX, endpointExecution.getDelayMax());
                resultLookUpParams.put(EndpointService.CLUSTER_MQ_PARAMETER_DELAY_UNIT, endpointExecution.getDelayUnit());

                // Wait for response from another node for indicated delay time plus two seconds for inter-node communication. Wait indefinatelly if Wait was requested
                Long waitTime = endpointExecution.isWait() ? -1L : null;
                if (endpointExecution.getDelayMax() != null) {
                    waitTime = 2000L + endpointExecution.getDelayUnit().toMillis(endpointExecution.getDelayMax());
                }
                endpointResult = (EndpointResult) clusterEventPublisher.publishEvent(endpointExecution.getEndpoint(), ClusterEventActionEnum.getEndpointExecutionResult, resultLookUpParams, true,
                    pendingExecution.getAsyncId(), waitTime);

            } else {
                endpointResult = endpointService.getOrWaitForEndpointExecutionResult(pendingExecution.getAsyncId(), endpointExecution.isCancel(), endpointExecution.isKeep(), endpointExecution.isWait(),
                    endpointExecution.getDelayMax(), endpointExecution.getDelayUnit());
            }
        }

        handleResponse(endpointResult, endpointExecution);
    }
}