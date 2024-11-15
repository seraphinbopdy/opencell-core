package org.meveo.apiv2.accounts.resource;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.CustomerAccountDto;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v2/account/customerAccount")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface CustomerAccountV2Resource {

    /**
     * Create a new customer account
     *
     * @param postData The customer account's data
     * @return Request processing status
     */
    @POST
    @Path("/")
    @Operation(summary = "Create a customer account",
            tags = { "Customer account management" })
    ActionStatus create(CustomerAccountDto postData);

    /**
     * Update an existing customer account
     *
     * @param postData The customer account's data
     * @return Request processing status
     */
    @PUT
    @Path("/")
    @Operation(summary = "Update a customer account",
            tags = { "Customer account management" })
    ActionStatus update(CustomerAccountDto postData);

    /**
     * Create new or update existing customer account
     *
     * @param postData The customer account's data
     * @return Request processing status
     */
    @POST
    @Path("/createOrUpdate")
    @Operation(summary = "Create or update a customer account", deprecated = true,
            tags = { "Deprecated" })
    ActionStatus createOrUpdate(CustomerAccountDto postData);
    
    
    
}
