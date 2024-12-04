package org.meveo.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;

/**
 * This class is used to generate Swagger documentation for Legacy endpoints of APIv0
 *
 * @author Thang Nguyen
 */
@Path("/openapi.{type:json|yaml}")
public class ApiLegacySwaggerGeneration {

    // AKK Migrate me
    
//extends BaseOpenApiResource {
//
//    private static String oasStandardApiTxt;
//
//    @GET
//    @Produces({ MediaType.APPLICATION_JSON, "application/json" })
//    @io.swagger.v3.oas.annotations.Operation(hidden = true)
//    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) {
//
//        // On first call populate the data
//        if (oasStandardApiTxt == null) {
//            loadOpenAPI();
//        }
//
//        return Response.ok().entity(oasStandardApiTxt).build();
//    }
//
//    /**
//     * Load Open API definition for the swagger
//     */
//    private void loadOpenAPI() {
//        try {
//
//            OpenApiContext ctx = new JaxrsOpenApiContextBuilder<>().ctxId("apiv0").configLocation("/openapi-configuration-apiv0.json").buildContext(true);
//            OpenAPI oasStandardApi = ctx.read();
//
//            Paths newPaths = new Paths();
//            for (String aKey : oasStandardApi.getPaths().keySet()) {
//                if (aKey.startsWith("/api/rest")) {
//                    newPaths.put(aKey, oasStandardApi.getPaths().get(aKey));
//                } else {
//                    newPaths.put("/api/rest" + aKey, oasStandardApi.getPaths().get(aKey));
//                }
//            }
//            oasStandardApi.setPaths(newPaths);
//            oasStandardApi.setSecurity(null);
//
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//            oasStandardApiTxt = mapper.writeValueAsString(oasStandardApi);
//
//        } catch (OpenApiConfigurationException e) {
//            Logger log = LoggerFactory.getLogger(this.getClass());
//            log.error("Failed to create a Swagger documentation file", e);
//
//        } catch (JsonProcessingException e) {
//            Logger log = LoggerFactory.getLogger(this.getClass());
//            log.error("Failed to create a Swagger documentation file", e);
//        }
//    }
}