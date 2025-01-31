package org.meveo.service.script;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.Cache;
import org.meveo.cache.CacheContainerProvider;
import org.meveo.cache.CacheKeyStr;

import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.inject.Inject;

/**
 * Provides cache related services (loading, update) for script related operations
 */
public class ScriptCacheContainerProvider implements CacheContainerProvider, Serializable {

    private static final long serialVersionUID = -1406610327757896373L;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    /**
     * Stores compiled scripts. Key format: &lt;cluster node code&gt;_&lt;scriptInstance code&gt;. Value is a compiled script class and class instance
     */
    @Resource(lookup = "java:jboss/infinispan/cache/opencell/opencell-script-cache")
    private Cache<CacheKeyStr, Class<ScriptInterface>> compiledScripts;

    /**
     * Get a summary of cached information.
     * 
     * @return A list of a map containing cache information with cache name as a key and cache as a value
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, Cache> getCaches() {
        Map<String, Cache> summaryOfCaches = new HashMap<String, Cache>();
        summaryOfCaches.put(compiledScripts.getName(), compiledScripts);

        return summaryOfCaches;
    }

    /**
     * Refresh cache by name. Removes <b>current provider's</b> data from cache and populates it again
     * 
     * @param cacheName Name of cache to refresh or null to refresh all caches
     */
    @Override
    @Asynchronous
    public void refreshCache(String cacheName) {

        if (cacheName == null || cacheName.equals(compiledScripts.getName()) || cacheName.contains(compiledScripts.getName())) {
            scriptInstanceService.compileAndInitializeAll();
        }
    }

    /**
     * Populate cache by name
     * 
     * @param cacheName Name of cache to populate or null to populate all caches
     */
    @Override
    public void populateCache(String cacheName) {

        if (cacheName == null || cacheName.equals(compiledScripts.getName())) {
            scriptInstanceService.compileAndInitializeAll();
        }
    }
}