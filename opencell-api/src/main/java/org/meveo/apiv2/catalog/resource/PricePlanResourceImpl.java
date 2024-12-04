package org.meveo.apiv2.catalog.resource;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.core.Response;

import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.catalog.service.PricePlanApiService;

@Stateless
@Interceptors({ WsRestApiInterceptor.class })
public class PricePlanResourceImpl implements PricePlanResource {

    @Inject
    private PricePlanApiService pricePlanApiService;

	@Override
	public Response getDiscountPlanItem(String pricePlanMatrixCode, int pricePlanMatrixVersion) {
		return Response.ok().entity(pricePlanApiService.checkIfUsed(pricePlanMatrixCode, pricePlanMatrixVersion)).build();
	}

}
