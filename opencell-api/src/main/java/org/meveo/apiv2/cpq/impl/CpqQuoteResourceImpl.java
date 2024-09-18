package org.meveo.apiv2.cpq.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.cpq.resource.CpqQuoteResource;
import org.meveo.apiv2.cpq.service.CpqQuoteApiService;
import org.meveo.apiv2.ordering.resource.oo.ImmutableAvailableOpenOrder;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

@Interceptors({ WsRestApiInterceptor.class })
public class CpqQuoteResourceImpl implements CpqQuoteResource {
	
	@Inject
	private CpqQuoteApiService cpqQuoteApiService;

	@Transactional
	@Override
	public Response findAvailableOpenOrders(String quoteCode) {
		
		List<ImmutableAvailableOpenOrder> result = cpqQuoteApiService.findAvailableOpenOrders(quoteCode);
		
		Map<String, Object> response = new HashMap<>();
		response.put("availableOpenOrders", result);
		
		return Response.ok(response).build();
	}

}
