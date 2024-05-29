package org.meveo.api.rest.invoice.impl;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.invoice.PaymentTermApi;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.invoice.PaymentTermResource;
import org.meveo.apiv2.billing.PaymentTermMapper;
import org.meveo.model.payments.PaymentTerm;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.Response;

@Stateless
@Interceptors({ WsRestApiInterceptor.class })
public class PaymentTermResourceImpl  implements PaymentTermResource {
	
	@Inject
	private PaymentTermApi paymentTermApi;
	private PaymentTermMapper paymentTermMapper = new PaymentTermMapper();
	
	@Override
	public Response create(org.meveo.api.dto.invoice.PaymentTerm postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
		PaymentTerm paymentTerm = paymentTermApi.create(paymentTermMapper.toEntity(postData));
		result.setEntityCode(paymentTerm.getCode());
		result.setEntityId(paymentTerm.getId());
		return Response.ok(result).build();
	}
	
	@Override
	public Response update(org.meveo.api.dto.invoice.PaymentTerm postData, String paymentTermCode) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
			paymentTermApi.update(paymentTermMapper.toEntity(postData), paymentTermCode);
		return Response.ok(result).build();
	}
	
	@Override
	public Response find(String paymentTermCode) {
		return Response.ok(paymentTermMapper.toResource(paymentTermApi.find(paymentTermCode))).build();
	}
	
	@Override
	public Response remove(String paymentTermCode) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
		paymentTermApi.delete(paymentTermCode);
		return Response.ok(result).build();
	}
	
	@Override
	public Response enable(String paymentTermCode) {
		return enableOrdDisable(paymentTermCode, true);
	}
	
	@Override
	public Response disable(String paymentTermCode) {
		return enableOrdDisable(paymentTermCode, false);
	}
	
	private Response enableOrdDisable(String paymentTermCode, boolean enable) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
		paymentTermApi.enableOrDisable(paymentTermCode, enable);
		return Response.ok(result).build();
	}
}
