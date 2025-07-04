package org.meveo.api.restful.swagger;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.meveo.api.restful.constant.MapRestUrlAndStandardUrl;
import org.meveo.api.restful.swagger.service.Apiv1SwaggerDeleteOperation;
import org.meveo.api.restful.swagger.service.Apiv1SwaggerGetOperation;
import org.meveo.api.restful.swagger.service.Apiv1SwaggerPostOperation;
import org.meveo.api.restful.swagger.service.Apiv1SwaggerPutOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.integration.GenericOpenApiContext;
import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.inject.Inject;
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
 * This class is used to generate Swagger documentation for RESTful endpoints of APIv1
 *
 * @author Thang Nguyen
 */
@Path("/openapi.{type:json|yaml}")
public class ApiRestSwaggerGeneration extends BaseOpenApiResource {

    @Inject
    private Apiv1SwaggerPostOperation postOperation;

    @Inject
    private Apiv1SwaggerGetOperation getOperation;

    @Inject
    private Apiv1SwaggerPutOperation putOperation;

    @Inject
    private Apiv1SwaggerDeleteOperation deleteOperation;
    
    private static final Logger log = LoggerFactory.getLogger(ApiRestSwaggerGeneration.class);

    private static String oasRestApiTxt;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, "application/json" })
    @io.swagger.v3.oas.annotations.Operation(hidden = true)
    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) {

        // On first call populate the data
        if (oasRestApiTxt == null) {
            loadOpenAPI();
        }

        return Response.ok().entity(oasRestApiTxt).build();
    }

    /**
     * Load Open API definition for the swagger
     */
    private void loadOpenAPI() {
        try {
        	
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new TypeAdapter<OffsetDateTime>() {
                    @Override
                    public void write(JsonWriter out, OffsetDateTime value) throws IOException {
                        out.value(value != null ? value.toString() : null);
                    }
                    @Override
                    public OffsetDateTime read(JsonReader in) throws IOException {
                        return in.hasNext() ? OffsetDateTime.parse(in.nextString()) : null;
                    }
                })
                .create();
            
            OpencellJaxrsScanner customScanner = new OpencellJaxrsScanner();
            customScanner.setResourcePackages(new HashSet<>(Arrays.asList(
                "org.meveo.api.rest", 
                "org.meveo.api.rest.impl"
            )));

            OpenAPI oasStandardApi = new OpenAPI();
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

            String jsonApiStd = gson.toJson(oasStandardApi.getPaths());
            Map<String, PathItem> MAP_SWAGGER_PATHS = gson.fromJson(jsonApiStd, 
                new TypeToken<Map<String, PathItem>>() {}.getType());

            OpenAPI oasRestApi = new OpenAPI();
            Info info = new Info()
                .title("Opencell OpenApi definition V1")
                .version("1.0")
                .description("This Swagger documentation contains API v1 endpoints")
                .termsOfService("http://opencell.com/terms/")
                .contact(new Contact().email("opencell@opencellsoft.com"))
                .license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html"));
            
            oasRestApi.setInfo(info);

            SwaggerConfiguration oasRestConfig = new SwaggerConfiguration()
                .openAPI(oasRestApi)
                .prettyPrint(true)
                .readAllResources(false)
                .scannerClass(OpencellJaxrsScanner.class.getName());

            setOpenApiConfiguration(oasRestConfig);

            GenericOpenApiContextBuilder ctxBuilder = new GenericOpenApiContextBuilder();
            ctxBuilder.setCtxId("Apiv1-rest-id");
            ctxBuilder.openApiConfiguration(oasRestConfig);
            
            OpencellJaxrsScanner restScanner = new OpencellJaxrsScanner();
            restScanner.setResourcePackages(new HashSet<>(Arrays.asList(
                "org.meveo.api.restful"
            )));
            
            //ctxBuilder.openApiScanner(restScanner);
            OpenApiContext restContext;
			try {
				restContext = ctxBuilder.buildContext(true);
	            oasRestApi = restContext.read();

			} catch (OpenApiConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            Paths paths = new Paths();
            oasRestApi.setPaths(paths);
            oasRestApi.setComponents(oasStandardApi.getComponents());
            oasRestApi.setServers(Collections.singletonList(new Server().url("/opencell").description("Root path")));
            oasRestApi.setSecurity(Collections.singletonList(new SecurityRequirement().addList("auth")));

            for (Map.Entry<String, String> mapPathEntry : MapRestUrlAndStandardUrl.MAP_RESTFUL_URL_AND_STANDARD_URL.entrySet()) {
                String aStdPath = mapPathEntry.getKey();
                String aRFPath = mapPathEntry.getValue();
                PathItem pathItem = oasRestApi.getPaths().containsKey(aRFPath) ? 
                    oasRestApi.getPaths().get(aRFPath) : new PathItem();

                for (Map.Entry<String, PathItem> mapSwaggerEntry : MAP_SWAGGER_PATHS.entrySet()) {
                    String anOldPath = mapSwaggerEntry.getKey();
                    PathItem pathItemInOldSwagger = mapSwaggerEntry.getValue();
                    String[] splitStdPath = aStdPath.split(MapRestUrlAndStandardUrl.SEPARATOR);
                    
                    if (splitStdPath[1].equals(anOldPath)) {
                        switch (splitStdPath[0]) {
                            case MapRestUrlAndStandardUrl.GET:
                                if (pathItemInOldSwagger.getGet() != null)
                                    getOperation.setGet(pathItem, pathItemInOldSwagger.getGet(), aRFPath);
                                break;
                            case MapRestUrlAndStandardUrl.POST:
                                if (pathItemInOldSwagger.getPost() != null)
                                    postOperation.setPost(pathItem, pathItemInOldSwagger.getPost(), aRFPath);
                                break;
                            case MapRestUrlAndStandardUrl.PUT:
                                if (pathItemInOldSwagger.getPut() != null)
                                    putOperation.setPut(pathItem, pathItemInOldSwagger.getPut(), aRFPath);
                                break;
                            case MapRestUrlAndStandardUrl.DELETE:
                                if (pathItemInOldSwagger.getDelete() != null)
                                    deleteOperation.setDelete(pathItem, pathItemInOldSwagger.getDelete(), aRFPath);
                                break;
                        }
                        break;
                    }
                }

                paths.addPathItem(aRFPath, pathItem);
            }

            oasRestApi.setPaths(paths);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            oasRestApiTxt = mapper.writeValueAsString(oasRestApi);

        } catch (JsonProcessingException e) {
            log.error("Failed to create a Swagger documentation file", e);
        }
    }
}