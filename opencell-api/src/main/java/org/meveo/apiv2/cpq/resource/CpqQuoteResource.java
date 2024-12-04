package org.meveo.apiv2.cpq.resource;

import org.meveo.apiv2.quote.QuoteEmailInput;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v2/cpq/quotes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CpqQuoteResource {

    @GET
    @Path("/{quoteCode}/availableOpenOrders")
    @Operation(summary = "Get available open orders for a quote", 
    tags = {"Quote management"},
    responses = {
    		@ApiResponse(responseCode = "200", description = "The Open Orders avaiblable for quote")
    })
    Response findAvailableOpenOrders(@Parameter(description = "", required = true) @PathParam("quoteCode") String quoteCode);
	
	@POST
	@Path("/sendByEmail")
	@Operation(summary = "send approved quote by email",
			tags = {"Quote management"},
			responses = {
					@ApiResponse(responseCode = "200", description = "The email has been sent")
			})
	Response sendByEmail(@Parameter(description = "", required = true)QuoteEmailInput quoteEmailInput);
}
