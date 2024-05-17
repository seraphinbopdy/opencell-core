package org.meveo.admin.job;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.meveo.model.billing.InvoicePaymentStatusEnum.PAID;
import static org.meveo.model.dunning.DunningActionInstanceStatusEnum.DONE;
import static org.meveo.model.dunning.DunningActionInstanceStatusEnum.TO_BE_DONE;
import static org.meveo.model.payments.ActionChannelEnum.EMAIL;
import static org.meveo.model.payments.ActionChannelEnum.LETTER;
import static org.meveo.model.payments.ActionModeEnum.AUTOMATIC;
import static org.meveo.model.payments.ActionTypeEnum.*;
import static org.meveo.model.payments.DunningCollectionPlanStatusEnum.*;
import static org.meveo.model.shared.DateUtils.addDaysToDate;

import org.meveo.admin.exception.BusinessException;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoicePaymentStatusEnum;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.dunning.*;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.*;
import org.meveo.model.shared.DateUtils;
import org.meveo.model.shared.Name;
import org.meveo.model.shared.Title;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.payments.impl.*;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Stateless
public class TriggerCollectionPlanLevelsJobBean extends BaseJobBean {

    @Inject
    private BillingAccountService billingAccountService;

    @Inject
    private CustomerAccountService customerAccountService;

    @Inject
    private DunningCollectionPlanService collectionPlanService;

    @Inject
    private DunningCollectionPlanStatusService collectionPlanStatusService;

    @Inject
    private DunningLevelInstanceService levelInstanceService;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    private InvoiceService invoiceService;

    @Inject
    private DunningActionInstanceService actionInstanceService;

    @Inject
    private DunningSettingsService dunningSettingsService;

    @EJB
    private TriggerCollectionPlanLevelsJobBean jobBean;

    private final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        List<Long> collectionPlanToProcess = collectionPlanService.getActiveCollectionPlansIds();
        jobExecutionResult.setNbItemsToProcess(collectionPlanToProcess.size());
        for (Long collectionPlanId : collectionPlanToProcess) {
            try {
                jobBean.process(collectionPlanId, jobExecutionResult);
            } catch (Exception exception) {
                jobExecutionResult.addErrorReport(exception.getMessage());
            }
        }
        jobExecutionResult.setNbItemsCorrectlyProcessed(collectionPlanToProcess.size() - jobExecutionResult.getNbItemsProcessedWithError());
    }

    /**
     * Process collection plans
     *
     * @param collectionPlanId   Collection plan id to process
     * @param jobExecutionResult Job execution result
     */
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void process(Long collectionPlanId, JobExecutionResultImpl jobExecutionResult) {
        // Get collection plan
        DunningCollectionPlan collectionPlan = collectionPlanService.findById(collectionPlanId);
        Date dateToCompare;
        Date today = new Date();
        int index = 0;
        int nextLevel = 0;
        String lastAction = "";
        String nextAction = "";
        boolean updateCollectionPlan = false;

        // Check if collection plan has levels
        if (collectionPlan.getDunningLevelInstances() == null || collectionPlan.getDunningLevelInstances().isEmpty()) {
            throw new BusinessException("Collection plan ID : " + collectionPlan.getId() + " has no levelInstances associated");
        }

        // Check if collection plan is active and invoice is paid
        if(collectionPlan.getStatus().getStatus() == ACTIVE && collectionPlan.getRelatedInvoice().getPaymentStatus() == PAID) {
            collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
            collectionPlanService.update(collectionPlan);
        } else {
            collectionPlan.getDunningLevelInstances().sort(Comparator.comparing(DunningLevelInstance::getSequence));
            int nbLevelDone = 0;
            DunningLevelInstance levelInstance= null;
            List<DunningLevelInstance> levelInstances = collectionPlan.getDunningLevelInstances();

            for (int levelsIndex = 0 ; levelsIndex < levelInstances.size(); levelsIndex++) {
                levelInstance = levelInstances.get(levelsIndex);
                dateToCompare = getDateOverdue(levelInstance, collectionPlan);
                DunningLevelInstance previousLevelInstance = null;

                if (levelsIndex > 0) {
                    previousLevelInstance = levelInstances.get(levelsIndex - 1);
                }

                if(levelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
                    nbLevelDone++;
                }

                if (levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.TO_BE_DONE) && !collectionPlan.getRelatedInvoice().getPaymentStatus().equals(PAID)) {
                    if ((dateToCompare.before(today)
                            && collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE_AND_BALANCE_THRESHOLD)
                            && collectionPlan.getRelatedInvoice().getRecordedInvoice().getUnMatchingAmount().compareTo(levelInstance.getDunningLevel().getMinBalance()) > 0)
                        || (collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE) && dateToCompare.before(today))) {
                        nextLevel = index + 1;
                        boolean registerKO = false;
                        for (int i = 0; i < levelInstance.getActions().size(); i++) {
                            DunningActionInstance actionInstance = levelInstance.getActions().get(i);
                            if (actionInstance.getActionMode().equals(AUTOMATIC) && actionInstance.getActionStatus().equals(TO_BE_DONE)) {
                                try {
                                    if (previousLevelInstance != null && (previousLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE || previousLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.IGNORED)) {
                                        triggerAction(actionInstance, collectionPlan);
                                        actionInstance.setActionStatus(DunningActionInstanceStatusEnum.DONE);
                                        if (levelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.TO_BE_DONE) {
                                            levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.IN_PROGRESS);
                                            levelInstanceService.update(levelInstance);
                                        }
                                        collectionPlan.setLastActionDate(new Date());
                                        collectionPlan.setLastAction(actionInstance.getDunningAction().getActionType().toString());
                                    }
                                } catch (Exception exception) {
                                    registerKO = true;
                                    jobExecutionResult.addReport("Collection plan ID : "
                                            + collectionPlan.getId() + "/Level instance ID : "
                                            + levelInstance.getId() + "/Action instance ID : " + actionInstance.getId()
                                            + " : " + exception.getMessage());
                                }
                            }
                            actionInstanceService.update(actionInstance);
                        }
                        if (!registerKO) {
                            if (levelsIndex + 1 < levelInstances.size()) {
                                DunningLevelInstance currentLevel = levelInstances.get(levelsIndex);
                                for (int i = levelsIndex + 1; i < levelInstances.size(); i++) {
                                    if(levelInstances.get(i).getActions() != null && !levelInstances.get(i).getActions().isEmpty()) {
                                        collectionPlan.setNextAction(levelInstances.get(i).getActions().get(0).getDunningAction().getActionType().toString());
                                        collectionPlan.setNextActionDate(addDaysToDate(collectionPlan.getStartDate(), levelInstances.get(i).getDaysOverdue() - currentLevel.getDaysOverdue()));
                                        break;
                                    }
                                }
                            } else {
                                if(levelsIndex == levelInstances.size()) {
                                    collectionPlan.setNextAction(null);
                                    collectionPlan.setNextActionDate(null);
                                }
                            }
                            collectionPlanService.update(collectionPlan);
                            collectionPlanService.getEntityManager().flush();
                        }
                        updateCollectionPlan = true;
                        if(registerKO) {
                            jobExecutionResult.addNbItemsProcessedWithError(1L);
                        }
                        levelInstance = levelInstanceService.refreshOrRetrieve(levelInstance);
                        if (nextLevel < collectionPlan.getDunningLevelInstances().size()) {
                            collectionPlan.setCurrentDunningLevelSequence(collectionPlan.getDunningLevelInstances().get(nextLevel).getSequence());
                        }
                        if (levelInstance.getDunningLevel() != null
                                && levelInstance.getDunningLevel().isEndOfDunningLevel()
                                && collectionPlan.getRelatedInvoice().getPaymentStatus().equals(InvoicePaymentStatusEnum.UNPAID)
                                && nbLevelDone == collectionPlan.getDunningLevelInstances().size()) {
                            collectionPlan.setStatus(collectionPlanStatusService.findByStatus(FAILED));
                        }
                        if (collectionPlan.getRelatedInvoice().getPaymentStatus().equals(InvoicePaymentStatusEnum.PAID)) {
                            collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
                        }
                        long countActions = levelInstance.getActions().stream().filter(action -> action.getActionStatus() == DONE).count();
                        if (countActions > 0 && countActions < levelInstance.getActions().size()) {
                            levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.IN_PROGRESS);
                        }
                        if (countActions == levelInstance.getActions().size()) {
                            levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.DONE);
                        }
                    }

                    if (collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE_AND_BALANCE_THRESHOLD)
                            && collectionPlan.getRelatedInvoice().getRecordedInvoice().getUnMatchingAmount().compareTo(levelInstance.getDunningLevel().getMinBalance()) < 0
                            && levelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.TO_BE_DONE) {
                        levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.IGNORED);
                        levelInstanceService.update(levelInstance);
                        levelInstance.getActions().stream()
                                .filter(action -> action.getActionStatus() == TO_BE_DONE)
                                .forEach(action -> {
                                    action.setActionStatus(DunningActionInstanceStatusEnum.IGNORED);
                                    actionInstanceService.update(action);
                        });
                    }
                }
                if (levelInstance.getDunningLevel() == null) {
                    throw new BusinessException("No dunning level associated to level instance id " + levelInstance.getId());
                } else {
                    levelInstanceService.update(levelInstance);
                }
                index++;
            }
            if(nbLevelDone == collectionPlan.getDunningLevelInstances().size()) {
                collectionPlan.setNextActionDate(null);
                collectionPlan.setNextAction(null);
                if (collectionPlan.getRelatedInvoice().getPaymentStatus().equals(InvoicePaymentStatusEnum.UNPAID)) {
                    collectionPlan.setStatus(collectionPlanStatusService.findByStatus(FAILED));
                    updateCollectionPlan = true;
                }
                if (collectionPlan.getRelatedInvoice().getPaymentStatus().equals(InvoicePaymentStatusEnum.PAID)) {
                    collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
                    updateCollectionPlan = true;
                }
            }
        }
        if (updateCollectionPlan) {
            collectionPlanService.update(collectionPlan);
        }
    }

    private static Date getDateOverdue(DunningLevelInstance levelInstance, DunningCollectionPlan collectionPlan) {
        return DateUtils.addDaysToDate(collectionPlan.getStartDate(), ofNullable(collectionPlan.getPauseDuration()).orElse(0) + levelInstance.getDaysOverdue());
    }

    private void triggerAction(DunningActionInstance actionInstance, DunningCollectionPlan collectionPlan) {
        DunningSettings dunningSettings = dunningSettingsService.findLastOne();

        // Execute script
        if (actionInstance.getActionType().equals(SCRIPT) && actionInstance.getDunningAction() != null) {
            HashMap<String, Object> context = new HashMap<>();
            context.put(Script.CONTEXT_ENTITY, collectionPlan.getRelatedInvoice());
            context.put("customerAccount", collectionPlan.getCustomerAccount());
            if (dunningSettings != null) {
                context.put("dunningMode", dunningSettings.getDunningMode());
            }
            scriptInstanceService.execute(actionInstance.getDunningAction().getScriptInstance().getCode(), context);
        }

        // Send notification
        if (actionInstance.getActionType().equals(SEND_NOTIFICATION)
                && (actionInstance.getDunningAction().getActionChannel().equals(EMAIL)
                || actionInstance.getDunningAction().getActionChannel().equals(LETTER))) {
                sendEmail(actionInstance.getDunningAction().getActionNotificationTemplate(),
                        collectionPlan.getRelatedInvoice(), collectionPlan.getLastActionDate());
        }

        // Retry payment
        if (actionInstance.getActionType().equals(RETRY_PAYMENT)) {
        	collectionPlanService.launchPaymentAction(collectionPlan);
        }
    }

    private void sendEmail(EmailTemplate emailTemplate, Invoice invoice, Date lastActionDate) {
        if (invoice.getSeller() != null && invoice.getSeller().getContactInformation() != null
                && invoice.getSeller().getContactInformation().getEmail() != null
                && !invoice.getSeller().getContactInformation().getEmail().isBlank()) {
            Seller seller = invoice.getSeller();
            Map<Object, Object> params = new HashMap<>();
            BillingAccount billingAccount =
                    billingAccountService.findById(invoice.getBillingAccount().getId(), asList("customerAccount"));
            params.put("billingAccountDescription", billingAccount.getDescription());
            params.put("billingAccountAddressAddress1", billingAccount.getAddress() != null ?
                    billingAccount.getAddress().getAddress1() : "");
            params.put("billingAccountAddressZipCode", billingAccount.getAddress() != null ?
                    billingAccount.getAddress().getZipCode() : "");
            params.put("billingAccountAddressCity", billingAccount.getAddress() != null ?
                    billingAccount.getAddress().getCity() : "");
            params.put("billingAccountContactInformationPhone", billingAccount.getContactInformation() != null ?
                    billingAccount.getContactInformation().getPhone() : "");

            CustomerAccount customerAccount = customerAccountService.findById(billingAccount.getCustomerAccount().getId());
            if (billingAccount.getIsCompany()) {
                params.put("customerAccountLegalEntityTypeCode",
                        ofNullable(billingAccount.getLegalEntityType()).map(Title::getCode).orElse(""));
            } else {
                Name name = ofNullable(billingAccount.getName()).orElse(null);
                Title title = ofNullable(name).map(Name::getTitle).orElse(null);
                params.put("customerAccountLegalEntityTypeCode",
                        ofNullable(title).map(Title::getDescription).orElse(""));
            }
            params.put("customerAccountAddressAddress1", customerAccount.getAddress() != null ?
                    customerAccount.getAddress().getAddress1() : "");
            params.put("customerAccountAddressZipCode", customerAccount.getAddress() != null ?
                    customerAccount.getAddress().getZipCode() : "");
            params.put("customerAccountAddressCity", customerAccount.getAddress() != null ?
                    customerAccount.getAddress().getCity() : "");
            params.put("customerAccountDescription", customerAccount.getDescription());
            params.put("customerAccountLastName", customerAccount.getName() != null ?
                    customerAccount.getName().getLastName() : "");
            params.put("customerAccountFirstName", customerAccount.getName() != null ?
                    customerAccount.getName().getFirstName() : "");

            params.put("invoiceInvoiceNumber", invoice.getInvoiceNumber());
            params.put("invoiceTotal", invoice.getAmountWithTax());
            params.put("invoiceDueDate", formatter.format(invoice.getDueDate()));
            params.put("dayDate", formatter.format(new Date()));

            params.put("dunningCollectionPlanLastActionDate", lastActionDate != null ? formatter.format(lastActionDate) : "");
            List<File> attachments = new ArrayList<>();
            String invoiceFileName = invoiceService.getFullPdfFilePath(invoice, false);
            File attachment = new File(invoiceFileName);
            if (attachment.exists()) {
                attachments.add(attachment);
            } else {
                log.warn("No Pdf file exists for the invoice : {}",
                        invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getTemporaryInvoiceNumber());
            }
            if (billingAccount.getContactInformation() != null && billingAccount.getContactInformation().getEmail() != null) {
                try {
                    collectionPlanService.sendNotification(seller.getContactInformation().getEmail(),
                            billingAccount, emailTemplate, params, attachments);
                } catch (Exception exception) {
                    throw new BusinessException(exception.getMessage());
                }
            } else {
                throw new BusinessException("The email is missing for the billing account : " + billingAccount.getCode());
            }
        } else {
            throw new BusinessException("The email sending skipped because the from email is missing for the seller : " + invoice.getSeller().getCode());
        }
    }

}