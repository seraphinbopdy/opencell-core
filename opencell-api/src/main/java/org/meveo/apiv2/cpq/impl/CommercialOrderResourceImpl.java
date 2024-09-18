package org.meveo.apiv2.cpq.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.cpq.resource.CommercialOrderResource;
import org.meveo.apiv2.cpq.service.CommercialOrderApiService;
import org.meveo.apiv2.ordering.resource.oo.ImmutableAvailableOpenOrder;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

@Interceptors({ WsRestApiInterceptor.class })
public class CommercialOrderResourceImpl implements CommercialOrderResource {
	
	@Inject
	private CommercialOrderApiService commercialOrderApiService;

	@Transactional
	@Override
	public Response findAvailableOpenOrders(String quoteCode) {

		List<ImmutableAvailableOpenOrder> result = commercialOrderApiService.findAvailableOpenOrders(quoteCode);

		Map<String, Object> response = new HashMap<>();
		response.put("availableOpenOrders", result);

		return Response.ok(response).build();
	}

}
