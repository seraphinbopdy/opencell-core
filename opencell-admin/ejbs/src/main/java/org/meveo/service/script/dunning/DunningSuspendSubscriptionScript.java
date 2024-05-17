package org.meveo.service.script.dunning;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceLine;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.dunning.DunningModeEnum;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.script.Script;

import java.util.*;

public class DunningSuspendSubscriptionScript extends Script {

	private static final long serialVersionUID = 1L;

	private SubscriptionService subscriptionService = (SubscriptionService) getServiceInterface("SubscriptionService");

	@Override
	public void execute(Map<String, Object> context) throws BusinessException {
		// Get parameters
		Invoice invoice = (Invoice) context.get(Script.CONTEXT_ENTITY);
		CustomerAccount customerAccount = (CustomerAccount) context.get("customerAccount");
		DunningModeEnum dunningMode = (DunningModeEnum) context.get("dunningMode");

		// Suspend subscription for invoice level
		if (DunningModeEnum.INVOICE_LEVEL.equals(dunningMode)) {
			if (invoice == null) {
				log.warn("No Invoice passed as CONTEXT_ENTITY");
				throw new BusinessException("No Invoice passed as CONTEXT_ENTITY");
			} else {
				// Suspend subscription attached to invoice if it is active
				if(invoice.getSubscription() != null && SubscriptionStatusEnum.ACTIVE.equals(invoice.getSubscription().getStatus())) {
					subscriptionService.subscriptionSuspension(invoice.getSubscription(), new Date());
				}
				//
				if (invoice.getInvoiceLines() != null && !invoice.getInvoiceLines().isEmpty()) {
					for (InvoiceLine invoiceLine : invoice.getInvoiceLines()) {
						if(invoiceLine.getSubscription() != null && SubscriptionStatusEnum.ACTIVE.equals(invoiceLine.getSubscription().getStatus())) {
							subscriptionService.subscriptionSuspension(invoiceLine.getSubscription(), new Date());
						}
					}
				}
			}
			// Suspend subscription for customer level
		} else if (DunningModeEnum.CUSTOMER_LEVEL.equals(dunningMode)) {
			if (customerAccount == null) {
				log.warn("No CustomerAccount passed as CONTEXT_ENTITY");
				throw new BusinessException("No CustomerAccount passed as CONTEXT_ENTITY");
			} else {
				// Suspend all active subscriptions for customer
				if (customerAccount.getCustomer() != null) {
					List<Subscription> subscriptions = subscriptionService.listByCustomer(customerAccount.getCustomer());
					if (subscriptions != null && !subscriptions.isEmpty()) {
						for (Subscription subscription : subscriptions) {
							if (SubscriptionStatusEnum.ACTIVE.equals(subscription.getStatus())) {
								subscriptionService.subscriptionSuspension(subscription, new Date());
							}
						}
					}
				}
			}
		} else {
			log.warn("Unsupported dunning mode {}", dunningMode);
			throw new BusinessException("Unsupported dunning mode " + dunningMode);
		}
	}
}
