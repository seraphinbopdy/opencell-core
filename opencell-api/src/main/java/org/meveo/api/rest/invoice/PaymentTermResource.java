package org.meveo.api.rest.invoice;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.meveo.api.dto.invoice.PaymentTerm;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/billing/invoicing/paymentTerms")
@Tag(name = "paymentTerms", description = "@%PaymentTermrs")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface PaymentTermResource {
	
	@POST
	Response create(PaymentTerm postData);
	
	@PUT
	@Path("/{paymentTermCode}")
	Response update(PaymentTerm postData, @PathParam("paymentTermCode") String paymentTermCode);
	
	@GET
	@Path("/{paymentTermCode}")
	Response find(@PathParam("paymentTermCode") String paymentTermCode);
	
	@DELETE
	@Path("/{paymentTermCode}")
	Response remove(@PathParam("paymentTermCode") String paymentTermCode);
	
	@PATCH
	@Path("/{paymentTermCode}/enable")
	Response enable(@PathParam("paymentTermCode") String paymentTermCode);
	
	@PATCH
	@Path("/{paymentTermCode}/disable")
	Response disable(@PathParam("paymentTermCode") String paymentTermCode);
}
