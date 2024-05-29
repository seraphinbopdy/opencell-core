package org.meveo.api.rest.invoice;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.meveo.api.dto.invoice.PaymentTerm;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
