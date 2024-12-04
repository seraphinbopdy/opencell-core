package org.meveo.apiv2.accounts.resource;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.CustomerHierarchyDto;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * API for managing account hierarchies.
 * 
 * 
 */
@Path("/v2/account/accountHierarchy")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface AccountHierarchyV2Resource {

    /**
     * This service allows to create / update (if exist already) and close / terminate (if termination date is set) a list of customer, customer accounts, billing accounts, user
     * accounts, subscriptions, services, and access in one transaction. It can activate and terminate subscription and service instance. Close customer account. Terminate billing
     * and user account.
     *
     * @param postData posted data
     * @return action status.
     */
    @POST
    @Path("/customerHierarchyUpdate")
    @Operation(summary = "Update account hierarchy",
            tags = { "AccountHierarchy" },
            description ="This service allows to create / update (if exist already) and close / terminate (if termination date is set) a list of customer, customer accounts, billing accounts, user"
                    + " accounts, subscriptions, services, and access in one transaction. It can activate and terminate subscription and service instance. Close customer account. Terminate billing"
                    + " and user account")
    ActionStatus customerHierarchyUpdate(CustomerHierarchyDto postData);
    
}
