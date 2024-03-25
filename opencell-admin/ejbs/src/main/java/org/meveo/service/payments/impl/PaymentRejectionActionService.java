package org.meveo.service.payments.impl;

import java.util.List;

import javax.ejb.Stateless;

import org.meveo.model.payments.PaymentRejectionAction;
import org.meveo.service.base.BusinessService;

@Stateless
public class PaymentRejectionActionService extends BusinessService<PaymentRejectionAction> {
	
	public List<PaymentRejectionAction> findActionsByCodeAndPaymentGateway(String code, Long paymentGatewayId) {
		return getEntityManager()
				.createNamedQuery("PaymentRejectionAction.getActionsByRejectionCode", PaymentRejectionAction.class)
				.setParameter("code", code)
				.setParameter("paymentGatewayId", paymentGatewayId)
				.getResultList();
	}
}
