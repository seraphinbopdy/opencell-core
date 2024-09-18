package org.meveo.service.payments.impl;

import java.util.List;

import org.meveo.model.payments.RejectedPayment;
import org.meveo.model.payments.RejectionActionStatus;
import org.meveo.service.base.BusinessService;

import jakarta.ejb.Stateless;

@Stateless
public class RejectedPaymentService extends BusinessService<RejectedPayment> {
	
    public void updateRejectionActionsStatus(String rejectionCode, List<RejectionActionStatus> statusSourceList, RejectionActionStatus statusDestination) {
    	getEntityManager()
		.createNamedQuery("RejectedPayment.updateRejectionActionsStatus")
		.setParameter("rejectedCode", rejectionCode)
		.setParameter("statusDestination", statusDestination)
		.setParameter("statusSourceList", statusSourceList)
		.executeUpdate();
    }
}
