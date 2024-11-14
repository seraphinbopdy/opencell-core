package org.meveo.apiv2.accounts.resource;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.BillingAccountDto;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v2/account/billingAccount")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface BillingAccountV2Resource {

    /**
     * Create a new billing account.
     *
     * @param postData Billing account data
     * @return Request processing status
     */
    @POST
    @Path("/")
    @Operation(summary = "Create a new billing account", tags = { "Billing account management" })
    ActionStatus create(BillingAccountDto postData);

    /**
     * Update existing billing account.
     *
     * @param postData Billing account data
     * @return Request processing status
     */
    @PUT
    @Path("/")
    @Operation(summary = "Update existing billing account", tags = { "Billing account management" })
    ActionStatus update(BillingAccountDto postData);

    /**
     * Create or update Billing Account based on code.
     *
     * @param postData Billing account data
     * @return Request processing status
     */
    @POST
    @Path("/createOrUpdate")
    @Operation(summary = "Create or update Billing Account based on cod", tags = { "Deprecated" }, deprecated = true)
    ActionStatus createOrUpdate(BillingAccountDto postData);

}
