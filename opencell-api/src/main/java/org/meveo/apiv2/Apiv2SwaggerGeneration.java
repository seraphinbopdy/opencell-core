package org.meveo.apiv2;

import jakarta.ws.rs.Path;

/**
 * This class is used to generate Swagger documentation for endpoints of APIv2
 *
 * @author Thang Nguyen
 */
@Path("/openapi.{type:json|yaml}")
public class Apiv2SwaggerGeneration { 
//extends BaseOpenApiResource {
//
//    private final Logger log = LoggerFactory.getLogger(this.getClass());
//
//    private static String openAPIv2Txt;
//
//    @GET
//    @Produces({ MediaType.APPLICATION_JSON, "application/json" })
//    @io.swagger.v3.oas.annotations.Operation(hidden = true)
//    public Response getOpenApi(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) {
//
//        if (openAPIv2Txt == null) {
//            loadOpenAPI();
//        }
//        return Response.ok().entity(openAPIv2Txt).build();
//
//    }
//
//    /**
//     * Load Open API definition for the swagger
//     */
//    private void loadOpenAPI() {
//        try {
//            OpenApiContext ctx = new JaxrsOpenApiContextBuilder<>().ctxId("apiv2").configLocation("/openapi-configuration-apiv2.json").buildContext(true);
//
//            OpenAPI openAPIv2 = ctx.read();
//
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//            openAPIv2Txt = mapper.writeValueAsString(openAPIv2);
//
//        } catch (OpenApiConfigurationException e) {
//            log.error("Failed to create a Swagger documentation file", e);
//        } catch (JsonProcessingException e) {
//            log.error("Failed to create a Swagger documentation file", e);
//        }
//    }
}