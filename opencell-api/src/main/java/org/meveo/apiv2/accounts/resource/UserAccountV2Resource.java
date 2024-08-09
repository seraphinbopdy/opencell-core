package org.meveo.apiv2.accounts.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.UserAccountDto;
import org.meveo.apiv2.models.ApiException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
