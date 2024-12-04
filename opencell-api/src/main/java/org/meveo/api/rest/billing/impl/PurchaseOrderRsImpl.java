package org.meveo.api.rest.billing.impl;

import org.meveo.api.billing.PurchaseOrderApi;
import org.meveo.api.dto.billing.PurchaseOrderDto;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.billing.PurchaseOrderRs;
import org.meveo.api.rest.impl.BaseRs;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Interceptors({ WsRestApiInterceptor.class })
public class PurchaseOrderRsImpl extends BaseRs implements PurchaseOrderRs {

	@Inject
	private PurchaseOrderApi purchaseOrderApi;
	
	@Override
	public Response create(PurchaseOrderDto postData) {
		return Response.ok().entity(purchaseOrderApi.create(postData)).build();
	}
	
	@Override
	public Response update(PurchaseOrderDto postData, Long id) {
		purchaseOrderApi.update(id, postData);
		return Response.noContent().build();
	}
}
