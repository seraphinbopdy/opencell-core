package org.meveo.apiv2.catalog.resource.pricelist;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/**
 * PriceList Endpoints
 *
 * @author zelmeliani
 * @since 15.0
 *
 */
@Path("/catalog")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CatalogPriceListResource {
	
	@GET
	@Path("/{billingAccountCode}/priceList")
	Response getPriceLists(
			@QueryParam("offset") @DefaultValue("0") Long offset,
			@QueryParam("limit") @DefaultValue("50") Long limit,
            @QueryParam("sortOrder") String sortOrder, 
            @QueryParam("sortBy") String orderBy,
            @PathParam("billingAccountCode") String billingAccountCode,
            @Context Request request);
}