package org.meveo.apiv2.settings.globalSettings;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.meveo.apiv2.settings.AdvancedSettings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/v2/setting/advanced")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AdvancedSettingsResource {

    @POST
    @Operation(summary = "Advanced settings",
            tags = {"Post", "Advanced settings"},
            description = "Create Advanced settings",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Advanced settings was successfully created"),
                    @ApiResponse(responseCode = "400",
                            description = "Bad Request")
            })
    Response create(@Parameter(description = "Advanced settings", required = true) AdvancedSettings input);
    
    @PUT
    @Path("/{id}")
    @Operation(summary = "Advanced settings",
            tags = {"Put", "Advanced settings"},
            description = "Update Advanced settings",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Advanced settings was successfully updated"),
                    @ApiResponse(responseCode = "400", description = "Bad Request"),
                    @ApiResponse(responseCode = "404", description = "The AdvancedSettings does not exist")
            })
    Response update(@Parameter(description = "Advanced settings id", required = true) @PathParam("id") Long id,
            @Parameter(description = "Advanced settings", required = true) AdvancedSettings input);

    @PUT
    @Operation(summary = "Advanced settings",
            tags = {"Patch", "Advanced settings"},
            description = "Update Advanced settings",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Advanced settings was successfully updated"),
                    @ApiResponse(responseCode = "400", description = "Bad Request"),
                    @ApiResponse(responseCode = "404", description = "The AdvancedSettings does not exist")
            })
    Response patch(@Parameter(description = "Advanced settings", required = true) List<AdvancedSettings> input);


}
