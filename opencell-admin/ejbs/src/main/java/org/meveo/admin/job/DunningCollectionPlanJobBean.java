package org.meveo.admin.job;

import org.meveo.admin.util.ResourceBundle;
import org.meveo.model.billing.Invoice;
import org.meveo.model.dunning.DunningModeEnum;
import org.meveo.model.dunning.DunningPolicy;
import org.meveo.model.dunning.DunningSettings;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.*;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.DunningPolicyService;
import org.meveo.service.payments.impl.DunningSettingsService;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
public class DunningCollectionPlanJobBean extends BaseJobBean {

    @Inject
    private DunningPolicyService dunningPolicyService;

    @Inject
    private AccountOperationService accountOperationService;

    @Inject
    private DunningSettingsService dunningSettingsService;

    @Inject
    CustomerAccountService customerAccountService;

    @Inject
    protected ResourceBundle resourceMessages;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        // Get the list of active policies
        List<DunningPolicy> policies = dunningPolicyService.getPolicies(true);
        // Update job execution result
        jobExecutionResult.setNbItemsToProcess(policies.size());
        // Get the last dunning setting configuration
        DunningSettings dunningSettings = dunningSettingsService.findLastOne();
        // Collection Plan number
        int dunningCollectionPlanNumber = 0;

        try {
            if (!policies.isEmpty()) {
                if (DunningModeEnum.INVOICE_LEVEL.equals(dunningSettings.getDunningMode())) {
                    dunningCollectionPlanNumber = processCollectionPlanForInvoiceLevel(policies);
                } else if (DunningModeEnum.CUSTOMER_LEVEL.equals(dunningSettings.getDunningMode())) {
                    dunningCollectionPlanNumber = processCollectionPlanForCustomerLevel(dunningSettings, policies);
                }
            }

            // Add report information's
            if (dunningCollectionPlanNumber != 0) {
                jobExecutionResult.addReport(resourceMessages.getString("jobExecution.dunning.collection.plan.lines.number", dunningCollectionPlanNumber));
            }

            // Add number of items correctly processed
            jobExecutionResult.addNbItemsCorrectlyProcessed(policies.size() - jobExecutionResult.getNbItemsProcessedWithError());
        } catch (Exception exception) {
            jobExecutionResult.addErrorReport(exception.getMessage());
        }
    }

    /**
     * Process collection plan for invoice level
     * @param policies List of {@link DunningPolicy}
     * @return Number of collection plan
     */
    private int processCollectionPlanForInvoiceLevel(List<DunningPolicy> policies) {
        // Sort policies by isDefaultPolicy and policyPriority
        List<DunningPolicy> sortedPolicies = sortDunningPolicies(policies);
        // Initialize a list of eligible invoices
        Map<DunningPolicy, List<Invoice>> eligibleInvoicesByPolicy = new HashMap<>();

        for (DunningPolicy policy : sortedPolicies) {
            // Get the list of eligible invoices by policy
            List<Invoice> eligibleInvoice = dunningPolicyService.findEligibleInvoicesForPolicy(policy);

            if (eligibleInvoice != null && !eligibleInvoice.isEmpty()) {
                List<Invoice> invoicesWithDebitTransaction = new ArrayList<>();
                eligibleInvoice.forEach(invoice -> {
                    List<AccountOperation> sdAOs = accountOperationService.listByInvoice(invoice);
                    boolean isDebitTransaction = sdAOs.stream().anyMatch(ao -> ao.getTransactionCategory().equals(OperationCategoryEnum.DEBIT));
                    if (isDebitTransaction && eligibleInvoicesByPolicy.values().stream().noneMatch(invoices -> invoices.contains(invoice))) {
                        invoicesWithDebitTransaction.add(invoice);
                    }
                });

                if (!invoicesWithDebitTransaction.isEmpty()) {
                    eligibleInvoicesByPolicy.put(policy, invoicesWithDebitTransaction);
                }
            }
        }

        return dunningPolicyService.processEligibleInvoice(eligibleInvoicesByPolicy);
    }

    /**
     * Sort policies by isDefaultPolicy and policyPriority
     * @param policies List of {@link DunningPolicy}
     * @return Sorted list of {@link DunningPolicy}
     */
    private static List<DunningPolicy> sortDunningPolicies(List<DunningPolicy> policies) {
        return policies
                .stream()
                .sorted(Comparator
                        .comparing(DunningPolicy::getIsDefaultPolicy)
                        .reversed()
                        .thenComparing(DunningPolicy::getPolicyPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private int processCollectionPlanForCustomerLevel(DunningSettings dunningSettings, List<DunningPolicy> policies) {
        List<DunningPolicy> sortedPolicies = sortDunningPolicies(policies);
        CustomerBalance customerBalance = dunningSettings.getCustomerBalance();
        List<CustomerAccount> customerAccounts = customerAccountService.getListCustomerAccountNotUsedInDunning();
        List<String> linkedOccTemplates = getOccTemplateCodesToUse(customerBalance);
        Map<CustomerAccount, BigDecimal> customerAccountsBalance = new HashMap<>();
        customerAccounts.forEach(customerAccount -> {
            BigDecimal balance = customerAccountService.getCustomerAccountBalance(customerAccount, linkedOccTemplates);
            customerAccountsBalance.put(customerAccount, balance);
        });
        Map<DunningPolicy, Map<CustomerAccount, BigDecimal>> eligibleCustomerAccountsByPolicy = getEligibleCustomerAccount(sortedPolicies, customerAccountsBalance);
        return dunningPolicyService.processEligibleCustomerAccounts(eligibleCustomerAccountsByPolicy);
    }

    /**
     * Retrieves the OCC template codes to use based on the provided {@link CustomerBalance}.
     *
     * @param customerBalance The {@link CustomerBalance} from which to retrieve OCC template codes.
     * @return A {@link List} of OCC template codes.
     */
    private List<String> getOccTemplateCodesToUse(CustomerBalance customerBalance) {
        List<String> listOccTemplateCodes = Collections.emptyList();

        if (customerBalance.getOccTemplates() != null && !customerBalance.getOccTemplates().isEmpty()) {
            listOccTemplateCodes = customerBalance.getOccTemplates().stream()
                    .map(OCCTemplate::getCode)
                    .collect(Collectors.toList());
        }

        return listOccTemplateCodes;
    }

    /**
     * Get eligible customer account
     * @param sortedPolicies Sorted policies
     * @param customerAccountsBalance Customer Account balance
     * @return A map of dunning policies, customer account and balance
     */
    private static Map<DunningPolicy, Map<CustomerAccount, BigDecimal>> getEligibleCustomerAccount(List<DunningPolicy> sortedPolicies, Map<CustomerAccount, BigDecimal> customerAccountsBalance) {
        Map<DunningPolicy, Map<CustomerAccount, BigDecimal>> eligibleCustomerAccountsByPolicy = new HashMap<>();
        for (DunningPolicy policy : sortedPolicies) {
            Map<CustomerAccount, BigDecimal> eligibleCustomerAccounts = new HashMap<>();
            customerAccountsBalance.forEach((customerAccount, balance) -> {
                if (policy.getMinBalanceTrigger() != null && balance.compareTo(BigDecimal.valueOf(policy.getMinBalanceTrigger())) >= 0) {
                    eligibleCustomerAccounts.put(customerAccount, balance);
                }
            });
            eligibleCustomerAccountsByPolicy.put(policy, eligibleCustomerAccounts);
        }
        return eligibleCustomerAccountsByPolicy;
    }
}