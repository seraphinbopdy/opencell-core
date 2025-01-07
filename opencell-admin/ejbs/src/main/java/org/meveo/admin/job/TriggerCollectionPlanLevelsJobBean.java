package org.meveo.admin.job;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.meveo.model.billing.InvoicePaymentStatusEnum.PAID;
import static org.meveo.model.dunning.DunningActionInstanceStatusEnum.DONE;
import static org.meveo.model.dunning.DunningActionInstanceStatusEnum.TO_BE_DONE;
import static org.meveo.model.payments.ActionChannelEnum.EMAIL;
import static org.meveo.model.payments.ActionChannelEnum.LETTER;
import static org.meveo.model.payments.ActionModeEnum.AUTOMATIC;
import static org.meveo.model.payments.ActionTypeEnum.RETRY_PAYMENT;
import static org.meveo.model.payments.ActionTypeEnum.SCRIPT;
import static org.meveo.model.payments.ActionTypeEnum.SEND_NOTIFICATION;
import static org.meveo.model.payments.DunningCollectionPlanStatusEnum.ACTIVE;
import static org.meveo.model.payments.DunningCollectionPlanStatusEnum.FAILED;
import static org.meveo.model.payments.DunningCollectionPlanStatusEnum.SUCCESS;
import static org.meveo.model.payments.PaymentMethodEnum.CARD;
import static org.meveo.model.payments.PaymentMethodEnum.DIRECTDEBIT;
import static org.meveo.model.shared.DateUtils.addDaysToDate;
import static org.meveo.model.shared.DateUtils.daysBetween;

import java.io.File;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.proxy.HibernateProxy;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.CurrencyDto;
import org.meveo.api.dto.account.CustomerAccountDto;
import org.meveo.apiv2.payments.AccountOperationsDetails;
import org.meveo.apiv2.payments.ImmutableAccountOperationsDetails;
import org.meveo.apiv2.payments.ImmutableCustomerBalance;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoicePaymentStatusEnum;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.dunning.DunningActionInstance;
import org.meveo.model.dunning.DunningActionInstanceStatusEnum;
import org.meveo.model.dunning.DunningCollectionPlan;
import org.meveo.model.dunning.DunningDetermineLevelBy;
import org.meveo.model.dunning.DunningLevelInstance;
import org.meveo.model.dunning.DunningLevelInstanceStatusEnum;
import org.meveo.model.dunning.DunningModeEnum;
import org.meveo.model.dunning.DunningSettings;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.CardPaymentMethod;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.CustomerBalance;
import org.meveo.model.payments.DunningCollectionPlanStatusEnum;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.model.payments.PaymentGateway;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.shared.Name;
import org.meveo.model.shared.Title;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.CustomerBalanceService;
import org.meveo.service.payments.impl.DunningActionInstanceService;
import org.meveo.service.payments.impl.DunningCollectionPlanService;
import org.meveo.service.payments.impl.DunningCollectionPlanStatusService;
import org.meveo.service.payments.impl.DunningLevelInstanceService;
import org.meveo.service.payments.impl.DunningSettingsService;
import org.meveo.service.payments.impl.PaymentService;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

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

    @Inject
    private PaymentService paymentService;

    private final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    private static final String COLLECTION_PLAN_ID_MESSAGE = "Collection plan ID : ";
    
    @Inject
    private CustomerBalanceService customerBalanceService;

    @Inject
    private AccountOperationService accountOperationService;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        // Get the last dunning setting configuration
        DunningSettings dunningSettings = dunningSettingsService.findLastOne();
        // Get collection plans to process
        List<Long> collectionPlanToProcess = getCollectionPlansToProcess(dunningSettings);
        // Set number of items to process
        jobExecutionResult.setNbItemsToProcess(collectionPlanToProcess.size());
        // Process collection plans
        collectionPlanToProcess.forEach(collectionPlanId -> {
            // Get collection plan
            DunningCollectionPlan collectionPlan = collectionPlanService.findById(collectionPlanId);

            try {
                if(dunningSettings.getDunningMode().equals(DunningModeEnum.INVOICE_LEVEL)) {
                    jobBean.processInvoiceLevel(collectionPlan, jobExecutionResult);
                } else if(dunningSettings.getDunningMode().equals(DunningModeEnum.CUSTOMER_LEVEL)) {
                    jobBean.processCustomerLevel(collectionPlan, jobExecutionResult);
                }

                // Check all processed collection plan to verify if the invoice is paid
                getAndUpdateProcessedCollectionPlans(collectionPlan, dunningSettings);
            } catch (Exception exception) {
                jobExecutionResult.addErrorReport(exception.getMessage());
            }
        });

        // Update job execution result
        jobExecutionResult.setNbItemsCorrectlyProcessed(collectionPlanToProcess.size() - jobExecutionResult.getNbItemsProcessedWithError());
    }

    /**
     * Get collection plans to process
     *
     * @param dunningSettings Dunning settings
     * @return Collection plans to process
     */
    private List<Long> getCollectionPlansToProcess(DunningSettings dunningSettings) {
        List<Long> activeInvoiceLevelCollectionPlansIds = collectionPlanService.getActiveInvoiceLevelCollectionPlansIds();
        List<Long> activeCustomerLevelCollectionPlansIds = collectionPlanService.getActiveCustomerLevelCollectionPlansIds();
        List<Long> collectionPlanToProcess = new ArrayList<>();

        if(dunningSettings.getDunningMode().equals(DunningModeEnum.INVOICE_LEVEL)) {
            collectionPlanToProcess = activeInvoiceLevelCollectionPlansIds;
            stopCollectionPlan(activeCustomerLevelCollectionPlansIds);
        } else if(dunningSettings.getDunningMode().equals(DunningModeEnum.CUSTOMER_LEVEL)) {
            collectionPlanToProcess = activeCustomerLevelCollectionPlansIds;
            stopCollectionPlan(activeInvoiceLevelCollectionPlansIds);
        }

        return collectionPlanToProcess;
    }

    /**
     * Get and update processed collection plans
     *
     */
    private void getAndUpdateProcessedCollectionPlans(DunningCollectionPlan collectionPlan, DunningSettings pDunningSettings) {
        checkAndUpdateCollectionPlanWhenPaidInvoice(collectionPlan, pDunningSettings);
    }

    /**
     * Check and update collection plan when invoice is paid
     *
     * @param processedCollectionPlan Processed collection plan
     */
    private void checkAndUpdateCollectionPlanWhenPaidInvoice(DunningCollectionPlan processedCollectionPlan, DunningSettings pDunningSettings) {
        if (pDunningSettings.getDunningMode().equals(DunningModeEnum.INVOICE_LEVEL)) {
            if(processedCollectionPlan.getStatus().getStatus() == ACTIVE && processedCollectionPlan.getRelatedInvoice().getPaymentStatus() == PAID) {
                finalizeCollectionPlan(processedCollectionPlan);
            }
        } else if (pDunningSettings.getDunningMode().equals(DunningModeEnum.CUSTOMER_LEVEL)) {
            CustomerBalance customerBalance = pDunningSettings.getCustomerBalance();
            CustomerAccount customerAccount = processedCollectionPlan.getCustomerAccount();
            List<String> linkedOccTemplates = getOccTemplateCodesToUse(customerBalance);
            BigDecimal balance = customerAccountService.getCustomerAccountBalance(customerAccount, linkedOccTemplates);

            if (processedCollectionPlan.getRelatedPolicy().getMinBalanceTrigger() != null &&
                    balance.compareTo(BigDecimal.valueOf(processedCollectionPlan.getRelatedPolicy().getMinBalanceTrigger())) < 0) {
                finalizeCollectionPlan(processedCollectionPlan);
            }
        }
    }

    /**
     * Process collection plans
     *
     * @param collectionPlan   Collection plan to process
     * @param jobExecutionResult Job execution result
     */
    public void processInvoiceLevel(DunningCollectionPlan collectionPlan, JobExecutionResultImpl jobExecutionResult) {
        Date dateToCompare;
        Date today = new Date();
        int index = 0;
        int nextLevel = 0;
        boolean updateCollectionPlan = false;

        // Check if collection plan has levels
        if (collectionPlan.getDunningLevelInstances() == null || collectionPlan.getDunningLevelInstances().isEmpty()) {
            throw new BusinessException(COLLECTION_PLAN_ID_MESSAGE + collectionPlan.getId() + " has no levelInstances associated");
        }

        // Check if collection plan is active and invoice is paid
        if(collectionPlan.getStatus().getStatus() == ACTIVE && collectionPlan.getRelatedInvoice().getPaymentStatus() == PAID) {
            // Ignore levels and actions after paying invoice
            collectionPlanService.ignoreLevelsAndActionsAfterStoppingDunningCollectionPlanOrPayingInvoice(collectionPlan);

            // Update collection plan status to success
            collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
            collectionPlan.setNextActionDate(null);
            collectionPlan.setNextAction(null);
            collectionPlanService.update(collectionPlan);
        } else {
            collectionPlan.getDunningLevelInstances().sort(Comparator.comparing(DunningLevelInstance::getSequence));
            int nbLevelDone = 0;
            DunningLevelInstance levelInstance= null;
            List<DunningLevelInstance> levelInstances = collectionPlan.getDunningLevelInstances();

            for (int levelsIndex = 0 ; levelsIndex < levelInstances.size(); levelsIndex++) {
                levelInstance = levelInstances.get(levelsIndex);
                dateToCompare = levelInstance.getExecutionDate();
                DunningLevelInstance previousLevelInstance = null;

                if (levelsIndex > 0) {
                    previousLevelInstance = levelInstances.get(levelsIndex - 1);
                }

                if(levelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
                    nbLevelDone++;
                }

                if (levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.TO_BE_DONE) && !collectionPlan.getRelatedInvoice().getPaymentStatus().equals(PAID)) {
                LocalDate ldDateToCompare = dateToCompare.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate ldToday = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                boolean isDateEquals =  ldDateToCompare.isBefore(ldToday) || ldDateToCompare.isEqual(ldToday);

                    if ((isDateEquals
                            && collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE_AND_BALANCE_THRESHOLD)
                            && (collectionPlan.getRelatedInvoice().getRecordedInvoice().getUnMatchingAmount().compareTo(levelInstance.getDunningLevel().getMinBalance()) > 0))
                            || (isDateEquals && collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE))) {
                    nextLevel = index + 1;
                    boolean registerKO = false;
                    for (int i = 0; i < levelInstance.getActions().size(); i++) {
                        DunningActionInstance actionInstance = levelInstance.getActions().get(i);
                            if (actionInstance.getActionMode().equals(AUTOMATIC) && actionInstance.getActionStatus().equals(TO_BE_DONE)) {
                            try {
                                    if (previousLevelInstance == null || (previousLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE || previousLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.IGNORED)) {
                                        triggerActionAndRefreshLevelsAndActions(collectionPlan, levelInstance, actionInstance);
                                    }
                            } catch (Exception exception) {
                                registerKO = true;
                                    jobExecutionResult.addReport(COLLECTION_PLAN_ID_MESSAGE
                                        + collectionPlan.getId() + "/Level instance ID : "
                                        + levelInstance.getId() + "/Action instance ID : " + actionInstance.getId()
                                        + " : " + exception.getMessage());
                            }
                        }
                        actionInstanceService.update(actionInstance);
                    }
                    if (!registerKO) {
                        if (levelsIndex + 1 < levelInstances.size()) {
                            for (int i = levelsIndex + 1; i < levelInstances.size(); i++) {
                                    if(levelInstances.get(i).getActions() != null && !levelInstances.get(i).getActions().isEmpty()) {
                                        collectionPlan.setNextAction(levelInstances.get(i).getActions().get(0).getDunningAction().getCode());
                                        collectionPlan.setNextActionDate(addDaysToDate(collectionPlan.getStartDate(), levelInstances.get(i).getDaysOverdue()));
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

                        // Set current dunning level sequence
                    if (nextLevel < collectionPlan.getDunningLevelInstances().size()) {
                        collectionPlan.setCurrentDunningLevelSequence(collectionPlan.getDunningLevelInstances().get(nextLevel).getSequence());
                    }

                        // Update collection plan status to FAILED if is end of dunning and invoice is unpaid
                    if (levelInstance.getDunningLevel() != null && levelInstance.getDunningLevel().isEndOfDunningLevel() && collectionPlan.getRelatedInvoice().getPaymentStatus().equals(InvoicePaymentStatusEnum.UNPAID)) {
                        collectionPlan.setStatus(collectionPlanStatusService.findByStatus(FAILED));
                        collectionPlan.setNextAction(null);
                        collectionPlan.setNextActionDate(null);
                    }

                        // Update collection plan status to SUCCESS if is end of dunning and invoice is paid
                    if (collectionPlan.getRelatedInvoice().getPaymentStatus().equals(InvoicePaymentStatusEnum.PAID)) {
                        collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
                            collectionPlan.setNextAction(null);
                            collectionPlan.setNextActionDate(null);
                    }
                        setLevelAndActionStatus(levelInstance);
                    }

                    if (collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE_AND_BALANCE_THRESHOLD)
                            && collectionPlan.getRelatedInvoice().getRecordedInvoice().getUnMatchingAmount().compareTo(levelInstance.getDunningLevel().getMinBalance()) < 0
                            && levelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.TO_BE_DONE) {
                        ignoreLevelsAndActions(levelInstance);
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

        // Update collection plan after finishing treatment
        updateCollectionPlan = updateCollectionPlan(collectionPlan, updateCollectionPlan);

        if (updateCollectionPlan) {
            collectionPlanService.update(collectionPlan);
        }
    }

    /**
     * Process collection plans
     *
     * @param collectionPlan   Collection plan to process
     * @param jobExecutionResult Job execution result
     */
    public void processCustomerLevel(DunningCollectionPlan collectionPlan, JobExecutionResultImpl jobExecutionResult) {
        DunningSettings dunningSettings = dunningSettingsService.findLastOne();
        Date dateToCompare;
        Date today = new Date();
        int index = 0;
        int nextLevel = 0;
        boolean updateCollectionPlan = false;

        // Check if collection plan has levels
        if (collectionPlan.getDunningLevelInstances() == null || collectionPlan.getDunningLevelInstances().isEmpty()) {
            throw new BusinessException(COLLECTION_PLAN_ID_MESSAGE + collectionPlan.getId() + " has no levelInstances associated");
        }

        CustomerBalance customerBalance = dunningSettings.getCustomerBalance();
        List<String> linkedOccTemplates = getOccTemplateCodesToUse(customerBalance);
        BigDecimal balance = customerAccountService.getCustomerAccountBalance(collectionPlan.getCustomerAccount(), linkedOccTemplates);

        // Check if collection plan is active and invoice is paid
        if(collectionPlan.getStatus().getStatus() == ACTIVE && balance.compareTo(BigDecimal.valueOf(collectionPlan.getRelatedPolicy().getMinBalanceTrigger())) < 0) {
            finalizeCollectionPlan(collectionPlan);
        } else {
            collectionPlan.getDunningLevelInstances().sort(Comparator.comparing(DunningLevelInstance::getSequence));
            int nbLevelDone = 0;
            DunningLevelInstance levelInstance= null;
            List<DunningLevelInstance> levelInstances = collectionPlan.getDunningLevelInstances();

            for (int levelsIndex = 0 ; levelsIndex < levelInstances.size(); levelsIndex++) {
                levelInstance = levelInstances.get(levelsIndex);
                dateToCompare = levelInstance.getExecutionDate();
                DunningLevelInstance previousLevelInstance = null;

                if (levelsIndex > 0) {
                    previousLevelInstance = levelInstances.get(levelsIndex - 1);
                }

                if(levelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
                    nbLevelDone++;
                }

                if (levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.TO_BE_DONE) && balance.compareTo(BigDecimal.valueOf(collectionPlan.getRelatedPolicy().getMinBalanceTrigger())) > 0) {
                LocalDate ldDateToCompare = dateToCompare.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate ldToday = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                boolean isDateEquals =  ldDateToCompare.isBefore(ldToday) || ldDateToCompare.isEqual(ldToday);

                    if ((isDateEquals
                            && collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE_AND_BALANCE_THRESHOLD)
                            && (levelInstance.getDunningLevel().getMinBalance() == null || balance.compareTo(levelInstance.getDunningLevel().getMinBalance()) > 0))
                            || (isDateEquals && collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE))) {
                        nextLevel = index + 1;
                        boolean registerKO = false;
                        for (int i = 0; i < levelInstance.getActions().size(); i++) {
                            DunningActionInstance actionInstance = levelInstance.getActions().get(i);
                            if (actionInstance.getActionMode().equals(AUTOMATIC) && actionInstance.getActionStatus().equals(TO_BE_DONE)) {
                                try {
                                    if (previousLevelInstance == null || (previousLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE || previousLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.IGNORED)) {
                                        triggerActionAndRefreshLevelsAndActions(collectionPlan, levelInstance, actionInstance);
                                    }
                                } catch (Exception exception) {
                                    registerKO = true;
                                    jobExecutionResult.addReport(COLLECTION_PLAN_ID_MESSAGE
                                            + collectionPlan.getId() + "/Level instance ID : "
                                            + levelInstance.getId() + "/Action instance ID : " + actionInstance.getId()
                                            + " : " + exception.getMessage());
                                }
                            }
                            actionInstanceService.update(actionInstance);
                        }
                        if (!registerKO) {
                            if (levelsIndex + 1 < levelInstances.size()) {
                                for (int i = levelsIndex + 1; i < levelInstances.size(); i++) {
                                    if(levelInstances.get(i).getActions() != null && !levelInstances.get(i).getActions().isEmpty()) {
                                        collectionPlan.setNextAction(levelInstances.get(i).getActions().get(0).getDunningAction().getCode());
                                        collectionPlan.setNextActionDate(addDaysToDate(collectionPlan.getStartDate(), levelInstances.get(i).getDaysOverdue()));
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

                        // Set current dunning level sequence
                        if (nextLevel < collectionPlan.getDunningLevelInstances().size()) {
                            collectionPlan.setCurrentDunningLevelSequence(collectionPlan.getDunningLevelInstances().get(nextLevel).getSequence());
                        }

                        // Update collection plan status to FAILED if is end of dunning and invoice is unpaid
                        if (levelInstance.getDunningLevel() != null && levelInstance.getDunningLevel().isEndOfDunningLevel() && (levelInstance.getDunningLevel().getMinBalance() == null || balance.compareTo(levelInstance.getDunningLevel().getMinBalance()) > 0)) {
                            collectionPlan.setStatus(collectionPlanStatusService.findByStatus(FAILED));
                            collectionPlan.setNextAction(null);
                            collectionPlan.setNextActionDate(null);
                        }

                        // Update collection plan status to SUCCESS if is end of dunning and invoice is paid
                        if (levelInstance.getDunningLevel() != null && levelInstance.getDunningLevel().isEndOfDunningLevel() && (levelInstance.getDunningLevel().getMinBalance() == null || balance.compareTo(levelInstance.getDunningLevel().getMinBalance()) < 0)) {
                            collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
                            collectionPlan.setNextAction(null);
                            collectionPlan.setNextActionDate(null);
                        }

                        // check if all invoices are paid
                        List<Invoice> relatedInvoices = collectionPlan.getRelatedInvoices();
                        boolean allPaid = true;
                        for (Invoice invoice : relatedInvoices) {
                            if (!invoice.getPaymentStatus().equals(PAID)) {
                                allPaid = false;
                            }
                        }

                        if(collectionPlan.getBalance().equals(0) && allPaid) {
                            collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
                            collectionPlan.setNextAction(null);
                            collectionPlan.setNextActionDate(null);
                        }

                        setLevelAndActionStatus(levelInstance);
                        }

                    if (collectionPlan.getRelatedPolicy().getDetermineLevelBy().equals(DunningDetermineLevelBy.DAYS_OVERDUE_AND_BALANCE_THRESHOLD)
                            && (levelInstance.getDunningLevel().getMinBalance() == null || balance.compareTo(levelInstance.getDunningLevel().getMinBalance()) < 0)
                            && levelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.TO_BE_DONE) {
                        ignoreLevelsAndActions(levelInstance);
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
                if (balance.compareTo(BigDecimal.valueOf(collectionPlan.getRelatedPolicy().getMinBalanceTrigger())) > 0) {
                    collectionPlan.setStatus(collectionPlanStatusService.findByStatus(FAILED));
                    updateCollectionPlan = true;
                }
                if (balance.compareTo(BigDecimal.valueOf(collectionPlan.getRelatedPolicy().getMinBalanceTrigger())) < 0) {
                    collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
                    updateCollectionPlan = true;
                }
            }
        }

        updateCollectionPlan(collectionPlan, updateCollectionPlan);

        if (updateCollectionPlan) {
            collectionPlanService.update(collectionPlan);
        }
    }

    /**
     * Stop collection plans
     *
     * @param collectionPlanIds Collection plan ids
     */
    private void stopCollectionPlan(List<Long> collectionPlanIds) {
        collectionPlanIds.forEach(collectionPlanId -> {
            DunningCollectionPlan collectionPlan = collectionPlanService.findById(collectionPlanId);
            if(collectionPlan.getStatus().getStatus() == ACTIVE || collectionPlan.getStatus().getStatus() == DunningCollectionPlanStatusEnum.PAUSED) {
                collectionPlan.setStatus(collectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.STOPPED));
                collectionPlanService.update(collectionPlan);
            }
        });
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
     * Finalize collection plan
     *
     * @param collectionPlan Collection plan
     */
    private void finalizeCollectionPlan(DunningCollectionPlan collectionPlan) {
        // Ignore levels and actions after balance is less than min balance trigger
        collectionPlanService.ignoreLevelsAndActionsAfterStoppingDunningCollectionPlanOrPayingInvoice(collectionPlan);
        // Update collection plan status to success
        collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
        collectionPlan.setNextActionDate(null);
        collectionPlan.setNextAction(null);
        collectionPlan.setCloseDate(new Date());
        collectionPlan.setDaysOpen((int) daysBetween(collectionPlan.getCloseDate(), new Date()) + 1);
        collectionPlanService.update(collectionPlan);
    }

    /**
     * Ignore levels and actions
     *
     * @param levelInstance Level instance
     */
    private void ignoreLevelsAndActions(DunningLevelInstance levelInstance) {
        levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.IGNORED);
        levelInstance.setExecutionDate(null);
        levelInstanceService.update(levelInstance);
        levelInstance.getActions().stream()
                .filter(action -> action.getActionStatus() == TO_BE_DONE)
                .forEach(action -> {
                    action.setActionStatus(DunningActionInstanceStatusEnum.IGNORED);
                    action.setExecutionDate(null);
                    actionInstanceService.update(action);
                });
    }

    /**
     * Update collection plan
     *
     * @param collectionPlan Collection plan
     * @param updateCollectionPlan Update collection plan
     * @return Update collection plan
     */
    private boolean updateCollectionPlan(DunningCollectionPlan collectionPlan, boolean updateCollectionPlan) {
        if(collectionPlan.getDunningLevelInstances().get(collectionPlan.getDunningLevelInstances().size() - 1).getLevelStatus().equals(DunningLevelInstanceStatusEnum.IGNORED)) {
            collectionPlan.setStatus(collectionPlanStatusService.findByStatus(SUCCESS));
            collectionPlan.setNextAction(null);
            collectionPlan.setNextActionDate(null);
            updateCollectionPlan = true;
        }

        return updateCollectionPlan;
    }

    /**
     * Set level and action status
     *
     * @param levelInstance Level instance
     */
    private void setLevelAndActionStatus(DunningLevelInstance levelInstance) {
        long countActions = levelInstance.getActions().stream().filter(action -> action.getActionStatus() == DONE).count();
        if (countActions > 0 && countActions < levelInstance.getActions().size()) {
            levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.IN_PROGRESS);
            levelInstance.setExecutionDate(new Date());
        }
        if (countActions == levelInstance.getActions().size()) {
            levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.DONE);
            levelInstance.setExecutionDate(new Date());
        }
    }

    /**
     * Trigger action and refresh levels and actions
     *
     * @param collectionPlan Collection plan
     * @param levelInstance Level instance
     * @param actionInstance Action instance
     */
    private void triggerActionAndRefreshLevelsAndActions(DunningCollectionPlan collectionPlan, DunningLevelInstance levelInstance, DunningActionInstance actionInstance) {
        actionInstanceService.triggerAction(actionInstance, collectionPlan);
        collectionPlan = collectionPlanService.refreshOrRetrieve(collectionPlan);
        actionInstance.setActionStatus(DunningActionInstanceStatusEnum.DONE);
        actionInstance.setExecutionDate(new Date());
        if (levelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.TO_BE_DONE) {
            levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.IN_PROGRESS);
            levelInstance.setExecutionDate(new Date());
            levelInstanceService.update(levelInstance);
        }
        collectionPlan.setLastActionDate(new Date());
        collectionPlan.setLastAction(actionInstance.getDunningAction().getCode());
    }
}