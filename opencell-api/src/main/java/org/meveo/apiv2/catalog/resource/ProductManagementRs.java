package org.meveo.apiv2.catalog.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.response.cpq.GetProductDtoResponse;
import org.meveo.apiv2.catalog.SimpleOneshotProductDto;
import org.meveo.apiv2.catalog.SimpleRecurrentProductDto;
import org.meveo.apiv2.catalog.SimpleUsageProductDto;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v2/catalog/productManagement")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface ProductManagementRs {
    
    @POST
    @Path("/createProductSimpleOneshot")
    @Operation(summary = "This endpoint allows to create a new product linked to a one-shot charge",
            tags = { "Product" },
            description ="creation of the product",
            responses = {
                    @ApiResponse(responseCode="200", description = "the product successfully created",
                            content = @Content(schema = @Schema(implementation = GetProductDtoResponse.class))),
                    @ApiResponse(responseCode = "400", description = "the product already existe with the given code")
            })
    ActionStatus createProductSimpleOneshot(SimpleOneshotProductDto postData);




    @POST
    @Path("/createProductSimpleRecurrent")
    @Operation(summary = "This endpoint allows to create a new product linked to a recurrent charge",
            tags = { "Product" },
            description ="creation of the product",
            responses = {
                    @ApiResponse(responseCode="200", description = "the product successfully created",
                            content = @Content(schema = @Schema(implementation = GetProductDtoResponse.class))),
                    @ApiResponse(responseCode = "400", description = "the product already existe with the given code")
            })
    ActionStatus createProductSimpleRecurrent(SimpleRecurrentProductDto postData);




    @POST
    @Path("/createProductSimpleUsage")
    @Operation(summary = "This endpoint allows to create a new product linked to a usage charge",
            tags = { "Product" },
            description ="creation of the product",
            responses = {
                    @ApiResponse(responseCode="200", description = "the product successfully created",
                            content = @Content(schema = @Schema(implementation = GetProductDtoResponse.class))),
                    @ApiResponse(responseCode = "400", description = "the product already existe with the given code")
            })
    ActionStatus createProductSimpleUsage(SimpleUsageProductDto postData);
}
