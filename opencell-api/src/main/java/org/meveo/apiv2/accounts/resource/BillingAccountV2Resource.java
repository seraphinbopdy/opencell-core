package org.meveo.apiv2.accounts.resource;

import io.swagger.v3.oas.annotations.Operation;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.BillingAccountDto;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

}
