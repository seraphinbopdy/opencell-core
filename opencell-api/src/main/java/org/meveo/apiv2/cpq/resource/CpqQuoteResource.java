package org.meveo.apiv2.cpq.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.meveo.apiv2.quote.QuoteEmailInput;

import java.util.List;

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
