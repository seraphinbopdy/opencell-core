package org.meveo.service.billing.impl;

import org.meveo.model.billing.PurchaseOrder;
import org.meveo.service.base.BusinessService;

import jakarta.ejb.Stateless;

@Stateless
public class PurchaseOrderService extends BusinessService<PurchaseOrder> {
	
	public PurchaseOrder findByNumber(String number) {
		try {
			return getEntityManager().createNamedQuery("PurchaseOrder.findByNumber", PurchaseOrder.class)
					.setParameter("number", number)
					.getSingleResult();
		} catch (Exception e) {
			return null;
		}
	}
}
