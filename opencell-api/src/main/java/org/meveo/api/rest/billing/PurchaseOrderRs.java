package org.meveo.api.rest.billing;

import org.meveo.api.dto.billing.PurchaseOrderDto;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
