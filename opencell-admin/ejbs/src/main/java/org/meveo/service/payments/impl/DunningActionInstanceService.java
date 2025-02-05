package org.meveo.service.payments.impl;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import jakarta.inject.Inject;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.dunning.*;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.shared.Name;
import org.meveo.model.shared.Title;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

import static java.util.Optional.ofNullable;
import static org.meveo.model.payments.ActionChannelEnum.EMAIL;
import static org.meveo.model.payments.ActionTypeEnum.*;

@Stateless
public class DunningActionInstanceService extends PersistenceService<DunningActionInstance> {

    @Inject
    private DunningSettingsService dunningSettingsService;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    private CustomerAccountService customerAccountService;

    @Inject
    private DunningCollectionPlanService collectionPlanService;

    @Inject
    private BillingAccountService billingAccountService;

    @Inject
    private InvoiceService invoiceService;

    private final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	
    /**
     * Find a dunning action instance by code and dunning level instance
     *
     * @param code Code
     * @param dunningLevelInstance Level instance
     * @return A dunning action instance
     */
	public DunningActionInstance findByCodeAndDunningLevelInstance(String code, Long dunningLevelInstance) {
		QueryBuilder qb = new QueryBuilder(DunningActionInstance.class, "d", Arrays.asList("dunningLevelInstance"));
		qb.addCriterion("d.code", "=", code, true);
		qb.addCriterion("d.dunningLevelInstance.id", "=", dunningLevelInstance, false);

        try {
            return (DunningActionInstance) qb.getQuery(getEntityManager()).getSingleResult();
        } catch (NoResultException exception) {
            return null;
        }
	}

    /**
     * Update the status of a dunning action instance
     *
     * @param actionStatus Action status
     * @param dunningLevelInstance Level instance
     * @return The number of updated records
     */
	public int updateStatus(DunningActionInstanceStatusEnum actionStatus, DunningLevelInstance dunningLevelInstance) {
        return getEntityManager()
                .createNamedQuery("DunningActionInstance.updateStatus")
                .setParameter("actionStatus", actionStatus)
                .setParameter("dunningLevelInstance", dunningLevelInstance)
                .executeUpdate();
    }

    /**
     * Create a list of action instances
     *
     * @param pDunningPolicyLevel   Policy level
     * @param pDunningLevelInstance Level instance
     * @return A list of action instances
     */
    public List<DunningActionInstance> createDunningActionInstances(DunningCollectionPlan collectionPlan, DunningPolicyLevel pDunningPolicyLevel, DunningLevelInstance pDunningLevelInstance) {
        List<DunningActionInstance> actionInstances = new ArrayList<>();

        for (DunningAction action : pDunningPolicyLevel.getDunningLevel().getDunningActions()) {
            DunningActionInstance dunningActionInstance = createDunningActionInstance(action, pDunningLevelInstance);
            dunningActionInstance.setDunningLevelInstance(pDunningLevelInstance);
            dunningActionInstance.setCode(action.getCode());
            dunningActionInstance.setDescription(action.getDescription());
            dunningActionInstance.setCfValues(action.getCfValues());
            dunningActionInstance.setExecutionDate(pDunningLevelInstance.getExecutionDate());

            if(collectionPlan != null) {
                dunningActionInstance.setCollectionPlan(collectionPlan);
}

            this.create(dunningActionInstance);
            actionInstances.add(dunningActionInstance);
        }

        return actionInstances;
    }

    /**
     * Create a dunning action instance
     *
     * @param action Dunning action
     * @param pDunningLevelInstance Level instance
     * @return A dunning action instance
     */
    public DunningActionInstance createDunningActionInstance(DunningAction action, DunningLevelInstance pDunningLevelInstance) {
        DunningActionInstance dunningActionInstance = new DunningActionInstance();
        dunningActionInstance.setDunningAction(action);
        dunningActionInstance.setActionType(action.getActionType());
        dunningActionInstance.setActionMode(action.getActionMode());
        dunningActionInstance.setActionOwner(action.getAssignedTo());

        if (pDunningLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
            dunningActionInstance.setActionStatus(DunningActionInstanceStatusEnum.DONE);
            dunningActionInstance.setExecutionDate(new Date());
        } else {
            dunningActionInstance.setActionStatus(DunningActionInstanceStatusEnum.TO_BE_DONE);
        }

        return dunningActionInstance;
    }

    /**
     * Trigger action
     *
     * @param actionInstance Action instance
     * @param collectionPlan Collection plan
     */
    public void triggerAction(DunningActionInstance actionInstance, Invoice pInvoice, DunningCollectionPlan collectionPlan) {
        // Execute script
        if (actionInstance.getActionType().equals(SCRIPT) && actionInstance.getDunningAction() != null) {
            executeScriptAction(dunningSettingsService.findLastOne(), actionInstance, pInvoice);
        }

        // Send notification
        if (actionInstance.getActionType().equals(SEND_NOTIFICATION) && actionInstance.getDunningAction().getActionChannel().equals(EMAIL)) {
            executeNotificationAction(dunningSettingsService.findLastOne(), actionInstance, pInvoice, collectionPlan);
        }

        // Retry payment
        if (actionInstance.getActionType().equals(RETRY_PAYMENT)) {
            collectionPlanService.launchPaymentAction(collectionPlan);
        }
    }

    /**
     * Execute script action
     *
     * @param dunningSettings Dunning settings
     * @param actionInstance Action instance
     * @param pInvoice Invoice
     */
    private void executeScriptAction(DunningSettings dunningSettings, DunningActionInstance actionInstance, Invoice pInvoice) {
        HashMap<String, Object> context = new HashMap<>();
        context.put(Script.CONTEXT_ENTITY, pInvoice);
        context.put("customerAccount", pInvoice.getBillingAccount().getCustomerAccount());

        if (dunningSettings != null) {
            context.put("dunningMode", dunningSettings.getDunningMode());
        }

        scriptInstanceService.execute(actionInstance.getDunningAction().getScriptInstance().getCode(), context);
    }

    /**
     * Execute notification action
     *
     * @param dunningSettings Dunning settings
     * @param actionInstance Action instance
     * @param collectionPlan Collection plan
     */
    private void executeNotificationAction(DunningSettings dunningSettings, DunningActionInstance actionInstance, Invoice pInvoice, DunningCollectionPlan collectionPlan) {
        if(dunningSettings != null && dunningSettings.getDunningMode() == DunningModeEnum.INVOICE_LEVEL) {
            sendEmail(actionInstance.getDunningAction().getActionNotificationTemplate(), collectionPlan, pInvoice);
        } else if (dunningSettings != null && dunningSettings.getDunningMode() == DunningModeEnum.CUSTOMER_LEVEL) {
            customerAccountService.sendEmail(actionInstance.getDunningAction().getActionNotificationTemplate(), actionInstance.getCollectionPlan());
        }
    }

    /**
     * Send email
     *
     * @param pEmailTemplate Email template
     * @param pCollectionPlan Collection Plan
     * @param pInvoice Invoice
     */
    private void sendEmail(EmailTemplate pEmailTemplate, DunningCollectionPlan pCollectionPlan, Invoice pInvoice) {
        // Get the invoice from collection plan
        Invoice invoice = pInvoice != null ? pInvoice : pCollectionPlan.getRelatedInvoice();

        if (invoice.getSeller() != null
                && invoice.getSeller().getContactInformation() != null
                && invoice.getSeller().getContactInformation().getEmail() != null
                && !invoice.getSeller().getContactInformation().getEmail().isBlank()) {
            // Get Billing Account and Customer Account
            BillingAccount billingAccount = billingAccountService.findById(invoice.getBillingAccount().getId(), List.of("customerAccount"));
            CustomerAccount customerAccount = customerAccountService.findById(billingAccount.getCustomerAccount().getId());
            // prepare params
            Map<Object, Object> params = new HashMap<>();
            // Set billing account, Customer Account, Invoice and Collection Plan information's
            setBillingAccountInfos(params, billingAccount);
            setCustomerAccountInfos(params, customerAccount);
            setCollectionPlanInfos(params, pCollectionPlan);
            setInvoiceInfos(params, invoice);
            // prepare attachments
            List<File> attachments = setAttachment(invoice);

            // send notification
            sendNotification(billingAccount, invoice.getSeller(), pEmailTemplate, params, attachments);
        } else {
            throw new BusinessException("The email sending skipped because the from email is missing for the seller : " + invoice.getSeller().getCode());
        }
    }

    /**
     * Set billing account infos
     *
     * @param params Params
     * @param billingAccount Billing account {@link BillingAccount}
     */
    private void setBillingAccountInfos(Map<Object, Object> params, BillingAccount billingAccount) {
        params.put("billingAccountDescription", billingAccount.getDescription());
        params.put("billingAccountAddressAddress1", billingAccount.getAddress() != null ? billingAccount.getAddress().getAddress1() : StringUtils.EMPTY);
        params.put("billingAccountAddressZipCode", billingAccount.getAddress() != null ? billingAccount.getAddress().getZipCode() : StringUtils.EMPTY);
        params.put("billingAccountAddressCity", billingAccount.getAddress() != null ? billingAccount.getAddress().getCity() : StringUtils.EMPTY);
        params.put("billingAccountContactInformationPhone", billingAccount.getContactInformation() != null ? billingAccount.getContactInformation().getPhone() : StringUtils.EMPTY);
        params.put("billingAccountLegalEntityTypeCode", billingAccount.getLegalEntityType() != null ? billingAccount.getLegalEntityType().getCode() : StringUtils.EMPTY);
        params.put("billingAccountCcedEmails", billingAccount.getCcedEmails());
        params.put("contactInformationEmail", billingAccount.getContactInformation() != null ? billingAccount.getContactInformation().getEmail() : StringUtils.EMPTY);
        params.put("contactInformationPhone", billingAccount.getContactInformation() != null ? billingAccount.getContactInformation().getPhone() : StringUtils.EMPTY);
        params.put("contactInformationMobile", billingAccount.getContactInformation() != null ? billingAccount.getContactInformation().getMobile() : StringUtils.EMPTY);

        if (Boolean.TRUE.equals(billingAccount.getIsCompany())) {
            params.put("customerAccountLegalEntityTypeCode", ofNullable(billingAccount.getLegalEntityType()).map(Title::getCode).orElse(StringUtils.EMPTY));
        } else {
            Name name = billingAccount.getName();
            Title title = ofNullable(name).map(Name::getTitle).orElse(null);
            params.put("customerAccountLegalEntityTypeCode", ofNullable(title).map(Title::getDescription).orElse(StringUtils.EMPTY));
        }
    }

    /**
     * Set customer account infos
     *
     * @param params Params
     * @param customerAccount Customer account {@link CustomerAccount}
     */
    private void setCustomerAccountInfos(Map<Object, Object> params, CustomerAccount customerAccount) {
        params.put("customerAccountAddressAddress1", customerAccount.getAddress() != null ? customerAccount.getAddress().getAddress1() : StringUtils.EMPTY);
        params.put("customerAccountAddressZipCode", customerAccount.getAddress() != null ? customerAccount.getAddress().getZipCode() : StringUtils.EMPTY);
        params.put("customerAccountAddressCity", customerAccount.getAddress() != null ? customerAccount.getAddress().getCity() : StringUtils.EMPTY);
        params.put("customerAccountDescription", customerAccount.getDescription());
        params.put("customerAccountLastName", customerAccount.getName() != null ? customerAccount.getName().getLastName() : StringUtils.EMPTY);
        params.put("customerAccountFirstName", customerAccount.getName() != null ? customerAccount.getName().getFirstName() : StringUtils.EMPTY);
    }

    /**
     * Set invoice infos
     *
     * @param params Params
     * @param invoice Invoice {@link Invoice}
     */
    private void setInvoiceInfos(Map<Object, Object> params, Invoice invoice) {
        params.put("invoiceInvoiceNumber", invoice.getInvoiceNumber());
        params.put("invoiceOrderOrderNumber", invoice.getOpenOrderNumber());
        params.put("invoicePaymentMethodType", invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().getPaymentType().name() : StringUtils.EMPTY);
        params.put("invoiceAmountWithoutTax", invoice.getAmountWithoutTax());
        params.put("invoiceAmountWithTax", invoice.getAmountWithTax());
        params.put("invoicePaymentStatus", invoice.getPaymentStatus().name());
        params.put("invoiceTotal", invoice.getAmountWithTax());
        params.put("invoiceDueDate", formatter.format(invoice.getDueDate()));
        params.put("dayDate", formatter.format(new Date()));
    }

    /**
     * Set collection plan infos
     *
     * @param params Params
     * @param pCollectionPlan Collection Plan {@link DunningCollectionPlan}
     */
    private void setCollectionPlanInfos(Map<Object, Object> params, DunningCollectionPlan pCollectionPlan) {
        params.put("dunningCollectionPlanId", pCollectionPlan != null ? pCollectionPlan.getCollectionPlanNumber() : StringUtils.EMPTY);
        params.put("dunningCollectionPlanStatusStatus", pCollectionPlan != null ? pCollectionPlan.getStatus().getStatus().name() : StringUtils.EMPTY);
        params.put("dunningCollectionPlanLastAction", pCollectionPlan != null ? pCollectionPlan.getLastAction() : StringUtils.EMPTY);
        String lastActionDate = pCollectionPlan != null && pCollectionPlan.getLastActionDate() != null ? formatter.format(pCollectionPlan.getLastActionDate()) : null;
        params.put("dunningCollectionPlanLastActionDate", lastActionDate != null ? formatter.format(lastActionDate) : StringUtils.EMPTY);
    }

    /**
     * Set attachment
     *
     * @param invoice Invoice
     * @return A list of files
     */
    private List<File> setAttachment(Invoice invoice) {
        List<File> attachments = new ArrayList<>();
        String invoiceFileName = invoiceService.getFullPdfFilePath(invoice, false);
        File attachment = new File(invoiceFileName);

        if (attachment.exists()) {
            attachments.add(attachment);
        } else {
            log.warn("No Pdf file exists for the invoice : {}", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getTemporaryInvoiceNumber());
        }

        return attachments;
    }

    /**
     * Send notification
     *
     * @param billingAccount Billing account
     * @param seller Seller
     * @param emailTemplate Email template
     * @param params Params
     * @param attachments Attachments
     */
    private void sendNotification(BillingAccount billingAccount, Seller seller, EmailTemplate emailTemplate, Map<Object, Object> params, List<File> attachments) {
        if (billingAccount.getContactInformation() != null && billingAccount.getContactInformation().getEmail() != null) {
            try {
                collectionPlanService.sendNotification(seller.getContactInformation().getEmail(), billingAccount, emailTemplate, params, attachments);
            } catch (Exception exception) {
                throw new BusinessException(exception.getMessage());
            }
        } else {
            throw new BusinessException("The email is missing for the billing account : " + billingAccount.getCode());
        }
    }
}
