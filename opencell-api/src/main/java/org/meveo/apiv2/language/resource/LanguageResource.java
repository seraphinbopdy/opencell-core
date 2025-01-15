package org.meveo.apiv2.language.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.meveo.apiv2.language.LanguageDto;

@Path("/v2/language")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface LanguageResource {

    @POST
    @Path("/isoLanguage")
    @Operation(
            summary = "Create a new language",
            tags = { "language" },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The language was successfully created"),
                    @ApiResponse(responseCode = "404", description = "Entity does not exist"),
                    @ApiResponse(responseCode = "412", description = "Missing parameters"),
                    @ApiResponse(responseCode = "400", description = "Language creation failed")
            }
    )
    Response createLanguage(LanguageDto languageDto);

    @PUT
    @Path("/isoLanguage/{id}")
    @Operation(
            summary = "Update an existing ISO language",
            tags = { "language" },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The language was successfully updated"),
                    @ApiResponse(responseCode = "404", description = "Entity does not exist"),
                    @ApiResponse(responseCode = "412", description = "Missing parameters"),
                    @ApiResponse(responseCode = "400", description = "Language update failed")
            }
    )
    Response updateLanguage(@PathParam("id") Long id, LanguageDto languageDto);
}

