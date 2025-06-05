package org.meveo.apiv2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * This class is used to generate Swagger documentation for endpoints of APIv2
 *
 * @author Thang Nguyen
 */
@Path("/v2/openapi.json")
public class Apiv2SwaggerGeneration extends BaseOpenApiResource {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @GET
    @Produces({ MediaType.APPLICATION_JSON, "application/json" })
    @io.swagger.v3.oas.annotations.Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo) {

    	String fileName = "doc/swagger/openapi.json";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                log.error("OpenAPI file not found: {}", fileName);
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("OpenAPI documentation not found")
                               .build();
            }

            String content = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            return Response.ok(content,  MediaType.valueOf("application/json")).build();

        } catch (Exception e) {
            log.error("Error reading OpenAPI file: {}", fileName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Failed to read OpenAPI file")
                           .build();
        }
    }
}