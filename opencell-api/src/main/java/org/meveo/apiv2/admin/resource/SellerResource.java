package org.meveo.apiv2.admin.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.meveo.api.dto.ActionStatus;
import org.meveo.apiv2.admin.Seller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.meveo.apiv2.models.Resource;

@Path("/v2/seller")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SellerResource {

	@POST
	@Operation(
			summary=" Create seller for v2.  ",
			description=" new version for seller.  ",
			operationId="    POST_Seller_create",
			responses= {
				@ApiResponse(description=" action status ",
						content=@Content(
									schema=@Schema(
											implementation= Response.class
											)
								)
				)}
	)
	Response create(Seller postData);
	

    /**
     * Update seller.
     * 
     * @param postData posted data
     * @return action status.
     */
    @PUT
	@Operation(
			summary=" Update seller v2.  ",
			description=" new version for updating a seller.  ",
			operationId="    PUT_Seller_update",
			responses= {
				@ApiResponse(description=" action status. ",
						content=@Content(
									schema=@Schema(
											implementation= Response.class
											)
								)
				)}
	)
    Response update(Seller postData);
    


    /**
     * Create or update a seller.
     *
     * @param postData posted data
     * @return created or updated seller.
     */
    @POST
    @Path("/createOrUpdate")
	@Operation(
			summary=" Create or update a seller. ",
			description=" Create or update a seller. ",
			operationId="    POST_Seller_createOrUpdate",
			responses= {
				@ApiResponse(description=" created or updated seller. ",
						content=@Content(
									schema=@Schema(
											implementation= Response.class
											)
								)
				)}
	)
    Response createOrUpdate(Seller postData);
}
