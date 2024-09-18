package org.meveo.service.billing.impl;

import java.util.List;

import org.meveo.model.payments.PaymentTerm;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

@Stateless
public class PaymentTermService extends PersistenceService<PaymentTerm> {
	
	public PaymentTerm findByCode(String paymentTermCode) {
		try{
			return getEntityManager().createNamedQuery("PaymentTerm.findByCode", PaymentTerm.class)
					.setParameter("code", paymentTermCode.toLowerCase())
					.getSingleResult();
		}catch (NoResultException e){
			return null;
		}
	}
	
	public List<PaymentTerm> findAllEnabledPaymentTerm() {
		return getEntityManager().createNamedQuery("PaymentTerm.findAllEnabledPaymentTerm", PaymentTerm.class)
				.getResultList();
	}
}