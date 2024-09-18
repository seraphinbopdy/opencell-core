package org.meveo.service.billing.impl;


import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.meveo.model.billing.InvoiceType;
import org.meveo.model.billing.InvoiceValidationRule;
import org.meveo.service.base.BusinessService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class InvoiceValidationRulesService extends BusinessService<InvoiceValidationRule> {
	
	@Inject
	InvoiceTypeService invoiceTypeService;

    public void updateInvoiceTypePriority(InvoiceValidationRule invoiceValidationRule) {

        InvoiceType invoiceType = invoiceTypeService.refreshOrRetrieve(invoiceValidationRule.getInvoiceType());
        List<InvoiceValidationRule> invoiceValidationRules = invoiceType.getInvoiceValidationRules()
				.stream().filter(rule -> Objects.isNull(rule.getParentRule())).collect(Collectors.toList());

        if (invoiceValidationRule.getPriority() == null) {
            invoiceValidationRule.setPriority(invoiceValidationRules != null ? invoiceValidationRules.size() + 1 : null);
        } else if (invoiceValidationRule.isToReorder()) {
        	reorderInvoiceValidationRules(invoiceValidationRule, false);
        }
    }

	public void reorderInvoiceValidationRules(InvoiceValidationRule invoiceValidationRule, boolean remove) {
		InvoiceType invoiceType = invoiceTypeService.refreshOrRetrieve(invoiceValidationRule.getInvoiceType());
		List<InvoiceValidationRule> invoiceValidationRules = invoiceType.getInvoiceValidationRules()
				.stream().filter(rule -> Objects.isNull(rule.getParentRule())).collect(Collectors.toList());

		int rulePriority = invoiceValidationRule.getPriority();

		if (CollectionUtils.isEmpty(invoiceValidationRules)) {
			invoiceValidationRule.setPriority(1);
		} else if (rulePriority <= 0 || rulePriority > invoiceValidationRules.size() + 1) {
			invoiceValidationRule.setPriority(invoiceValidationRules.size() + 1);
		}

		Predicate<InvoiceValidationRule> ruleFilter = rule -> rule.getPriority() >= rulePriority;
		Predicate<InvoiceValidationRule> ruleFilterRemove = rule -> rule.getPriority() > rulePriority;

		invoiceValidationRules.stream().filter(remove ? ruleFilterRemove : ruleFilter).distinct()
				.forEach(currentRule -> {
					if (!currentRule.getId().equals(invoiceValidationRule.getId())) {
						currentRule = refreshOrRetrieve(currentRule);
						currentRule.setPriority(remove ? currentRule.getPriority() - 1 : currentRule.getPriority() + 1);
						update(currentRule);
					}
				});
	}

    public Long nextSequenceId() {
        NativeQuery query = getEntityManager().unwrap(Session.class).createNativeQuery("SELECT nextval('billing_invoice_validation_rule_seq')");
        return ((long) query.getSingleResult()) + 1L;
    }


}
