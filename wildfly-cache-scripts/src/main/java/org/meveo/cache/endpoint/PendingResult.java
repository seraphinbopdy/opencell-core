/**
 * 
 */
package org.meveo.cache.endpoint;

import java.io.Serializable;

/**
 * [Pending] result of an endpoint execution.
 * 
 * @author clement.bareth
 * @author Andrius Karpavicius
 */
public class PendingResult implements Serializable {

    private static final long serialVersionUID = 1665526550131647309L;

    /**
     * The id of the endpoint.
     */
    private Long endpointId;

    /**
     * Asynchronous operation ID - also used as a cache key
     */
    private String asyncId;

    /**
     * Execution result
     */
    private EndpointResult executionResult;

    /**
     * Constructor.
     */
    public PendingResult() {
    }

    /**
     * Constructor
     * 
     * @param endpointId Endpoint ID
     * @param asyncId Asynchronous operation ID
     */
    public PendingResult(Long endpointId, String asyncId) {
        this.endpointId = endpointId;
        this.asyncId = asyncId;
    }

    /**
     * @return Id of the endpoint.
     */
    public Long getEndpointId() {
        return endpointId;
    }

    /**
     * @param endpointId Id of the endpoint.
     */
    public void setEndpointId(Long endpointId) {
        this.endpointId = endpointId;
    }

    /**
     * @return Asynchronous operation ID - also used as a cache key
     */
    public String getAsyncId() {
        return asyncId;
    }

    /**
     * @param asyncId Asynchronous operation ID - also used as a cache key
     */
    public void setAsyncId(String asyncId) {
        this.asyncId = asyncId;
    }

    /**
     * @return Execution result
     */
    public EndpointResult getExecutionResult() {
        return executionResult;
    }

    /**
     * @param result Execution result
     */
    public void setExecutionResult(EndpointResult executionResult) {
        this.executionResult = executionResult;
    }

    @Override
    public String toString() {
        return "Endpoint id " + endpointId + (executionResult == null ? ", No result yet." : (", " + executionResult.toString()));
    }
}