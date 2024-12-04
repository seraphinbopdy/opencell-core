package org.meveo.apiv2.accounts.resource;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.UserAccountDto;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v2/account/userAccount")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserAccountV2Resource {
    
    /**
     * Create a new user account.
     *
     * @param postData The user account's data
     * @return Request processing status
     */
    @POST
    @Path("/")
    @Operation(summary = "Create a new user account", tags = { "User account management" })
    ActionStatus create(UserAccountDto postData);

    /**
     * Update an existing user account
     *
     * @param postData The user account's data
     * @return Request processing status
     */
    @PUT
    @Path("/")
    @Operation(summary = "Update an existing user account", tags = { "User account management" })
    ActionStatus update(UserAccountDto postData);

    /**
     * Create new or update an existing user account.
     *
     * @param postData The user account's data
     * @return Request processing status
     */
    @POST
    @Operation(summary = "Create new or update an existing user account", tags = { "Deprecated" }, deprecated = true)
    @Path("/createOrUpdate")
    ActionStatus createOrUpdate(UserAccountDto postData);

}
