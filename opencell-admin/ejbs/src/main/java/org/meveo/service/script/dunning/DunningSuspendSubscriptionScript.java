package org.meveo.service.script.dunning;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.Invoice;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.script.Script;

import java.util.*;

public class DunningSuspendSubscriptionScript extends Script {

	private static final long serialVersionUID = 1L;

	private SubscriptionService subscriptionService = (SubscriptionService) getServiceInterface("SubscriptionService");

	@Override
	public void execute(Map<String, Object> context) throws BusinessException {
		Invoice invoice = (Invoice) context.get(Script.CONTEXT_ENTITY);

		if (invoice == null) {
			log.warn("No Invoice passed as CONTEXT_ENTITY");
			throw new BusinessException("No Invoice passed as CONTEXT_ENTITY");
		} else {
			subscriptionService.subscriptionSuspension(invoice.getSubscription(), new Date());
		}
	}
}
