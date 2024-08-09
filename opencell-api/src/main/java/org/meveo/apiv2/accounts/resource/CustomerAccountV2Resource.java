package org.meveo.apiv2.accounts.resource;

import io.swagger.v3.oas.annotations.Operation;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.CustomerAccountDto;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
