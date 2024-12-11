package org.meveo.admin.job;

import static jakarta.ejb.TransactionAttributeType.REQUIRED;
import static java.util.Optional.ofNullable;
import static org.meveo.model.dunning.DunningLevelInstanceStatusEnum.DONE;
import static org.meveo.model.payments.ActionChannelEnum.EMAIL;
import static org.meveo.model.payments.ActionChannelEnum.LETTER;
import static org.meveo.model.payments.ActionModeEnum.AUTOMATIC;
import static org.meveo.model.payments.ActionTypeEnum.SCRIPT;
import static org.meveo.model.payments.ActionTypeEnum.SEND_NOTIFICATION;
import static org.meveo.model.shared.DateUtils.addDaysToDate;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.dunning.DunningActionInstance;
import org.meveo.model.dunning.DunningActionInstanceStatusEnum;
import org.meveo.model.dunning.DunningCollectionPlan;
import org.meveo.model.dunning.DunningDetermineLevelBy;
import org.meveo.model.dunning.DunningLevel;
import org.meveo.model.dunning.DunningLevelInstance;
import org.meveo.model.dunning.DunningLevelInstanceStatusEnum;
import org.meveo.model.dunning.DunningModeEnum;
import org.meveo.model.dunning.DunningPolicy;
import org.meveo.model.dunning.DunningPolicyLevel;
import org.meveo.model.dunning.DunningSettings;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.model.shared.ContactInformation;
import org.meveo.model.shared.Name;
import org.meveo.model.shared.Title;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.DunningActionInstanceService;
import org.meveo.service.payments.impl.DunningCollectionPlanService;
import org.meveo.service.payments.impl.DunningLevelInstanceService;
import org.meveo.service.payments.impl.DunningLevelService;
import org.meveo.service.payments.impl.DunningPolicyService;
import org.meveo.service.payments.impl.DunningSettingsService;
import org.meveo.service.script.ScriptInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.inject.Inject;

@Stateless
public class TriggerReminderDunningLevelJobBean extends BaseJobBean {

    private static final long serialVersionUID = -3301732194304559773L;

    private static final Logger log = LoggerFactory.getLogger(TriggerReminderDunningLevelJobBean.class);

    private static final String MESSAGE_LOGE = "dunning level instance with status ignored has been created for invoice {} - Dunning Level Instance id: {}";

    private static final String CUSTOMER_ACCOUNT = "customerAccount";

    private static final String DUNNING_ACTIONS = "dunningActions";

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    @Inject
    private DunningPolicyService policyService;

    @Inject
    private DunningLevelInstanceService dunningLevelInstanceService;

    @Inject
    private DunningLevelService levelService;

    @Inject
    private BillingAccountService billingAccountService;

    @Inject
    private CustomerAccountService customerAccountService;

    @Inject
    private DunningCollectionPlanService collectionPlanService;

    @Inject
    private InvoiceService invoiceService;

    @Inject
    private DunningSettingsService dunningSettingsService;

    @Inject
    private DunningActionInstanceService actionInstanceService;

    /**
     * Execute the job
     *
     * @param jobExecutionResult Job execution result
     * @param jobInstance        Job instance
     */
    @TransactionAttribute(REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        List<DunningPolicy> policies = policyService.getPolicies(true);
        DunningSettings dunningSettings = dunningSettingsService.findLastOne();

        try {
            int numberOFAllInvoicesProcessed = 0;
            for (DunningPolicy policy : policies) {
                DunningCollectionPlan dunningCollectionPlan = collectionPlanService.findByPolicy(policy);
                boolean cpProcessed = false;
                for (DunningPolicyLevel policyLevel : policy.getDunningLevels()) {
                    if (policyLevel.getDunningLevel() != null && policyLevel.getDunningLevel().isReminder()) {
                        List<Invoice> invoices = policyService.findEligibleInvoicesForPolicy(policy);
                        cpProcessed = processInvoices(invoices, policyLevel, dunningCollectionPlan, dunningSettings, policy);
                        jobExecutionResult.setNbItemsToProcess(jobExecutionResult.getNbItemsToProcess() + invoices.size());
                        numberOFAllInvoicesProcessed += invoices.size();
                    }
                }

                if(dunningCollectionPlan != null && cpProcessed) {
                    dunningCollectionPlan.setLastActionDate(new Date());
                    collectionPlanService.update(dunningCollectionPlan);
                }
            }
            jobExecutionResult.addNbItemsCorrectlyProcessed(numberOFAllInvoicesProcessed - jobExecutionResult.getNbItemsProcessedWithError());
        } catch (Exception exception) {
            jobExecutionResult.addErrorReport(exception.getMessage());
        }
    }

    /**
     * Process invoices
     *
     * @param invoices         Invoices
     * @param policyLevel      Policy level
     * @param pDunningSettings Dunning settings
     * @param pDunningPolicy   Dunning policy
     * @return True if processed
     */
    private boolean processInvoices(List<Invoice> invoices, DunningPolicyLevel policyLevel, DunningCollectionPlan dunningCollectionPlan, DunningSettings pDunningSettings, DunningPolicy pDunningPolicy) {
        Date today = new Date();
        boolean processed = false;
        DunningLevel reminderLevel = levelService.findById(policyLevel.getDunningLevel().getId(), List.of(DUNNING_ACTIONS));

        if(pDunningSettings != null) {
            if (pDunningSettings.getDunningMode().equals(DunningModeEnum.INVOICE_LEVEL) && pDunningPolicy.getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE)) {
                processed = processInvoicesWithDaysOverdue(invoices, policyLevel, dunningCollectionPlan, pDunningSettings, pDunningPolicy, reminderLevel, today);
            }

            if (pDunningSettings.getDunningMode().equals(DunningModeEnum.INVOICE_LEVEL) && pDunningPolicy.getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE_AND_BALANCE_THRESHOLD)) {
                processed = processInvoicesWithDaysOverdueAndBalanceThreshold(invoices, policyLevel, dunningCollectionPlan, pDunningSettings, pDunningPolicy, reminderLevel, today);
            }
        }

        return processed;
    }

    /**
     * Process invoices with days overdue
     *
     * @param invoices         Invoices
     * @param policyLevel      Policy level
     * @param pDunningSettings Dunning settings
     * @param pDunningPolicy   Dunning policy
     * @param reminderLevel    Reminder level
     * @param today            Today
     * @return True if processed
     */
    private boolean processInvoicesWithDaysOverdue(List<Invoice> invoices, DunningPolicyLevel policyLevel, DunningCollectionPlan dunningCollectionPlan, DunningSettings pDunningSettings, DunningPolicy pDunningPolicy, DunningLevel reminderLevel, Date today) {
        boolean processed = false;

        for (Invoice invoice : invoices) {
            Date dateToCompare = addDaysToDate(invoice.getDueDate(), reminderLevel.getDaysOverdue());
            // Get billing account and customer account from invoice
            BillingAccount billingAccount = billingAccountService.findById(invoice.getBillingAccount().getId(), List.of(CUSTOMER_ACCOUNT));
            CustomerAccount customerAccount = customerAccountService.findById(billingAccount.getCustomerAccount().getId());

            if (simpleDateFormat.format(dateToCompare).equals(simpleDateFormat.format(today)) && !invoice.isReminderLevelTriggered()) {
                DunningLevelInstance dunningLevelInstance = launchActions(customerAccount, invoice, policyLevel, dunningCollectionPlan);
                markInvoiceAsReminderAlreadySent(invoice);
                updateDunningLevelInstance(dunningLevelInstance);
                processed = true;
            } else {
                // Create a new level instance with status ignored
                createIgnoredDunningLevelInstance(customerAccount, invoice, policyLevel);
            }
        }

        return processed;
    }

    /**
     * Process invoices with days overdue and balance threshold
     *
     * @param invoices         Invoices
     * @param policyLevel      Policy level
     * @param pDunningSettings Dunning settings
     * @param pDunningPolicy   Dunning policy
     * @param reminderLevel    Reminder level
     * @param today            Today
     * @return True if processed
     */
    private boolean processInvoicesWithDaysOverdueAndBalanceThreshold(List<Invoice> invoices, DunningPolicyLevel policyLevel, DunningCollectionPlan dunningCollectionPlan, DunningSettings pDunningSettings, DunningPolicy pDunningPolicy, DunningLevel reminderLevel, Date today) {
        boolean processed = false;

        for (Invoice invoice : invoices) {
            Date dateToCompare = addDaysToDate(invoice.getDueDate(), reminderLevel.getDaysOverdue());
            // Get billing account and customer account from invoice
            BillingAccount billingAccount = billingAccountService.findById(invoice.getBillingAccount().getId(), List.of(CUSTOMER_ACCOUNT));
            CustomerAccount customerAccount = customerAccountService.findById(billingAccount.getCustomerAccount().getId());

            if ((simpleDateFormat.format(dateToCompare).equals(simpleDateFormat.format(today)) && !invoice.isReminderLevelTriggered()) && invoice.getNetToPay().compareTo(reminderLevel.getMinBalance()) > 0) {
                DunningLevelInstance dunningLevelInstance = launchActions(customerAccount, invoice, policyLevel, dunningCollectionPlan);
                markInvoiceAsReminderAlreadySent(invoice);
                updateDunningLevelInstance(dunningLevelInstance);
                processed = true;
            } else {
                // Create a new level instance with status ignored
                createIgnoredDunningLevelInstance(customerAccount, invoice, policyLevel);
            }
        }

        return processed;
    }

    /**
     * Create a new level instance with status ignored
     *
     * @param customerAccount Customer account
     * @param invoice Invoice
     * @param policyLevel Policy level
     */
    private void createIgnoredDunningLevelInstance(CustomerAccount customerAccount, Invoice invoice, DunningPolicyLevel policyLevel) {
        DunningLevelInstance ignoredDunningLevelInstance = dunningLevelInstanceService.createIgnoredDunningLevelInstance(customerAccount, invoice, policyLevel);
        log.debug(MESSAGE_LOGE, invoice.getId(), ignoredDunningLevelInstance.getId());
    }

    /**
     * Mark invoice as reminder already sent
     *
     * @param invoice Invoice
     */
    private void markInvoiceAsReminderAlreadySent(Invoice invoice) {
        invoice.setReminderLevelTriggered(true);
        invoiceService.update(invoice);
    }

    /**
     * Launch actions
     *
     * @param pInvoice Invoice
     * @param pDunningPolicyLevel Policy level
     * @return A new level instance
     */
    private DunningLevelInstance launchActions(CustomerAccount pCustomerAccount, Invoice pInvoice, DunningPolicyLevel pDunningPolicyLevel, DunningCollectionPlan pDunningCollectionPlan) {
        // Check if a dunning level instance already exists for the pInvoice
        List<DunningLevelInstance> dunningLevelInstances = dunningLevelInstanceService.findByInvoice(pInvoice);

        DunningLevelInstance reminderDunningLevelInstance = null;
        if (dunningLevelInstances != null && !dunningLevelInstances.isEmpty()) {
            // Check if we have already processed the pInvoice for the current level
            for (DunningLevelInstance dunningLevelInstance : dunningLevelInstances) {
                if (dunningLevelInstance.getDunningLevel().getId().equals(pDunningPolicyLevel.getDunningLevel().getId())) {
                    reminderDunningLevelInstance = dunningLevelInstance;
                }
            }
        }

        if (reminderDunningLevelInstance == null) {
            reminderDunningLevelInstance = dunningLevelInstanceService.createDunningLevelInstanceWithoutCollectionPlan(pCustomerAccount, pInvoice, pDunningPolicyLevel, DunningLevelInstanceStatusEnum.IN_PROGRESS);
        }

        for (DunningActionInstance action : reminderDunningLevelInstance.getActions()) {
            if (action.getActionMode().equals(AUTOMATIC) && (action.getActionType().equals(SCRIPT) || action.getActionType().equals(SEND_NOTIFICATION))) {
                actionInstanceService.triggerAction(action, pDunningCollectionPlan);
                action.setActionStatus(DunningActionInstanceStatusEnum.DONE);
                action.setExecutionDate(new Date());
            }
        }

        return reminderDunningLevelInstance;
    }

    /**
     * Update a level instance
     *
     * @param pDunningLevelInstance Level instance
     */
    private void updateDunningLevelInstance(DunningLevelInstance pDunningLevelInstance) {
        pDunningLevelInstance.setLevelStatus(DONE);
        pDunningLevelInstance.setExecutionDate(new Date());
        dunningLevelInstanceService.update(pDunningLevelInstance);
    }
}