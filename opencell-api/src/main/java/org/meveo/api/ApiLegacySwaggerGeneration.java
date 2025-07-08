package org.meveo.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.meveo.api.restful.swagger.OpencellJaxrsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.integration.GenericOpenApiContext;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * This class is used to generate Swagger documentation for Legacy endpoints of APIv0
 *
 * @author Thang Nguyen
 */
@Path("/openapi.{type:json|yaml}")
public class ApiLegacySwaggerGeneration extends BaseOpenApiResource {

    private static String oasStandardApiTxt;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, "application/json" })
    @io.swagger.v3.oas.annotations.Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) {

        // On first call populate the data
        if (oasStandardApiTxt == null) {
            loadOpenAPI();
        }

        return Response.ok().entity(oasStandardApiTxt).build();
    }

    /**
     * Load Open API definition for the swagger
     */
    private void loadOpenAPI() {
        try {            
            
            OpencellJaxrsScanner customScanner = new OpencellJaxrsScanner();
            customScanner.setResourcePackages(new HashSet<>(Arrays.asList(
                "org.meveo.api.rest", 
                "org.meveo.api.rest.impl"
            )));
            
            OpenAPI oasStandardApi = new OpenAPI();
            Info info = new Info()
                    .title("Opencell OpenApi definition Legacy API")
                    .description("This Swagger documentation contains all Legacy API endpoints")
                    .termsOfService("http://opencell.com/terms/")
                    .contact(new Contact().email("opencell@opencellsoft.com"))
                    .license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html"));
            oasStandardApi.setInfo(info);
            oasStandardApi.setServers(Collections.singletonList(new Server().url("/opencell").description("Root path")));
            oasStandardApi.setSecurity(Collections.singletonList(new SecurityRequirement().addList("auth")));
            
            SwaggerConfiguration oasStandardConfig = new SwaggerConfiguration()
                    .openAPI(oasStandardApi)
                    .scannerClass(OpencellJaxrsScanner.class.getName())
                    .resourcePackages(new HashSet<>(Arrays.asList(
                        "org.meveo.api.rest", 
                        "org.meveo.api.rest.impl"
                    )));
            
            OpenApiContext ctx = OpenApiContextLocator.getInstance()
                    .getOpenApiContext(OpenApiContext.OPENAPI_CONTEXT_ID_DEFAULT);
            
            if (ctx instanceof GenericOpenApiContext) {
                ((GenericOpenApiContext) ctx).setOpenApiScanner(customScanner);
                ((GenericOpenApiContext) ctx).setOpenApiConfiguration(oasStandardConfig);
                oasStandardApi = ctx.read();
            }
            
            Paths newPaths = new Paths();
            for (String aKey : oasStandardApi.getPaths().keySet()) {
                if (aKey.startsWith("/api/rest")) {
                    newPaths.put(aKey, oasStandardApi.getPaths().get(aKey));
                } else {
                    newPaths.put("/api/rest" + aKey, oasStandardApi.getPaths().get(aKey));
                }
            }
            oasStandardApi.setPaths(newPaths);

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            oasStandardApiTxt = mapper.writeValueAsString(oasStandardApi);

        } catch (JsonProcessingException e) {
            Logger log = LoggerFactory.getLogger(this.getClass());
            log.error("Failed to create a Swagger documentation file", e);
        }
    }
}