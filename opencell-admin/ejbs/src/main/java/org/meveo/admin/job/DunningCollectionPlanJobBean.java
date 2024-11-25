package org.meveo.admin.job;

import org.apache.commons.jexl3.*;
import org.meveo.admin.job.utils.DunningUtils;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.model.billing.Invoice;
import org.meveo.model.dunning.*;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.*;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.DunningPolicyService;
import org.meveo.service.payments.impl.DunningSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
public class DunningCollectionPlanJobBean extends BaseJobBean {

    private static Logger log = LoggerFactory.getLogger(DunningCollectionPlanJobBean.class);

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
                } else if (DunningModeEnum.CUSTOMER_LEVEL.equals(dunningSettings.getDunningMode()) && dunningSettings.getCustomerBalance() != null) {
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
                // Check if the customer account is not already added in the eligibleCustomerAccountsByPolicy and if the balance is greater than the minBalanceTrigger
                if (eligibleCustomerAccountsByPolicy.values().stream().noneMatch(accounts -> accounts.containsKey(customerAccount)) &&
                        policy.getMinBalanceTrigger() != null &&
                        balance.compareTo(BigDecimal.valueOf(policy.getMinBalanceTrigger())) >= 0) {
                    String rules = DunningUtils.getRules(policy);
                    Boolean isEligible = checkCustomerWithCondition(customerAccount, rules);

                    if (Boolean.TRUE.equals(isEligible)) {
                        eligibleCustomerAccounts.put(customerAccount, balance);
                    } else {
                        log.info("Customer Account: {} is not eligible for policy rules: {}", customerAccount.getCode(), rules);
                    }
                }
            });

            if (!eligibleCustomerAccounts.isEmpty()) {
                eligibleCustomerAccountsByPolicy.put(policy, eligibleCustomerAccounts);
            }
        }

        return eligibleCustomerAccountsByPolicy;
    }

    /**
     * Check customer with condition
     * @param customerAccount Customer account
     * @param conditions Condition
     * @return True if customer is eligible
     */
    public static Boolean checkCustomerWithCondition(CustomerAccount customerAccount, String conditions) {
        JexlEngine jexl = new JexlBuilder().create();

        String creditCategoryCode = (customerAccount.getCreditCategory() != null) ? customerAccount.getCreditCategory().getCode() : null;
        String customerCategoryCode = (customerAccount.getCustomer().getCustomerCategory().getCode() != null) ? customerAccount.getCustomer().getCustomerCategory().getCode() : null;
        String paymentMethod = (customerAccount.getPreferredPaymentMethod() != null) ? customerAccount.getPreferredPaymentMethod().getPaymentType().name() : null;
        Boolean company = (customerAccount.getCustomer().getIsCompany() != null) ? customerAccount.getCustomer().getIsCompany() : null;

        JexlContext context = new MapContext();
        context.set("creditCategory", creditCategoryCode);
        context.set("customerCategory", customerCategoryCode);
        context.set("paymentMethod", paymentMethod);
        context.set("isCompany", company);

        JexlExpression expression = jexl.createExpression(conditions);
        return (Boolean) expression.evaluate(context);
    }
}