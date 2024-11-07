package org.meveo.apiv2.accounting.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/v2/accounting")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public interface AccountingResource {

    @GET
    @Path("/auxiliaryAccounts/{customerAccountCode}")
    @Operation(summary = "Get the auxiliary account information corresponding to the giver customer account",
            tags = {"AuxiliaryCode" },
            description = "Returns auxiliary account information corresponding to the giver customer account",
            responses = {
            @ApiResponse(responseCode = "200", description = "Auxiliary account information are successfully evaluated"),
            @ApiResponse(responseCode = "404", description = "Customer account not fount"),
            @ApiResponse(responseCode = "500", description = "Auxiliary account information not correctly evaluated")
    })
    Response getAuxiliaryAccount(@PathParam("customerAccountCode") String customerAccountCode);
    
    @GET
    @Path("/vat/{vat_number}/{country_code}/validate")
    @Operation(summary = "Check the validate VAT in EUROPE",
            tags = {"vat_number", "country_code" },
            description = "Check the validate VAT in EUROPE by vatNumber et countryCode",
            responses = {
            @ApiResponse(responseCode = "200", description = "valid VAT in EUROPE"),
            @ApiResponse(responseCode = "500", description = "inValid VAT in EUROPE")
    })
    Response getValByValNbContryCode(@PathParam("vat_number") String vatNumber,
            @PathParam("country_code") String countryCode);
}