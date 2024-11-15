package org.meveo.api.rest.billing;

import org.meveo.api.dto.billing.PurchaseOrderDto;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * purchase order API specification implementation
 *
 * @author Tarik.FA
 */
@Path("/purchaseOrder")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface PurchaseOrderRs {

    @POST
    @Path("")
    Response create(PurchaseOrderDto postData);

    @PUT
    @Path("/{id}")
    Response update(PurchaseOrderDto postData, @PathParam("id") Long id);

}
