package org.meveo.service.billing.impl;

import org.meveo.model.payments.PaymentTerm;
import org.meveo.service.base.PersistenceService;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import java.util.List;

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
