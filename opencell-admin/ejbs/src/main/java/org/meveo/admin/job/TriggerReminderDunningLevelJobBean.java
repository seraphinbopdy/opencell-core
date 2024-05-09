package org.meveo.admin.job;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.dunning.*;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.model.shared.ContactInformation;
import org.meveo.model.shared.Name;
import org.meveo.model.shared.Title;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.payments.impl.*;
import org.meveo.service.script.ScriptInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Optional.ofNullable;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static org.meveo.model.dunning.DunningLevelInstanceStatusEnum.DONE;
import static org.meveo.model.payments.ActionChannelEnum.EMAIL;
import static org.meveo.model.payments.ActionChannelEnum.LETTER;
import static org.meveo.model.payments.ActionModeEnum.AUTOMATIC;
import static org.meveo.model.payments.ActionTypeEnum.SCRIPT;
import static org.meveo.model.payments.ActionTypeEnum.SEND_NOTIFICATION;
import static org.meveo.model.shared.DateUtils.addDaysToDate;

@Stateless
public class TriggerReminderDunningLevelJobBean extends BaseJobBean {

    private static final long serialVersionUID = -3301732194304559773L;

    private static final Logger log = LoggerFactory.getLogger(TriggerReminderDunningLevelJobBean.class);

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    private final SimpleDateFormat emailDateFormatter = new SimpleDateFormat("yyyy-MM-dd");

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
    private ScriptInstanceService scriptInstanceService;

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
                        cpProcessed = processInvoices(invoices, policyLevel.getDunningLevel(), policyLevel, dunningCollectionPlan, dunningSettings, policy);
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
    private boolean processInvoices(List<Invoice> invoices, DunningLevel reminderLevel, DunningPolicyLevel policyLevel, DunningCollectionPlan dunningCollectionPlan, DunningSettings pDunningSettings, DunningPolicy pDunningPolicy) {
        Date today = new Date();
        boolean processed = false;
        reminderLevel = levelService.findById(policyLevel.getDunningLevel().getId(), List.of("dunningActions"));

        if(pDunningSettings != null) {
            if (pDunningSettings.getDunningMode().equals(DunningModeEnum.INVOICE_LEVEL)) {
                if (pDunningPolicy.getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE)) {
                    for (Invoice invoice : invoices) {
                        Date dateToCompare = addDaysToDate(invoice.getDueDate(), reminderLevel.getDaysOverdue());
                        // Get billing account and customer account from invoice
                        BillingAccount billingAccount = billingAccountService.findById(invoice.getBillingAccount().getId(), List.of("customerAccount"));
                        CustomerAccount customerAccount = customerAccountService.findById(billingAccount.getCustomerAccount().getId());

                        if (simpleDateFormat.format(dateToCompare).equals(simpleDateFormat.format(today)) && !invoice.isReminderLevelTriggered()) {
                            DunningLevelInstance dunningLevelInstance = launchActions(billingAccount, customerAccount, invoice, policyLevel);
                            markInvoiceAsReminderAlreadySent(invoice);
                            updateDunningLevelInstance(dunningLevelInstance);
                            processed = true;
                        } else {
                            // Create a new level instance with status ignored
                            DunningLevelInstance ignoredDunningLevelInstance = dunningLevelInstanceService.createIgnoredDunningLevelInstance(customerAccount, invoice, policyLevel);
                            log.debug("A new dunning level instance with status ignored has been created for invoice {} - Dunning Level Instance id: {}", invoice.getId(), ignoredDunningLevelInstance.getId());
                        }
                    }
                } else if (pDunningPolicy.getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE_AND_BALANCE_THRESHOLD)) {
                    for (Invoice invoice : invoices) {
                        Date dateToCompare = addDaysToDate(invoice.getDueDate(), reminderLevel.getDaysOverdue());
                        // Get billing account and customer account from invoice
                        BillingAccount billingAccount = billingAccountService.findById(invoice.getBillingAccount().getId(), List.of("customerAccount"));
                        CustomerAccount customerAccount = customerAccountService.findById(billingAccount.getCustomerAccount().getId());

                        if ((simpleDateFormat.format(dateToCompare).equals(simpleDateFormat.format(today)) && !invoice.isReminderLevelTriggered()) && invoice.getNetToPay().compareTo(reminderLevel.getMinBalance()) > 0) {
                            DunningLevelInstance dunningLevelInstance = launchActions(billingAccount, customerAccount, invoice, policyLevel);
                            markInvoiceAsReminderAlreadySent(invoice);
                            updateDunningLevelInstance(dunningLevelInstance);
                            processed = true;
                        } else {
                            // Create a new level instance with status ignored
                            DunningLevelInstance ignoredDunningLevelInstance = dunningLevelInstanceService.createIgnoredDunningLevelInstance(customerAccount, invoice, policyLevel);
                            log.debug("A new dunning level instance with status ignored has been created for invoice {} - Dunning Level Instance id: {}", invoice.getId(), ignoredDunningLevelInstance.getId());
                        }
                    }
                }
            }
        }

        return processed;
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
     * @param pInvoice           Invoice
     * @param pDunningPolicyLevel Policy level
     * @return A new level instance
     */
    private DunningLevelInstance launchActions(BillingAccount pBillingAccount, CustomerAccount pCustomerAccount, Invoice pInvoice, DunningPolicyLevel pDunningPolicyLevel) {
        // Check if a dunning level instance already exists for the pInvoice
        List<DunningLevelInstance> dunningLevelInstances = dunningLevelInstanceService.findByInvoice(pInvoice);
        if (dunningLevelInstances != null && !dunningLevelInstances.isEmpty()) {
            // Check if we have already processed the pInvoice for the current level
            for (DunningLevelInstance dunningLevelInstance : dunningLevelInstances) {
                if (dunningLevelInstance.getDunningLevel().getId().equals(pDunningPolicyLevel.getDunningLevel().getId())) {
                    return dunningLevelInstance;
                }
            }
        }

        // Create a new level instance
        DunningLevelInstance dunningLevelInstance = dunningLevelInstanceService.createDunningLevelInstanceWithoutCollectionPlan(pCustomerAccount, pInvoice, pDunningPolicyLevel, DunningLevelInstanceStatusEnum.IN_PROGRESS);

        for (DunningActionInstance action : dunningLevelInstance.getActions()) {
            if (action.getActionMode().equals(AUTOMATIC) && (action.getActionType().equals(SCRIPT) || action.getActionType().equals(SEND_NOTIFICATION))) {
                if (action.getActionType().equals(SCRIPT)) {
                    ScriptInstance scriptInstance = action.getDunningAction().getScriptInstance();
                    if (scriptInstance != null) {
                        scriptInstanceService.execute(scriptInstance.getCode(), new HashMap<>());
                    }
                }

                if (action.getActionType().equals(SEND_NOTIFICATION) && (action.getDunningAction().getActionChannel().equals(EMAIL)
                        || action.getDunningAction().getActionChannel().equals(LETTER))) {
                    sendReminderEmail(action.getDunningAction().getActionNotificationTemplate(), pInvoice, pBillingAccount, pCustomerAccount);
                }

                action.setActionStatus(DunningActionInstanceStatusEnum.DONE);
            }
        }

        return dunningLevelInstance;
    }

    /**
     * Send reminder email
     *
     * @param emailTemplate Email template
     * @param invoice       Invoice
     */
    private void sendReminderEmail(EmailTemplate emailTemplate, Invoice invoice, BillingAccount billingAccount, CustomerAccount customerAccount) {
        if(invoice.getSeller() != null && invoice.getSeller().getContactInformation() != null
                && invoice.getSeller().getContactInformation().getEmail() != null
                && !invoice.getSeller().getContactInformation().getEmail().isBlank()) {
            Seller seller = invoice.getSeller();
            Map<Object, Object> params = new HashMap<>();
            params.put("billingAccountDescription", billingAccount.getDescription());
            params.put("billingAccountAddressAddress1", billingAccount.getAddress() != null ? billingAccount.getAddress().getAddress1() : "");
            params.put("billingAccountAddressZipCode", billingAccount.getAddress() != null ? billingAccount.getAddress().getZipCode() : "");
            params.put("billingAccountAddressCity", billingAccount.getAddress() != null ? billingAccount.getAddress().getCity() : "");

            ContactInformation contactInformation = billingAccount.getContactInformation();

            if(contactInformation != null) {
                params.put("contactInformationEmail",  contactInformation.getEmail() != null ? contactInformation.getEmail() : "");
                params.put("contactInformationPhone",  contactInformation.getPhone() != null ? contactInformation.getPhone() : "");
                params.put("contactInformationMobile",  contactInformation.getMobile()  != null ? contactInformation.getMobile() : "");
            }

            params.put("customerAccountFirstName",  customerAccount.getName() != null ? customerAccount.getName().getFirstName() : "");
            params.put("customerAccountLastName",  customerAccount.getName() != null ? customerAccount.getName().getLastName() : "");
            params.put("invoiceInvoiceNumber", invoice.getInvoiceNumber());
            params.put("invoiceDueDate", emailDateFormatter.format(invoice.getDueDate()));
            params.put("invoiceInvoiceDate", emailDateFormatter.format(new Date()));
            DecimalFormat decimalFormat = new DecimalFormat("#,###.00");
            params.put("invoiceAmountWithTax", decimalFormat.format(invoice.getAmountWithTax()));
            params.put("invoiceAmountWithoutTax", decimalFormat.format(invoice.getAmountWithoutTax()));
            params.put("invoicePaymentMethodType", invoice.getPaymentMethodType());
            params.put("invoicePaymentStatus", invoice.getPaymentStatus());
            params.put("invoiceOrderOrderNumber", invoice.getOrder() != null ? invoice.getOrder().getOrderNumber() : "");

            if(Boolean.TRUE.equals(billingAccount.getIsCompany())) {
                params.put("billingAccountLegalEntityTypeCode", ofNullable(billingAccount.getLegalEntityType()).map(Title::getCode).orElse(""));
            } else {
                Name name = ofNullable(billingAccount.getName()).orElse(null);
                Title title = ofNullable(name).map(Name::getTitle).orElse(null);
                params.put("billingAccountLegalEntityTypeCode", ofNullable(title).map(Title::getDescription).orElse(""));
            }

            params.put("customerAccountAddressAddress1", customerAccount.getAddress() != null ? customerAccount.getAddress().getAddress1() : "");
            params.put("customerAccountAddressZipCode", customerAccount.getAddress() != null ? customerAccount.getAddress().getZipCode() : "");
            params.put("customerAccountAddressCity", customerAccount.getAddress() != null ? customerAccount.getAddress().getCity() : "");
            params.put("customerAccountDescription", customerAccount.getDescription());
            params.put("dayDate", emailDateFormatter.format(new Date()));

            List<File> attachments = new ArrayList<>();
            String invoiceFileName = invoiceService.getFullPdfFilePath(invoice, false);
            File attachment = new File(invoiceFileName);

            if (!attachment.exists()) {
                log.warn("No Pdf file exists for the invoice : {}", ofNullable(invoice.getInvoiceNumber()).orElse(invoice.getTemporaryInvoiceNumber()));
            } else {
                attachments.add(attachment);
            }

            if (billingAccount.getContactInformation() != null && billingAccount.getContactInformation().getEmail() != null) {
                collectionPlanService.sendNotification(seller.getContactInformation().getEmail(),
                        billingAccount, emailTemplate, params, attachments);
            } else {
                throw new BusinessException("Billing account email is missing");
            }
        } else {
            throw new BusinessException("From email is missing, email sending skipped");
        }
    }

    /**
     * Update a level instance
     *
     * @param pDunningLevelInstance Level instance
     */
    private void updateDunningLevelInstance(DunningLevelInstance pDunningLevelInstance) {
        pDunningLevelInstance.setLevelStatus(DONE);
        dunningLevelInstanceService.update(pDunningLevelInstance);
    }
}