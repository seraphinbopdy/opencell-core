package org.meveo.apiv2.billing.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.meveo.apiv2.billing.InvoiceCancellationInput;

@Path("/v2/invoices")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public interface InvoicesResource {

    @PUT
    @Path("/cancellation")
    @Operation(summary = "Cancel list of invoices by filter", tags = {"Invoices" },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cancel list successfully processed")}
    )
    Response cancellation(@Parameter(description = "Object contains cancellation information", required = true)
                          InvoiceCancellationInput invoiceCancellationInput);
}
