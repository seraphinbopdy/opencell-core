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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

import org.infinispan.Cache;
import org.meveo.cache.CacheContainerProvider;
import org.meveo.cache.endpoint.EndpointResult;
import org.meveo.cache.endpoint.PendingResult;
import org.meveo.model.endpoint.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.inject.Inject;

/**
 * Provides cache related services (storing, loading, update) for endpoint related operations
 * 
 * @author Clément Bareth *
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @author Andrius Karpavicius
 */
public class EndpointCacheContainerProvider implements CacheContainerProvider, Serializable {

    private static final long serialVersionUID = -4257685171131361758L;

    @Inject
    private static Logger log = LoggerFactory.getLogger(EndpointCacheContainerProvider.class);

    /**
     * Cache of endpoint execution results
     */
    @Resource(lookup = "java:jboss/infinispan/cache/opencell/opencell-endpoint-results")
    private Cache<String, PendingResult> executionResults;

    /*
     * Map of Endpoints by code
     */
    @Resource(lookup = "java:jboss/infinispan/cache/opencell/opencell-endpoints")
    private Cache<String, Endpoint> endpointsByCode;

    /**
     * Futures of the execution of the endpoint. Asynchronous operation ID is the map key.
     */
    private static Map<String, CompletableFuture<EndpointResult>> executionFutures = new HashMap<String, CompletableFuture<EndpointResult>>();

    @Inject
    private EndpointService endpointService;

    /**
     * Get endpoint execution result by asynchronous operation ID
     * 
     * @param asyncId Asynchronous operation ID
     * @return Execution result information
     */
    public PendingResult getExecutionResult(String asyncId) {
        return executionResults.get(asyncId);
    }

    /**
     * Remove execution result by asynchronous operation ID
     * 
     * @param asyncId Asynchronous operation ID
     */
    public void removeExecutionResult(String asyncId) {
        executionResults.remove(asyncId);
    }

    /**
     * Initialize endpoint execution result cache
     * 
     * @param asyncId Asynchronous operation ID
     * @param future Future of asynchronous operation execution
     * @param endpointId Endpoint ID
     * @return Pending result
     */
    public void initializeExecutionResult(String asyncId, CompletableFuture<EndpointResult> future, Long endpointId) {

        // Just in case if script has completed before the cache was populated, supply missing information
        if (executionResults.containsKey(asyncId)) {
            executionResults.get(asyncId).setEndpointId(endpointId);

        } else {
            PendingResult pendingResult = new PendingResult(endpointId, asyncId);
            executionResults.put(asyncId, pendingResult);
        }
        executionFutures.put(asyncId, future);
    }

    /**
     * Removed cached endpoint by code
     * 
     * @param endpoint Endpoint to remove
     */
    public void removeEndpoint(Endpoint endpoint) {
        endpointsByCode.remove(endpoint.getCode());
    }

    /**
     * Update cached endpoint information
     * 
     * @param endpoint Endpoint to update
     */
    public void updateEndpoint(Endpoint endpoint) {
        if (endpoint.isActive()) {
            endpointsByCode.put(endpoint.getCode(), endpoint);
        } else {
            if (endpointsByCode.containsKey(endpoint.getCode())) {
                endpointsByCode.remove(endpoint.getCode());
            }
        }
    }

    /**
     * Add endpoint to cache by code
     * 
     * @param endpoint Endpoint to add
     */
    public void addEndpoint(Endpoint endpoint) {
        if (endpoint.isActive()) {
            endpointsByCode.put(endpoint.getCode(), endpoint);
        }
    }

    /**
     * Find the endpoint with largest regex matching the path
     * 
     * @param path Url path
     * @param method Http method
     * @return Endpoint matched
     */
    public Endpoint getEndpointForPath(String path, String method) {

        if (endpointsByCode.isEmpty()) {
            populateCache(null);
        }

        Endpoint result = null;
        Iterator<Map.Entry<String, Endpoint>> it = endpointsByCode.entrySet().iterator();
        while (it.hasNext()) {
            Endpoint endpoint = it.next().getValue();
            if (endpoint.getMethod().getLabel().equals(method)) {
                if (path.startsWith("/" + endpoint.getBasePath())) {
                    Matcher matcher = endpoint.getPathRegex().matcher(path);
                    if (matcher.matches() || matcher.lookingAt()) {
                        if ((result == null) || (endpoint.getPathRegex().pattern().length() > result.getPathRegex().pattern().length())) {
                            result = endpoint;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get a summary of cached information.
     * 
     * @return A map containing cache information with cache name as a key and cache as a value
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Cache> getCaches() {
        Map<String, Cache> summaryOfCaches = new HashMap<String, Cache>();
        summaryOfCaches.put(executionResults.getName(), executionResults);
        summaryOfCaches.put(endpointsByCode.getName(), endpointsByCode);

        return summaryOfCaches;
    }

    /**
     * Refresh cache by name. Removes current provider's data from cache and populates it again
     * 
     * @param cacheName Name of cache to refresh or null to refresh all caches
     */
    @Override
    @Asynchronous
    public void refreshCache(String cacheName) {

        // Refrsh endpointsByCode cache
        if (cacheName == null || cacheName.equals(endpointsByCode.getName()) || cacheName.contains(endpointsByCode.getName())) {
            endpointsByCode.clear();
            populateCache(cacheName);
        }

        // Clear executionResults cache and executionFutures collection. There is nothing to be refreshed
        if (cacheName == null || cacheName.equals(executionResults.getName()) || cacheName.contains(executionResults.getName())) {
            executionResults.clear();
            for (Entry<String, CompletableFuture<EndpointResult>> futureInfo : executionFutures.entrySet()) {
                CompletableFuture<EndpointResult> future = futureInfo.getValue();
                if (!future.isDone()) {
                    try {
                        future.cancel(true);
                    } catch (Exception e) {
                        log.error("Failed to cancel a future for asyncronous operation {}", futureInfo.getKey(), e);
                    }
                }
            }
            executionFutures.clear();
        }
    }

    /**
     * Populate cache by name
     * 
     * @param cacheName Name of cache to populate or null to populate all caches
     */
    @Override
    public void populateCache(String cacheName) {

        if (cacheName == null || cacheName.equals(endpointsByCode.getName()) || cacheName.contains(endpointsByCode.getName())) {

            log.debug("Start to pre-populate Endpoint cache");

            List<Endpoint> allEndpoints = endpointService.list();
            for (Endpoint endpoint : allEndpoints) {
                // Lazy load properties
                endpoint.getService().getCode();
                if (endpoint.getPathParameters() != null) {
                    endpoint.getPathParameters().forEach(e -> {
                    });
                }
                endpointService.detach(endpoint);
                endpointsByCode.put(endpoint.getCode(), endpoint);
            }

            log.info("Endpoint cache populated with {} values", allEndpoints.size());
        }
    }

    /**
     * Add execution result to cache once script has terminated
     * 
     * @param asyncId Asynchronous endpoint operation ID
     * @param result Endpoint execution id
     */
    public void setExecutionResult(String asyncId, EndpointResult result) {
        executionFutures.remove(asyncId);
        PendingResult pendingResult = null;
        if (executionResults.containsKey(asyncId)) {
            pendingResult = executionResults.get(asyncId);
            pendingResult.setExecutionResult(result);

            // Covers a case when script execution in asynchronous way occurred faster than cache population
        } else {
            pendingResult = new PendingResult();
            pendingResult.setExecutionResult(result);
            pendingResult.setAsyncId(asyncId);
        }
        executionResults.put(asyncId, pendingResult);
    }

    /**
     * @param asyncId Asynchronous endpoint operation ID
     * @return Future of the execution of the endpoint
     */
    public CompletableFuture<EndpointResult> getExecutionFuture(String asyncId) {

        return executionFutures.get(asyncId);
    }
}