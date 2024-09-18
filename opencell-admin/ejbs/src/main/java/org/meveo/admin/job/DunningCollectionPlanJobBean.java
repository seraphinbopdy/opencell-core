package org.meveo.admin.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.model.billing.Invoice;
import org.meveo.model.dunning.DunningPolicy;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.OperationCategoryEnum;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.DunningPolicyService;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

@Stateless
public class DunningCollectionPlanJobBean extends BaseJobBean {

    @Inject
    private DunningPolicyService policyService;

    @Inject
    private AccountOperationService accountOperationService;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        List<DunningPolicy> policies = policyService.getPolicies(true);
        jobExecutionResult.setNbItemsToProcess(policies.size());

        if (policies != null && !policies.isEmpty()) {
            Map<DunningPolicy, List<Invoice>> eligibleInvoicesByPolicy = new HashMap<>();

            // Sort policies by isDefaultPolicy and policyPriority
            List<DunningPolicy> sortedPolicies = policies.stream()
                    .sorted(Comparator.comparing(DunningPolicy::getIsDefaultPolicy).reversed()
                            .thenComparing(DunningPolicy::getPolicyPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            try {
                for (DunningPolicy policy : sortedPolicies) {
                    List<Invoice> eligibleInvoice = policyService.findEligibleInvoicesForPolicy(policy);
                    if (eligibleInvoice != null && !eligibleInvoice.isEmpty()) {
                        List<Invoice> invoicesWithDebitTransaction = new ArrayList<>();

                        eligibleInvoice.forEach(invoice -> {
                            List<AccountOperation> sdAOs = accountOperationService.listByInvoice(invoice);
                            boolean isDebitTransaction = sdAOs.stream().anyMatch(ao -> ao.getTransactionCategory().equals(OperationCategoryEnum.DEBIT));
                            if (isDebitTransaction)
                                invoicesWithDebitTransaction.add(invoice);
                        });

                        eligibleInvoicesByPolicy.put(policy, invoicesWithDebitTransaction);
                    }
                }
                policyService.processEligibleInvoice(eligibleInvoicesByPolicy, jobExecutionResult);
                jobExecutionResult.addNbItemsCorrectlyProcessed(sortedPolicies.size()
                        - jobExecutionResult.getNbItemsProcessedWithError());
            } catch (Exception exception) {
                jobExecutionResult.addErrorReport(exception.getMessage());
            }
        }
    }
}