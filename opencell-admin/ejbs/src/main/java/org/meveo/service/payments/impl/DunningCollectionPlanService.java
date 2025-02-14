package org.meveo.service.payments.impl;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static org.meveo.model.dunning.DunningLevelInstanceStatusEnum.DONE;
import static org.meveo.model.dunning.DunningLevelInstanceStatusEnum.TO_BE_DONE;
import static org.meveo.model.shared.DateUtils.addDaysToDate;
import static org.meveo.model.shared.DateUtils.daysBetween;
import static org.meveo.service.base.ValueExpressionWrapper.evaluateExpression;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.proxy.HibernateProxy;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.payment.AccountOperationDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoicePaymentStatusEnum;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.dunning.DunningActionInstanceStatusEnum;
import org.meveo.model.dunning.DunningCollectionPlan;
import org.meveo.model.dunning.DunningCollectionPlanStatus;
import org.meveo.model.dunning.DunningLevel;
import org.meveo.model.dunning.DunningLevelInstance;
import org.meveo.model.dunning.DunningLevelInstanceStatusEnum;
import org.meveo.model.dunning.DunningPauseReason;
import org.meveo.model.dunning.DunningPolicy;
import org.meveo.model.dunning.DunningPolicyLevel;
import org.meveo.model.dunning.DunningStopReason;
import org.meveo.model.payments.*;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.audit.logging.AuditLogService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.communication.impl.EmailSender;
import org.meveo.service.communication.impl.EmailTemplateService;
import org.meveo.service.communication.impl.InternationalSettingsService;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

@Stateless
public class DunningCollectionPlanService extends PersistenceService<DunningCollectionPlan> {
    
    @Inject
    private DunningStopReasonsService dunningStopReasonsService;

    @Inject
    private DunningLevelInstanceService levelInstanceService;

    @Inject
    private DunningCollectionPlanStatusService dunningCollectionPlanStatusService;

    @Inject
    private DunningCollectionPlanService dunningCollectionPlanService;

    @Inject
    private InvoiceService invoiceService;

    @Inject
    private DunningPolicyService policyService;

    @Inject
    private EmailSender emailSender;

    @Inject
    private EmailTemplateService emailTemplateService;

    @Inject
    private InternationalSettingsService internationalSettingsService;

    @Inject
    private PaymentService paymentService;

    @Inject
    private PaymentGatewayService paymentGatewayService;

    @Inject
    private DunningLevelInstanceService dunningLevelInstanceService;

    @Inject
    private BillingAccountService billingAccountService;

    @Inject
    private CustomerAccountService customerAccountService;

    @Inject
    private AuditLogService auditLogService;

    @Inject
    private AccountOperationService accountOperationService;

    @Inject
    private CustomerBalanceService customerBalanceService;

    private static final String STOP_REASON = "STOP_POLITIQUE";

    public DunningCollectionPlan findByPolicy(DunningPolicy dunningPolicy) {
        List<DunningCollectionPlan> result = getEntityManager()
                                                .createNamedQuery("DunningCollectionPlan.findByPolicy", entityClass)
                                                .setParameter("dunningPolicy", dunningPolicy)
                                                .getResultList();
        return result != null && !result.isEmpty() ? result.get(0) : null;
    }

    /**
     * Switch collection plan
     * @param oldCollectionPlan Old collection plan
     * @param policy Dunning policy
     * @param selectedPolicyLevel Selected policy level
     * @return New collection plan
     */
    public DunningCollectionPlan switchCollectionPlan(DunningCollectionPlan oldCollectionPlan, DunningPolicy policy, DunningPolicyLevel selectedPolicyLevel) {
        DunningStopReason stopReason = dunningStopReasonsService.findByStopReason(STOP_REASON);
        policy = policyService.refreshOrRetrieve(policy);
        DunningCollectionPlanStatus collectionPlanStatusStop = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.STOPPED);

        // Update the old collection plan after switching
        updateOldDunningCollectionPlanAfterSwitchAction(oldCollectionPlan, stopReason, collectionPlanStatusStop);

        // Create a new collection plan after switching
        DunningCollectionPlanStatus collectionPlanStatusActif = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.ACTIVE);
        DunningCollectionPlan newCollectionPlan = createNewDunningCollectionPlanAfterSwitchAction(policy, selectedPolicyLevel, oldCollectionPlan, collectionPlanStatusActif);
       
        if (policy.getDunningLevels() != null && !policy.getDunningLevels().isEmpty()) {
            List<DunningLevelInstance> levelInstances = new ArrayList<>();
            for (DunningPolicyLevel policyLevel : policy.getDunningLevels()) {
                DunningLevelInstance levelInstance;
                if (policyLevel.getSequence() < selectedPolicyLevel.getSequence()) {
                    levelInstance = dunningLevelInstanceService.createDunningLevelInstanceWithCollectionPlan(newCollectionPlan, collectionPlanStatusActif, policyLevel, DONE);
                } else {
                    levelInstance = dunningLevelInstanceService.createDunningLevelInstanceWithCollectionPlan(newCollectionPlan, collectionPlanStatusActif, policyLevel, TO_BE_DONE);
                    if (policyLevel.getSequence().equals(selectedPolicyLevel.getSequence())) {
                        DunningLevel nextLevel = findLevelBySequence(policy.getDunningLevels(), policyLevel.getSequence());
                        if(nextLevel != null
                                && nextLevel.getDunningActions() != null && !nextLevel.getDunningActions().isEmpty()) {
                            int dOverDue = Optional.ofNullable(nextLevel.getDaysOverdue()).orElse(0);
                            int i = 0;
                            while(i < nextLevel.getDunningActions().size() - 1) {
                            	if(nextLevel.getDunningActions().get(i).getActionMode().equals(ActionModeEnum.AUTOMATIC)) {
                            		break;
                            	}else {
                            		i++;
                            	}
                            }
                            newCollectionPlan.setNextAction((nextLevel.getDunningActions().get(i).getActionMode().equals(ActionModeEnum.AUTOMATIC))
                            		? nextLevel.getDunningActions().get(i).getCode()
                            				: nextLevel.getDunningActions().get(0).getCode());
                            newCollectionPlan.setNextActionDate(addDaysToDate(newCollectionPlan.getStartDate(), dOverDue));
                        }
                    }
                }
                levelInstances.add(levelInstance);
            }
            newCollectionPlan.setDunningLevelInstances(levelInstances);
            update(newCollectionPlan);
        }        
        
        newCollectionPlan.setCollectionPlanNumber("C" + newCollectionPlan.getId());
        update(newCollectionPlan);
        update(oldCollectionPlan);
        return newCollectionPlan;
    }

    /**
     * Update the old collection plan after switch action
     * @param oldCollectionPlan Old collection plan
     * @param stopReason Dunning stop reason
     * @param collectionPlanStatusStop Collection plan status object
     */
    private void updateOldDunningCollectionPlanAfterSwitchAction(DunningCollectionPlan oldCollectionPlan, DunningStopReason stopReason, DunningCollectionPlanStatus collectionPlanStatusStop) {
        // Ignore levels and actions after paying invoice
        ignoreLevelsAndActionsAfterStoppingDunningCollectionPlanOrPayingInvoice(oldCollectionPlan);

        // Update the old collection plan with the stop reason and status
        oldCollectionPlan.setStopReason(stopReason);
        oldCollectionPlan.setCloseDate(new Date());
        oldCollectionPlan.setStatus(collectionPlanStatusStop);
    }

    /**
     * Create a new collection plan after switch action
     * @param policy Dunning policy
     * @param selectedPolicyLevel Selected policy level
     * @param oldCollectionPlan Old collection plan
     * @param collectionPlanStatusActif Collection plan status object
     * @return New collection plan
     */
    private DunningCollectionPlan createNewDunningCollectionPlanAfterSwitchAction(DunningPolicy policy, DunningPolicyLevel selectedPolicyLevel, DunningCollectionPlan oldCollectionPlan, DunningCollectionPlanStatus collectionPlanStatusActif) {
        DunningCollectionPlan newCollectionPlan = new DunningCollectionPlan();
        newCollectionPlan.setRelatedPolicy(policy);
        newCollectionPlan.setBillingAccount(oldCollectionPlan.getBillingAccount());
        newCollectionPlan.setRelatedInvoice(oldCollectionPlan.getRelatedInvoice());
        newCollectionPlan.setCurrentDunningLevelSequence(selectedPolicyLevel.getSequence());
        newCollectionPlan.setTotalDunningLevels(policy.getTotalDunningLevels());
        newCollectionPlan.setStartDate(oldCollectionPlan.getStartDate());
        newCollectionPlan.setStatus(collectionPlanStatusActif);
        newCollectionPlan.setBalance(oldCollectionPlan.getBalance());
        newCollectionPlan.setInitialCollectionPlan(oldCollectionPlan);
        newCollectionPlan.setLastAction(oldCollectionPlan.getLastAction());
        newCollectionPlan.setLastActionDate(oldCollectionPlan.getLastActionDate());
        newCollectionPlan.setDaysOpen(abs((int) daysBetween(new Date(), newCollectionPlan.getStartDate())) + 1);
        newCollectionPlan.setCustomerAccount(oldCollectionPlan.getCustomerAccount());
        create(newCollectionPlan);
        return newCollectionPlan;
    }

    public List<DunningCollectionPlan> findByInvoiceId(long invoiceID) {
        return getEntityManager()
                    .createNamedQuery("DunningCollectionPlan.findByInvoiceId", entityClass)
                    .setParameter("invoiceID", invoiceID)
                    .getResultList();
    }

    /**
     * Create a collection plan from invoice and dunning policy
     * @param invoice : invoice
     * @param policy : dunningPolicy
     * @param collectionPlanStatus collection plan status object
     */
    public void createCollectionPlanFrom(Invoice invoice, DunningPolicy policy, DunningCollectionPlanStatus collectionPlanStatus) {
        invoice = invoiceService.refreshOrRetrieve(invoice);
        DunningCollectionPlan collectionPlan = new DunningCollectionPlan();
        collectionPlan.setRelatedPolicy(policy);
        collectionPlan.setBillingAccount(invoice.getBillingAccount());
        collectionPlan.setRelatedInvoice(invoice);
        collectionPlan.setCurrentDunningLevelSequence(0);
        collectionPlan.setTotalDunningLevels(policy.getTotalDunningLevels());
        collectionPlan.setStartDate(new Date());
        collectionPlan.setStatus(collectionPlanStatus);
        collectionPlan.setDaysOpen(abs((int) daysBetween(new Date(), collectionPlan.getStartDate())) + 1);
        Optional.ofNullable(invoice.getRecordedInvoice())
                                .ifPresent(recordedInvoice -> collectionPlan.setBalance(recordedInvoice.getTransactionalUnMatchingAmount()));

        // Create the collection plan
        create(collectionPlan);

        // Update the invoice with the collection plan
        invoice.setRelatedDunningCollectionPlan(collectionPlan);
        invoice.setDunningCollectionPlanTriggered(true);
        invoiceService.update(invoice);

        collectionPlan.setCollectionPlanNumber("C" + collectionPlan.getId());

        // Check and update dunning level instance attached to the invoice
        updateDunningLevelInstance(invoice, collectionPlan, collectionPlanStatus);

        // Create the dunning level instances
        if(policy.getDunningLevels() != null && !policy.getDunningLevels().isEmpty()) {
            // Get billing account and customer account from invoice
            BillingAccount billingAccount = billingAccountService.findById(invoice.getBillingAccount().getId(), List.of("customerAccount"));
            CustomerAccount customerAccount = customerAccountService.findById(billingAccount.getCustomerAccount().getId());

            // Create dunning level instances
            List<DunningLevelInstance> dunningLevelInstances = dunningLevelInstanceService.createDunningLevelInstancesWithCollectionPlan(customerAccount, invoice, policy, collectionPlan, collectionPlanStatus);
            collectionPlan.setDunningLevelInstances(dunningLevelInstances);

            // Update the collection plan with the first dunning level instance
            Optional<DunningLevelInstance> firstDunningLevelInstance = dunningLevelInstances.stream()
                    .filter(dunningLevelInstance -> dunningLevelInstance.getSequence() == 0)
                    .findFirst();

            // Update the collection plan with the next action and next action date
            Optional<DunningLevelInstance> nextDunningLevelInstance = dunningLevelInstances.stream()
                    .filter(dunningLevelInstance -> dunningLevelInstance.getSequence() == 1)
                    .filter(dunningLevelInstance -> dunningLevelInstance.getLevelStatus().equals(TO_BE_DONE))
                    .findFirst();

            // Update the collection plan with the first action and next action date
            updateCollectionPlanWhenFirstLevelIsDoneOrIgnored(collectionPlan, firstDunningLevelInstance, nextDunningLevelInstance);
    }

        auditLogService.trackOperation("CREATE DunningCollectionPlan", new Date(), collectionPlan, collectionPlan.getCollectionPlanNumber());
        update(collectionPlan);
    }

    /**
     * Update collection plan when first level is done
     * @param collectionPlan DunningCollectionPlan
     * @param firstDunningLevelInstance First DunningLevelInstance
     * @param nextDunningLevelInstance Next DunningLevelInstance
     */
    public void updateCollectionPlanWhenFirstLevelIsDoneOrIgnored(DunningCollectionPlan collectionPlan,
                                                         Optional<DunningLevelInstance> firstDunningLevelInstance,
                                                         Optional<DunningLevelInstance> nextDunningLevelInstance) {
        if(firstDunningLevelInstance.isPresent()) {
            if (firstDunningLevelInstance.get().getLevelStatus().equals(DONE)) {
                collectionPlan.setLastActionDate(addDaysToDate(collectionPlan.getStartDate(), firstDunningLevelInstance.get().getDaysOverdue()));
                collectionPlan.setLastAction(firstDunningLevelInstance.get().getActions().get(0).getCode());
                collectionPlan.setCurrentDunningLevelSequence(collectionPlan.getCurrentDunningLevelSequence() + 1);
            } else if (firstDunningLevelInstance.get().getLevelStatus()==DunningLevelInstanceStatusEnum.IGNORED) {
                collectionPlan.setLastActionDate(null);
                collectionPlan.setLastAction(null);
                collectionPlan.setCurrentDunningLevelSequence(collectionPlan.getCurrentDunningLevelSequence() + 1);
            }

            if (nextDunningLevelInstance.isPresent()) {
                collectionPlan.setNextAction(nextDunningLevelInstance.get().getActions().get(0).getDunningAction().getCode());
                collectionPlan.setNextActionDate(addDaysToDate(collectionPlan.getStartDate(), nextDunningLevelInstance.get().getDaysOverdue()));
            }
        }
    }

    /**
     * Update dunning level instance
     * @param pInvoice Invoice
     * @param pCollectionPlan DunningCollectionPlan
     */
    private void updateDunningLevelInstance(Invoice pInvoice, DunningCollectionPlan pCollectionPlan, DunningCollectionPlanStatus pDunningCollectionPlanStatus) {
        List<DunningLevelInstance> dunningLevelInstances = dunningLevelInstanceService.findByInvoiceAndEmptyCollectionPlan(pInvoice);
        if (dunningLevelInstances != null && !dunningLevelInstances.isEmpty()) {
            dunningLevelInstances.forEach(dunningLevelInstance -> {
                dunningLevelInstance.setCollectionPlan(pCollectionPlan);
                dunningLevelInstance.setCollectionPlanStatus(pDunningCollectionPlanStatus);
                dunningLevelInstanceService.update(dunningLevelInstance);
            });
        }
    }

    /**
     * Find dunning level by sequence
     * @param policyLevels List of DunningPolicyLevel
     * @param sequence Sequence
     * @return DunningLevel
     */
    private DunningLevel findLevelBySequence(List<DunningPolicyLevel> policyLevels, int sequence) {
        return policyLevels.stream()
                        .filter(policyLevel -> policyLevel.getSequence() == sequence)
                        .map(DunningPolicyLevel::getDunningLevel)
                        .findFirst()
                        .orElse(null);
    }

    public DunningCollectionPlan pauseCollectionPlan(boolean forcePause, Date pauseUntil, DunningCollectionPlan collectionPlanToPause,
                                                     DunningPauseReason dunningPauseReason, boolean retryPaymentOnResumeDate) {

    	collectionPlanToPause = dunningCollectionPlanService.refreshOrRetrieve(collectionPlanToPause);
		collectionPlanToPause = refreshLevelInstances(collectionPlanToPause);
		DunningCollectionPlanStatus dunningCollectionPlanStatus = dunningCollectionPlanStatusService.refreshOrRetrieve(collectionPlanToPause.getStatus());

        // Check rules before pausing the collection plan
		checkPauseCollectionPlanRules(collectionPlanToPause, dunningCollectionPlanStatus, pauseUntil);

		if(!forcePause) {
			Optional<DunningLevelInstance> dunningLevelInstance = collectionPlanToPause.getDunningLevelInstances()
					.stream().max(Comparator.comparing(DunningLevelInstance::getId));
            LocalDate pauseDate = pauseUntil.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            LocalDate endDate = DateUtils.addDaysToDate(collectionPlanToPause.getStartDate(), dunningLevelInstance.get().getDaysOverdue())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
			if(dunningLevelInstance.isPresent() && pauseUntil != null && pauseDate.isAfter(endDate)) {
                throw new BusinessApiException("Collection Plan cannot be paused, the pause until date is after the planned date for the last dunning level");
			}
		}
		
		DunningCollectionPlanStatus collectionPlanStatus = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.PAUSED);
		collectionPlanToPause.setStatus(collectionPlanStatus);
		collectionPlanToPause.setPausedUntilDate(pauseUntil);
		collectionPlanToPause.setPauseReason(dunningPauseReason);
		collectionPlanToPause.setRetryPaymentOnResumeDate(retryPaymentOnResumeDate);
		collectionPlanToPause.addPauseDuration((int) daysBetween(new Date(),collectionPlanToPause.getPausedUntilDate()));
        collectionPlanToPause.setNextActionDate(addDaysToDate(collectionPlanToPause.getNextActionDate(), collectionPlanToPause.getPauseDuration()));
		update(collectionPlanToPause);

        updateAllDunningLevelInstancesDate(collectionPlanToPause, collectionPlanToPause.getPauseDuration());

		return collectionPlanToPause; 
	}

    /**
     * Check pause collection plan rules
     *
     * @param collectionPlanToPause Collection plan to pause
     * @param dunningCollectionPlanStatus Dunning collection plan status
     * @param pauseUntil Pause until date
     */
    private void checkPauseCollectionPlanRules(DunningCollectionPlan collectionPlanToPause, DunningCollectionPlanStatus dunningCollectionPlanStatus, Date pauseUntil) {
        if(pauseUntil != null && pauseUntil.before(new Date())) {
            throw new BusinessApiException("Collection Plan cannot be paused, the pause until date is in the past");
        }

        if(!dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.ACTIVE)) {
            throw new BusinessApiException("Collection Plan with id " + collectionPlanToPause.getId() + " cannot be paused, the collection plan status is not active");
        }

        if(dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.STOPPED)) {
            throw new BusinessApiException("Collection Plan with id " + collectionPlanToPause.getId() + " cannot be paused, the collection plan is already stopped");
        }
    }

    private void updateAllDunningLevelInstancesDate(DunningCollectionPlan collectionPlanToPause, Integer pauseDuration) {
        // Get the dunning level instance to be done and shift the execution date
        collectionPlanToPause.getDunningLevelInstances().forEach(levelInstance -> {
            if (DunningLevelInstanceStatusEnum.TO_BE_DONE == levelInstance.getLevelStatus()) {
                levelInstance.setExecutionDate(addDaysToDate(levelInstance.getExecutionDate(), pauseDuration));
                levelInstance.getActions().forEach(actionInstance -> {
                    actionInstance.setExecutionDate(addDaysToDate(actionInstance.getExecutionDate(), pauseDuration));
                });
            }
        });
    }

    /**
     * Stop a collection plan
     * @param collectionPlanToStop Collection plan to stop
     * @param dunningStopReason Dunning stop reason
     * @return {@link DunningCollectionPlan}
     */
	public DunningCollectionPlan stopCollectionPlan(DunningCollectionPlan collectionPlanToStop, DunningStopReason dunningStopReason) {
        // Refresh or retrieve the collection plan to stop, get the dunning collection plan status
		collectionPlanToStop = dunningCollectionPlanService.refreshOrRetrieve(collectionPlanToStop);
		DunningCollectionPlanStatus dunningCollectionPlanStatus = dunningCollectionPlanStatusService.refreshOrRetrieve(collectionPlanToStop.getStatus());

        // Check if the collection plan status is not success or failed
		if(dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.SUCCESS)) {
            throw new BusinessApiException("Collection Plan with id "+collectionPlanToStop.getId()+" cannot be stopped, the collection plan status is success");
		}

		if(dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.FAILED)) {
            throw new BusinessApiException("Collection Plan with id "+collectionPlanToStop.getId()+" cannot be stopped, the collection plan status is failed");
		}
		
        // Ignore levels and actions after stopping dunning collection plan
        ignoreLevelsAndActionsAfterStoppingDunningCollectionPlanOrPayingInvoice(collectionPlanToStop);

        // Update the dunning collection plan infos after stop action
        updateDunningCollectionPlanInfosAfterStopAction(collectionPlanToStop, dunningStopReason);

        // Update the collection plan
        update(collectionPlanToStop);
		return collectionPlanToStop; 
	}

    /**
     * Ignore levels and actions after stopping dunning collection plan
     * @param collectionPlanToStop Collection plan to stop
     */
    public void ignoreLevelsAndActionsAfterStoppingDunningCollectionPlanOrPayingInvoice(DunningCollectionPlan collectionPlanToStop) {
        // Get the dunning level instance to update status if is to be done
        List<DunningLevelInstance> dunningLevelInstances = new ArrayList<>();

        // Update the dunning level instance status to ignored
        collectionPlanToStop.getDunningLevelInstances().forEach(levelInstance -> {
            if (DunningLevelInstanceStatusEnum.DONE != levelInstance.getLevelStatus() && DunningLevelInstanceStatusEnum.IGNORED != levelInstance.getLevelStatus()) {
                levelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.IGNORED);
                levelInstance.setExecutionDate(null); // Set execution date to null when level is ignored
                levelInstance.getActions().forEach(actionInstance -> {
                    actionInstance.setActionStatus(DunningActionInstanceStatusEnum.IGNORED);
                    actionInstance.setExecutionDate(null); // Set execution date to null when action is ignored
                });
            }
            dunningLevelInstances.add(levelInstance);
        });

        // Update the collection plan with the dunning level instances
        collectionPlanToStop.setDunningLevelInstances(dunningLevelInstances);
    }

    /**
     * Update the dunning collection plan infos after stop action
     * @param collectionPlanToStop Collection plan to stop
     * @param dunningStopReason Dunning stop reason
     */
    private void updateDunningCollectionPlanInfosAfterStopAction(DunningCollectionPlan collectionPlanToStop, DunningStopReason dunningStopReason) {
		DunningCollectionPlanStatus collectionPlanStatus = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.STOPPED);
		collectionPlanToStop.setStatus(collectionPlanStatus);
		collectionPlanToStop.setCloseDate(new Date());
		collectionPlanToStop.setDaysOpen((int) daysBetween(collectionPlanToStop.getCloseDate(), new Date()) + 1);
		collectionPlanToStop.setStopReason(dunningStopReason);
        collectionPlanToStop.setNextActionDate(null);
        collectionPlanToStop.setNextAction(null);
	}

    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public DunningCollectionPlan resumeCollectionPlan(DunningCollectionPlan collectionPlanToResume) {
		return resumeCollectionPlan(collectionPlanToResume, true);
	}
	
    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public DunningCollectionPlan resumeCollectionPlan(DunningCollectionPlan collectionPlanToResume, boolean validate) {
    	collectionPlanToResume = retrieveIfNotManaged(collectionPlanToResume);
    	collectionPlanToResume = refreshLevelInstances(collectionPlanToResume);
    	DunningCollectionPlanStatus dunningCollectionPlanStatus = dunningCollectionPlanStatusService.refreshOrRetrieve(collectionPlanToResume.getStatus());

		if(validate) {
			if(!dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.PAUSED)) {
				throw new BusinessApiException("Collection Plan with id " + collectionPlanToResume.getId() + " cannot be resumed, the collection plan is not paused");
			}
		}
		
		Optional<DunningLevelInstance> dunningLevelInstance = collectionPlanToResume.getDunningLevelInstances()
				.stream().max(Comparator.comparing(DunningLevelInstance::getId));
		if(dunningLevelInstance.isEmpty()) {
			throw new BusinessApiException("No dunning level instances found for the collection plan with id "+collectionPlanToResume.getId());
		}

		DunningCollectionPlanStatus collectionPlanStatus = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.ACTIVE);
		collectionPlanToResume.setStatus(collectionPlanStatus);
        collectionPlanToResume.setPauseReason(null);
        int pause = collectionPlanToResume.getPauseDuration();
		collectionPlanToResume.addPauseDuration((int) daysBetween(collectionPlanToResume.getPausedUntilDate(), new Date()));
        collectionPlanToResume.setNextActionDate(addDaysToDate(collectionPlanToResume.getNextActionDate(), (int) daysBetween(collectionPlanToResume.getPausedUntilDate(), new Date())));
        collectionPlanToResume.setPausedUntilDate(null);
		update(collectionPlanToResume);

        updateAllDunningLevelInstancesDate(collectionPlanToResume, collectionPlanToResume.getPauseDuration() - pause);
		return collectionPlanToResume;
	}

    @Override
    public void remove(DunningCollectionPlan entity) throws BusinessException {
    	super.remove(entity);
    }

	public List<DunningCollectionPlan> findDunningCollectionPlansToResume() {
        return getEntityManager()
                .createNamedQuery("DunningCollectionPlan.DCPtoResume", entityClass)
                .setParameter("resumeDate", new Date())
                .getResultList();
	}
	
	private DunningCollectionPlan refreshLevelInstances(DunningCollectionPlan dunningCollectionPlan) {
		List<DunningLevelInstance> dunningLevelInstances = new ArrayList<DunningLevelInstance>();
		for (DunningLevelInstance levelInstance : dunningCollectionPlan.getDunningLevelInstances()) {
		    levelInstance = levelInstanceService.findById(levelInstance.getId());
		    dunningLevelInstances.add(levelInstance);
		}
		
		dunningCollectionPlan.setDunningLevelInstances(dunningLevelInstances);
		return dunningCollectionPlan;
	}

	public List<Long> getActiveInvoiceLevelCollectionPlansIds() {
        return getEntityManager()
                .createNamedQuery("DunningCollectionPlan.activeInvoiceLevelCollectionPlansIds", Long.class)
                .getResultList();
    }

    public List<Long> getActiveCustomerLevelCollectionPlansIds() {
        return getEntityManager()
                .createNamedQuery("DunningCollectionPlan.activeCustomerLevelCollectionPlansIds", Long.class)
                .getResultList();
    }

    public void sendNotification(String emailFrom, BillingAccount billingAccount, EmailTemplate emailTemplate,
                                 Map<Object, Object> params, List<File> attachments) {
        emailTemplate = emailTemplateService.refreshOrRetrieve(emailTemplate);
        if(emailTemplate != null) {
            String languageCode = billingAccount.getCustomerAccount().getTradingLanguage().getLanguage().getLanguageCode();
            String emailSubject = internationalSettingsService.resolveSubject(emailTemplate,languageCode);
            String emailContent = internationalSettingsService.resolveEmailContent(emailTemplate,languageCode);
            String htmlContent = internationalSettingsService.resolveHtmlContent(emailTemplate,languageCode);
            String emailTo = billingAccount.getContactInformation().getEmail();
            String subject = emailTemplate.getSubject() != null
                    ? evaluateExpression(emailSubject, params, String.class) : "";
            String content = emailTemplate.getTextContent() != null
                    ? evaluateExpression(emailContent, params, String.class) : "";
            String contentHtml = emailTemplate.getHtmlContent() != null
                    ? evaluateExpression(htmlContent, params, String.class) : "";
            emailSender.send(emailFrom, asList(emailFrom), asList(emailTo), null, null,
                    subject, content, contentHtml, attachments, null, false);
        } else {
            log.error("Email template not found");
        }
    }

    public void sendNotification(String emailFrom, String customerAccountEmail, String languageCode, EmailTemplate emailTemplate,
                                 Map<Object, Object> params) {
        emailTemplate = emailTemplateService.refreshOrRetrieve(emailTemplate);
        if(emailTemplate != null) {
            String emailSubject = internationalSettingsService.resolveSubject(emailTemplate,languageCode);
            String emailContent = internationalSettingsService.resolveEmailContent(emailTemplate,languageCode);
            String htmlContent = internationalSettingsService.resolveHtmlContent(emailTemplate,languageCode);
            String subject = emailTemplate.getSubject() != null
                    ? evaluateExpression(emailSubject, params, String.class) : "";
            String content = emailTemplate.getTextContent() != null
                    ? evaluateExpression(emailContent, params, String.class) : "";
            String contentHtml = emailTemplate.getHtmlContent() != null
                    ? evaluateExpression(htmlContent, params, String.class) : "";
            emailSender.send(emailFrom, Collections.singletonList(emailFrom), Collections.singletonList(customerAccountEmail), null, null,
                    subject, content, contentHtml, null, null, false);
        } else {
            log.error("Email template not found");
        }
    }
    
    /**
     * Get Active or Paused DunningCollectionPlan by Dunning Settings id
     * @param id DunningSettings id
     * @return A list of {@link DunningCollectionPlan}
     */
    public List<DunningCollectionPlan> getActiveDunningCollectionPlan(Long id){
    	return getEntityManager()
                .createNamedQuery("DunningCollectionPlan.findActive", DunningCollectionPlan.class)
                .setParameter("id", id)
                .getResultList();
    }
    
    /**
     * Get Active or Paused DunningCollectionPlan by Dunning Settings id
     * @param id DunningSettings id
     * @return A list of {@link DunningCollectionPlan}
     */
    public List<DunningCollectionPlan> getPausedDunningCollectionPlan(Long id){
    	return getEntityManager()
                .createNamedQuery("DunningCollectionPlan.findPaused", DunningCollectionPlan.class)
                .setParameter("id", id)
                .getResultList();
    }

    /**
     * Launch payment Action or retry payment
     * @param collectionPlan DunningCollectionPlan
     */
    public void launchPaymentAction(DunningCollectionPlan collectionPlan) {
        PaymentMethod preferredPaymentMethod = null;
        CustomerAccount customerAccount = null;

        if (collectionPlan != null && collectionPlan.getBillingAccount() != null && collectionPlan.getBillingAccount().getCustomerAccount() != null && collectionPlan.getBillingAccount().getCustomerAccount().getPaymentMethods() != null) {
            preferredPaymentMethod = collectionPlan
                    .getBillingAccount()
                    .getCustomerAccount()
                    .getPaymentMethods()
                    .stream()
                    .filter(PaymentMethod::isPreferred)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("No preferred payment method found for customer account"
                            + collectionPlan.getBillingAccount().getCustomerAccount().getCode()));
        } else if(collectionPlan != null &&  collectionPlan.getCustomerAccount() != null && collectionPlan.getCustomerAccount().getPaymentMethods() != null) {
            customerAccount = collectionPlan.getCustomerAccount();
            preferredPaymentMethod = collectionPlan
                    .getCustomerAccount()
                    .getPaymentMethods()
                    .stream()
                    .filter(PaymentMethod::isPreferred)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("No preferred payment method found for customer account"
                            + collectionPlan.getCustomerAccount().getCode()));
        }

        if (collectionPlan != null && customerAccount != null && preferredPaymentMethod != null) {
            //PaymentService.doPayment consider amount to pay in cent so amount should be * 100
            //long amountToPay = collectionPlan.getRelatedInvoice().getNetToPay().multiply(BigDecimal.valueOf(100)).longValue();
            if (collectionPlan.getRelatedInvoices() != null && !collectionPlan.getRelatedInvoices().isEmpty()) {
                long amountToPay = collectionPlan.getRelatedInvoices().stream().mapToLong(invoice -> invoice.getNetToPay().multiply(BigDecimal.valueOf(100)).longValue()).sum();
                List<Invoice> invoices = collectionPlan.getRelatedInvoices().stream().filter(invoice -> invoice.getPaymentStatus().equals(InvoicePaymentStatusEnum.UNPAID)).toList();
                List<Long> ids = new ArrayList<>();
                for (Invoice invoice : invoices) {
                    List<AccountOperation> accountOperations = accountOperationService.listByInvoice(invoice);
                    ids.addAll(accountOperations.stream().map(AccountOperation::getId).toList());
                }
                PaymentGateway paymentGateway = paymentGatewayService.getPaymentGateway(customerAccount, preferredPaymentMethod, null);
                doPayment(preferredPaymentMethod, customerAccount, amountToPay, ids, paymentGateway);
            }
        }
    }

    public void doPayment(PaymentMethod preferredPaymentMethod, CustomerAccount customerAccount, long amountToPay, List<Long> accountOperationsToPayIds, PaymentGateway paymentGateway) {
        CustomerAccount customerAccountToPay = customerAccountService.findById(customerAccount.getId());
        if (preferredPaymentMethod.getPaymentType()== PaymentMethodEnum.DIRECTDEBIT || preferredPaymentMethod.getPaymentType()==PaymentMethodEnum.CARD) {
            try {
                if (accountOperationsToPayIds != null && !accountOperationsToPayIds.isEmpty()) {
                    if (preferredPaymentMethod.getPaymentType() == PaymentMethodEnum.CARD) {
                        if (preferredPaymentMethod instanceof HibernateProxy) {
                            preferredPaymentMethod = (PaymentMethod) ((HibernateProxy) preferredPaymentMethod).getHibernateLazyInitializer().getImplementation();
                        }
                        CardPaymentMethod paymentMethod = (CardPaymentMethod) preferredPaymentMethod;
                        paymentService.doPayment(customerAccountToPay, amountToPay, accountOperationsToPayIds,
                                true, true, paymentGateway, paymentMethod.getCardNumber(),
                                paymentMethod.getCardNumber(), paymentMethod.getHiddenCardNumber(),
                                paymentMethod.getExpirationMonthAndYear(), paymentMethod.getCardType(),
                                true, preferredPaymentMethod.getPaymentType(), false, null);
                    } else {
                        paymentService.doPayment(customerAccountToPay, amountToPay, accountOperationsToPayIds,
                                true, true, paymentGateway, null, null,
                                null, null, null, true, preferredPaymentMethod.getPaymentType(), false, null);
                    }
                }
            } catch (Exception exception) {
                throw new BusinessException("Error occurred during payment process for customer " + customerAccountToPay.getCode(), exception);
            }
        }
    }

    /**
     * Create collection plan for customer level
     * @param customerAccount Customer account
     * @param policy Policy
     * @param collectionPlanStatus Collection plan status
     */
    public void createCollectionPlanForCustomerLevel(CustomerAccount customerAccount, BigDecimal balance, DunningPolicy policy, DunningCollectionPlanStatus collectionPlanStatus,
                                                     List<String> linkedOccTemplates, CustomerBalance customerBalance) {
        customerAccount = customerAccountService.refreshOrRetrieve(customerAccount);
        DunningCollectionPlan collectionPlan = new DunningCollectionPlan();
        collectionPlan.setRelatedPolicy(policy);
        collectionPlan.setCustomerAccount(customerAccount);
        collectionPlan.setCurrentDunningLevelSequence(0);
        collectionPlan.setTotalDunningLevels(policy.getTotalDunningLevels());
        collectionPlan.setStartDate(new Date());
        collectionPlan.setStatus(collectionPlanStatus);
        collectionPlan.setDaysOpen(abs((int) daysBetween(new Date(), collectionPlan.getStartDate())) + 1);
        collectionPlan.setBalance(balance);
        create(collectionPlan);

        collectionPlan.setCollectionPlanNumber("C" + collectionPlan.getId());

        // Get the related invoices
        getRelatedInvoices(customerAccount, linkedOccTemplates, collectionPlan, customerBalance);
        // Get the billing account
        getBillingAccountForCollectionPlan(collectionPlan);

        // Check if there's already a dunning level instances already created => In the case of launching the reminder without creating a collection plan
        List<DunningLevelInstance> dunningLevelInstances = dunningLevelInstanceService.findByCustomerAccountAndEmptyCollectionPlan(customerAccount);
        if (dunningLevelInstances != null && !dunningLevelInstances.isEmpty()) {
            dunningLevelInstances.forEach(dunningLevelInstance -> {
                dunningLevelInstance.setCollectionPlan(collectionPlan);
                dunningLevelInstance.setCollectionPlanStatus(collectionPlanStatus);
                dunningLevelInstanceService.update(dunningLevelInstance);
            });
        }

        // Create the dunning level instances
        if(policy.getDunningLevels() != null && !policy.getDunningLevels().isEmpty()) {
            // Create dunning level instances
            List<DunningLevelInstance> createdDunningLevelInstance = dunningLevelInstanceService.createDunningLevelInstancesWithCollectionPlanForCustomerLevel(customerAccount, policy, collectionPlan, collectionPlanStatus);
            collectionPlan.setDunningLevelInstances(createdDunningLevelInstance);

            // Get the minimum sequence of the dunning levels
            int minSequence = policy.getDunningLevels().stream().map(DunningPolicyLevel::getSequence).min(Integer::compareTo).orElse(0);

            // Update the collection plan with the first dunning level instance
            Optional<DunningLevelInstance> firstDunningLevelInstance = createdDunningLevelInstance.stream()
                    .filter(dunningLevelInstance -> dunningLevelInstance.getSequence() == minSequence)
                    .findFirst();

            // Update the collection plan with the next action and next action date
            Optional<DunningLevelInstance> nextDunningLevelInstance = createdDunningLevelInstance.stream()
                    .filter(dunningLevelInstance -> dunningLevelInstance.getSequence() == minSequence + 1)
                    .filter(dunningLevelInstance -> dunningLevelInstance.getLevelStatus().equals(TO_BE_DONE))
                    .findFirst();

            // Update the collection plan with the first action and next action date
            updateCollectionPlanWhenFirstLevelIsDoneOrIgnored(collectionPlan, firstDunningLevelInstance, nextDunningLevelInstance);
        }

        auditLogService.trackOperation("CREATE DunningCollectionPlan", new Date(), collectionPlan, collectionPlan.getCollectionPlanNumber());
        update(collectionPlan);
    }

    /**
     * Get billing account for collection plan
     * @param collectionPlan Collection plan
     */
    private void getBillingAccountForCollectionPlan(DunningCollectionPlan collectionPlan) {
        List<BillingAccount> billingAccounts = collectionPlan.getRelatedInvoices().stream()
                .map(Invoice::getBillingAccount)
                .distinct()
                .toList();

        if (billingAccounts.size() == 1) {
            collectionPlan.setBillingAccount(billingAccounts.getFirst());
        }
    }

    /**
     * getRelatedInvoices
     * @param customerAccount Customer account
     * @param linkedOccTemplates Linked occ templates
     * @param collectionPlan Collection plan
     */
    private void getRelatedInvoices(CustomerAccount customerAccount, List<String> linkedOccTemplates, DunningCollectionPlan collectionPlan, CustomerBalance customerBalance) {
        List<AccountOperation> accountOperations = accountOperationService.getAccountOperations(customerAccount.getId(),
                null,
                linkedOccTemplates,
                null);
        Predicate<Invoice> unpaidInvoicePredicate = invoice -> List.of(InvoicePaymentStatusEnum.UNPAID, InvoicePaymentStatusEnum.PPAID).contains(invoice.getPaymentStatus());

        accountOperations = accountOperations.stream()
                .filter(ao -> ao.getInvoices().stream().noneMatch(Invoice::isDunningCollectionPlanTriggered))
                .filter(ao -> ao.getInvoices().stream().anyMatch(unpaidInvoicePredicate))
                .collect(Collectors.toList());

        // Filter account operations based on customer balance
        List<AccountOperationDto> result = customerBalanceService.filterAccountOperations(accountOperations, customerBalance);

        result.forEach(accountOperationDto -> {
            AccountOperation accountOperation = accountOperationService.findById(accountOperationDto.getId());
            if (accountOperation != null) {
                if (collectionPlan.getRelatedInvoices() == null) {
                    collectionPlan.setRelatedInvoices(new ArrayList<>());
                }
                collectionPlan.getRelatedInvoices().addAll(accountOperation.getInvoices());
                accountOperation.getInvoices().forEach(invoice -> {
                    invoice.setCollectionPlan(collectionPlan);
                    invoice.setDunningCollectionPlanTriggered(true);
                    invoiceService.update(invoice);
                });
            }
        });
    }
}