package org.meveo.apiv2.esignature.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.meveo.api.dto.ActionStatus;
import org.meveo.apiv2.esignature.SigantureRequest;
import org.meveo.apiv2.esignature.SignatureRequestWebHookPayload;
import org.meveo.apiv2.esignature.SignatureRequestWebhook;
import org.meveo.model.esignature.Operator;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/documents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SignatureRequestResource {
	@POST
	@Path("/signatureRequest")
	@Operation(
			summary="initiate and upload document depending on mode operator",
			description=" new version for seller.  ",
			operationId="    POST_SIGNATURE_REQUEST_steps",
			responses= {
					@ApiResponse(description=" response from yousign activation endpoint if operator yousign is used ",
							content=@Content(
									schema=@Schema(
											implementation= Response.class
									)
							)
					)}
	)
	Response sigantureRequest(SigantureRequest sigantureRequest);
	
	@GET
	@Path("/{operator}/signatureRequest/{signatureRequestId}")
	@Operation(
			summary="fetch a signature request ",
			description=" get data from signature request id ",
			operationId="    POST_SIGNATURE_REQUEST_fetch",
			responses= {
					@ApiResponse(description=" response from operator used  ",
							content=@Content(
									schema=@Schema(
											implementation= Response.class
									)
							)
					)}
	)
	Response fetchSignatureRequest(@PathParam("operator") Operator operator, @PathParam("signatureRequestId") String signatureRequestId);
	
	
	@GET
	@Path("{operator}/signatureRequest/{signatureRequestId}/documents/download")
	Response download(@PathParam("operator") Operator operator, @PathParam("signatureRequestId") String signatureRequestId);
	
	@POST
	@Path("/{operator}/signatureRequest/done")
	ActionStatus signatureRequestDone(@PathParam("operator") Operator operator, SignatureRequestWebHookPayload signatureRequestWebHookPayload);
	
}
