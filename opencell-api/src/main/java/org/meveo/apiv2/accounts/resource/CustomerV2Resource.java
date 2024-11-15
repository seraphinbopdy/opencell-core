package org.meveo.apiv2.accounts.resource;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.CustomerDto;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v2/account/customer")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface CustomerV2Resource {

    /**
     * Create a new customer
     *
     * @param postData The customer's data
     * @return Request processing status
     */
    @POST
    @Path("/")
    @Operation(summary = "Create a new customer",
            tags = { "Customer management" })
    ActionStatus create(CustomerDto postData);

    /**
     * Update an existing customer
     *
     * @param postData The customer's data
     * @return Request processing status
     */
    @PUT
    @Path("/")
    @Operation(summary = "Update an existing customer",
            tags = { "Customer management" })
    ActionStatus update(CustomerDto postData);
    
    /**
     * Create new or update existing customer
     *
     * @param postData The customer's data
     * @return Request processing status
     */
    @POST
    @Path("/createOrUpdate")
    @Operation(summary = " Create new or update existing customer",deprecated = true,
            tags = { "Deprecated" })
    ActionStatus createOrUpdate(CustomerDto postData);
    
    
    
}
