package org.meveo.api;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath("/api/rest/v0")
public class JaxRsActivatorLegacyAPIConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<Class<?>>();
        resources.add(ApiLegacySwaggerGeneration.class);
        Logger log = LoggerFactory.getLogger(getClass());
        log.info("Opencell OpenAPI definition is accessible in /api/rest/v0/openapi.{type:json|yaml}");

        return resources;
    }
}