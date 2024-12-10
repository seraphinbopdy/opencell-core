package org.meveo.apiv2.dunning.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.meveo.model.payments.PaymentMethodEnum.CARD;
import static org.meveo.model.payments.PaymentMethodEnum.DIRECTDEBIT;
import static org.meveo.model.shared.DateUtils.addDaysToDate;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.api.exception.ActionForbiddenException;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.apiv2.dunning.DunningActionInstanceInput;
import org.meveo.apiv2.dunning.DunningCollectionPlanPause;
import org.meveo.apiv2.dunning.DunningCollectionPlanStop;
import org.meveo.apiv2.dunning.DunningLevelInstanceInput;
import org.meveo.apiv2.dunning.MassPauseDunningCollectionPlan;
import org.meveo.apiv2.dunning.MassStopDunningCollectionPlan;
import org.meveo.apiv2.dunning.MassSwitchDunningCollectionPlan;
import org.meveo.apiv2.dunning.RemoveActionInstanceInput;
import org.meveo.apiv2.dunning.RemoveLevelInstanceInput;
import org.meveo.apiv2.dunning.SwitchDunningCollectionPlan;
import org.meveo.apiv2.dunning.UpdateLevelInstanceInput;
import org.meveo.apiv2.models.Resource;
import org.meveo.apiv2.ordering.services.ApiService;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.dunning.*;
import org.meveo.model.payments.ActionModeEnum;
import org.meveo.model.payments.DunningCollectionPlanStatusEnum;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.audit.logging.AuditLogService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.DunningActionInstanceService;
import org.meveo.service.payments.impl.DunningActionService;
import org.meveo.service.payments.impl.DunningAgentService;
import org.meveo.service.payments.impl.DunningCollectionPlanService;
import org.meveo.service.payments.impl.DunningCollectionPlanStatusService;
import org.meveo.service.payments.impl.DunningLevelInstanceService;
import org.meveo.service.payments.impl.DunningLevelService;
import org.meveo.service.payments.impl.DunningPauseReasonsService;
import org.meveo.service.payments.impl.DunningPolicyLevelService;
import org.meveo.service.payments.impl.DunningPolicyService;
import org.meveo.service.payments.impl.DunningStopReasonsService;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

public class DunningCollectionPlanApiService implements ApiService<DunningCollectionPlan> {

    private static final String SWITCH = "SWITCH";

    @Inject
    private GlobalSettingsVerifier globalSettingsVerifier;

    @Inject
    private ResourceBundle resourceMessages;

    @Inject
    private DunningCollectionPlanService dunningCollectionPlanService;

    @Inject
    private DunningPolicyService dunningPolicyService;

    @Inject
    private DunningPolicyLevelService dunningPolicyLevelService;

    @Inject
    private DunningActionService dunningActionService;

    @Inject
    private DunningActionInstanceService dunningActionInstanceService;

    @Inject
    private DunningAgentService dunningAgentService;

    @Inject
    private DunningLevelService dunningLevelService;

    @Inject
    private DunningLevelInstanceService dunningLevelInstanceService;

    @Inject
    private DunningPauseReasonsService dunningPauseReasonService;

    @Inject
    private DunningStopReasonsService dunningStopReasonService;

    @Inject
    private DunningCollectionPlanStatusService dunningCollectionPlanStatusService;

    @Inject
    private AuditLogService auditLogService;

    @Inject
    private CustomerAccountService customerAccountService;
    
    @Inject
    private BillingAccountService billingAccountService;

    private static final String NO_DUNNING_FOUND = "No Dunning Plan collection found with id : ";

    @Override
    public List<DunningCollectionPlan> list(Long offset, Long limit, String sort, String orderBy, String filter) {
        return null;
    }

    @Override
    public Long getCount(String filter) {
        return null;
    }

    @Override
    public Optional<DunningCollectionPlan> findById(Long id) {
        DunningCollectionPlan dunningCollectionPlan = dunningCollectionPlanService.findById(id);
        return ofNullable(dunningCollectionPlan);
    }

    @Override
    public DunningCollectionPlan create(DunningCollectionPlan baseEntity) {
        return null;
    }

    @Override
    public Optional<DunningCollectionPlan> update(Long id, DunningCollectionPlan baseEntity) {
        return empty();
    }

    @Override
    public Optional<DunningCollectionPlan> patch(Long id, DunningCollectionPlan baseEntity) {
        return empty();
    }

    @Override
    public Optional<DunningCollectionPlan> delete(Long id) {
        DunningCollectionPlan dunningCollectionPlan = findById(id).get();
        dunningCollectionPlanService.remove(dunningCollectionPlan);
        String origine = (dunningCollectionPlan != null) ? dunningCollectionPlan.getCollectionPlanNumber() : "";
        auditLogService.trackOperation("REMOVE", new Date(), dunningCollectionPlan, origine);
        return of(dunningCollectionPlan);
    }

    @Override
    public Optional<DunningCollectionPlan> findByCode(String code) {
        return empty();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Transactional
    public Optional<DunningCollectionPlan> switchCollectionPlan(Long collectionPlanId, SwitchDunningCollectionPlan switchDunningCollectionPlan) {
        globalSettingsVerifier.checkActivateDunning();
        DunningCollectionPlan oldCollectionPlan = dunningCollectionPlanService.findById(collectionPlanId);
        if (oldCollectionPlan == null) {
            throw new EntityDoesNotExistsException("Dunning collection plan with id " + collectionPlanId + " does not exits");
        }

        if(oldCollectionPlan.getStatus().getStatus().equals(DunningCollectionPlanStatusEnum.STOPPED) ||
                oldCollectionPlan.getStatus().getStatus().equals(DunningCollectionPlanStatusEnum.FAILED) ||
                oldCollectionPlan.getStatus().getStatus().equals(DunningCollectionPlanStatusEnum.SUCCESS)) {
            throw new BusinessApiException("Collection Plan with id " + oldCollectionPlan.getId() + " cannot be switched, the current collection plan status is " + oldCollectionPlan.getStatus().getStatus());
        }

        DunningPolicy policy = dunningPolicyService.findById(switchDunningCollectionPlan.getDunningPolicy().getId());
        if (policy == null) {
            throw new EntityDoesNotExistsException("Policy with id " + switchDunningCollectionPlan.getDunningPolicy().getId() + " does not exits");
        }
        DunningPolicyLevel policyLevel = dunningPolicyLevelService.findById(switchDunningCollectionPlan.getPolicyLevel().getId());
        if (policyLevel == null) {
            throw new EntityDoesNotExistsException("Policy level with id " + switchDunningCollectionPlan.getPolicyLevel().getId() + " does not exits");
        }
        Optional<DunningCollectionPlan> optional = of(dunningCollectionPlanService.switchCollectionPlan(oldCollectionPlan, policy, policyLevel));

        auditLogService.trackOperation(SWITCH, new Date(), oldCollectionPlan, oldCollectionPlan.getCollectionPlanNumber());

        optional.ifPresent(dunningCollectionPlan -> auditLogService.trackOperation(SWITCH, new Date(), dunningCollectionPlan, dunningCollectionPlan.getCollectionPlanNumber()));
        return optional;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Transactional
    public void massSwitchCollectionPlan(MassSwitchDunningCollectionPlan massSwitchDunningCollectionPlan) {
        globalSettingsVerifier.checkActivateDunning();
        DunningPolicy policy = dunningPolicyService.findById(massSwitchDunningCollectionPlan.getDunningPolicy().getId());
        if (policy == null) {
            throw new EntityDoesNotExistsException("Policy with id " + massSwitchDunningCollectionPlan.getDunningPolicy().getId() + " does not exits");
        }
        DunningPolicyLevel policyLevel = dunningPolicyLevelService.findById(massSwitchDunningCollectionPlan.getPolicyLevel().getId());
        if (policyLevel == null) {
            throw new EntityDoesNotExistsException("Policy level with id " + massSwitchDunningCollectionPlan.getPolicyLevel().getId() + " does not exits");
        }

        List<Resource> collectionPlanList = massSwitchDunningCollectionPlan.getCollectionPlanList();
        if (collectionPlanList != null) {
            for (Resource collectionPlanResource : collectionPlanList) {
                DunningCollectionPlan collectionPlan = dunningCollectionPlanService.findById(collectionPlanResource.getId());
                if (collectionPlan == null) {
                    throw new EntityDoesNotExistsException("Dunning collection plan with id " + collectionPlanResource.getId() + " does not exits");
                }
                dunningCollectionPlanService.switchCollectionPlan(collectionPlan, policy, policyLevel);
                auditLogService.trackOperation("SWITCH", new Date(), collectionPlan, collectionPlan.getCollectionPlanNumber());
            }
        }
    }

    public Optional<Map<String, Set<Long>>> checkMassSwitch(DunningPolicy policy, List<DunningCollectionPlan> collectionPlans) {
        Set<Long> canBeSwitched = new TreeSet<>();
        Set<Long> canNotBeSwitched = new TreeSet<>();
        Map<String, Set<Long>> massSwitchResult = new HashMap<>();
        
        List<Long> invoiceListId = new ArrayList<Long>();
    	
        for (DunningCollectionPlan collectionPlan : collectionPlans) {
            collectionPlan = dunningCollectionPlanService.findById(collectionPlan.getId());
            if (collectionPlan == null) {
                throw new EntityDoesNotExistsException("Collection plan does not exits");
            }
            invoiceListId.add(collectionPlan.getRelatedInvoice().getId());
        }
        
        List<Invoice> eligibleInvoice = dunningPolicyService.findEligibleInvoicesForPolicyWithInvoiceIds(policy, invoiceListId);

        if (eligibleInvoice != null && !eligibleInvoice.isEmpty()) {
            for (DunningCollectionPlan collectionPlan : collectionPlans) {
                collectionPlan = dunningCollectionPlanService.findById(collectionPlan.getId());
                if (collectionPlan == null) {
                    throw new EntityDoesNotExistsException("Collection plan does not exits");
                }
                for (Invoice invoice : eligibleInvoice) {
                    if (invoice.getId() == collectionPlan.getRelatedInvoice().getId()) {
                    	if(dunningPolicyService.minBalanceTriggerCurrencyCheck(policy, invoice) && dunningPolicyService.minBalanceTriggerCheck(policy, invoice)) {
                            canBeSwitched.add(collectionPlan.getId());
                    	}
                    }
                }
            }
            canNotBeSwitched = collectionPlans.stream().map(DunningCollectionPlan::getId).filter(collectionPlanId -> !canBeSwitched.contains(collectionPlanId)).collect(toSet());
        } else if (!dunningPolicyService.existPolicyRulesCheck(policy)) {
            for (DunningCollectionPlan collectionPlan : collectionPlans) {
                collectionPlan = dunningCollectionPlanService.findById(collectionPlan.getId());
                if (collectionPlan == null) {
                    throw new EntityDoesNotExistsException("Collection plan does not exits");
                }

            	if(dunningPolicyService.minBalanceTriggerCurrencyCheck(policy, collectionPlan.getRelatedInvoice()) && dunningPolicyService.minBalanceTriggerCheck(policy, collectionPlan.getRelatedInvoice())) {
                    canBeSwitched.add(collectionPlan.getId());
            	}
            }
            canNotBeSwitched = collectionPlans.stream().map(DunningCollectionPlan::getId).filter(collectionPlanId -> !canBeSwitched.contains(collectionPlanId)).collect(toSet());
        } else {
            canNotBeSwitched.addAll(collectionPlans.stream().map(DunningCollectionPlan::getId).collect(toList()));
        }
        
        massSwitchResult.put("canBESwitched", canBeSwitched);
        massSwitchResult.put("canNotBESwitched", canNotBeSwitched);
        return of(massSwitchResult);
    }

    public List<DunningPolicy> availableDunningPolicies(Long collectionPlanID) {
        DunningCollectionPlan collectionPlan = dunningCollectionPlanService.findById(collectionPlanID);
        if (collectionPlan == null) {
            throw new EntityDoesNotExistsException(resourceMessages.getString("error.collectionPlan.availablePolicies.collectionPlanNotFound", collectionPlanID));
        }
        Invoice invoice = ofNullable(collectionPlan.getRelatedInvoice())
            .orElseThrow(() -> new EntityDoesNotExistsException("No invoice found for collection plan : " + collectionPlanID));
        return dunningPolicyService.availablePoliciesForSwitch(invoice);
    }

    public Optional<DunningCollectionPlan> pauseCollectionPlan(DunningCollectionPlanPause dunningCollectionPlanPause, Long id) {
        globalSettingsVerifier.checkActivateDunning();
        var collectionPlanToPause = findById(id).orElseThrow(() -> new EntityDoesNotExistsException(NO_DUNNING_FOUND + id));
        DunningPauseReason dunningPauseReason = dunningPauseReasonService.findById(dunningCollectionPlanPause.getDunningPauseReason().getId());
        if (dunningPauseReason == null) {
            throw new EntityDoesNotExistsException("dunning Pause Reason with id " + dunningCollectionPlanPause.getDunningPauseReason().getId() + " does not exits");
        }
        if (dunningCollectionPlanPause.getRetryPaymentOnResumeDate()) {
        	checkPreferredMethodPayment(collectionPlanToPause);
        }
        collectionPlanToPause = dunningCollectionPlanService.pauseCollectionPlan(dunningCollectionPlanPause.getForcePause(), dunningCollectionPlanPause.getPauseUntil(),
            collectionPlanToPause, dunningPauseReason, dunningCollectionPlanPause.getRetryPaymentOnResumeDate());

        auditLogService.trackOperation("PAUSE Reason : " + dunningPauseReason.getPauseReason(), new Date(), collectionPlanToPause, collectionPlanToPause.getCollectionPlanNumber());
        return of(collectionPlanToPause);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void massPauseCollectionPlan(MassPauseDunningCollectionPlan massPauseDunningCollectionPlan) {
        globalSettingsVerifier.checkActivateDunning();
        DunningPauseReason pauseReason = dunningPauseReasonService.findById(massPauseDunningCollectionPlan.getDunningPauseReason().getId());
        if (pauseReason == null) {
            throw new EntityDoesNotExistsException("Dunning Pause Reason with id " + massPauseDunningCollectionPlan.getDunningPauseReason().getId() + " does not exits");
        }

        List<Resource> collectionPlanList = massPauseDunningCollectionPlan.getCollectionPlans();
        if (collectionPlanList != null) {
            for (Resource collectionPlanResource : collectionPlanList) {
                DunningCollectionPlan collectionPlan = dunningCollectionPlanService.findById(collectionPlanResource.getId());
                if (collectionPlan == null) {
                    throw new EntityDoesNotExistsException("Dunning collection plan with id " + collectionPlanResource.getId() + " does not exits");
                }
                if (massPauseDunningCollectionPlan.getRetryPaymentOnResumeDate()) {
                	checkPreferredMethodPayment(collectionPlan);
                }
                dunningCollectionPlanService.pauseCollectionPlan(massPauseDunningCollectionPlan.getForcePause(), massPauseDunningCollectionPlan.getPauseUntil(), collectionPlan,
                    pauseReason, massPauseDunningCollectionPlan.getRetryPaymentOnResumeDate());

                auditLogService.trackOperation("PAUSE Reason : " + pauseReason.getPauseReason(), new Date(), collectionPlan, collectionPlan.getCollectionPlanNumber());
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Transactional
    public Optional<DunningCollectionPlan> stopCollectionPlan(DunningCollectionPlanStop dunningCollectionPlanStop, Long id) {
        globalSettingsVerifier.checkActivateDunning();
        var collectionPlanToStop = findById(id).orElseThrow(() -> new EntityDoesNotExistsException(NO_DUNNING_FOUND + id));
        if(collectionPlanToStop.getStatus().getStatus().equals(DunningCollectionPlanStatusEnum.STOPPED)) {
            throw new BusinessApiException("Collection Plan with id " + collectionPlanToStop.getId() + " cannot be stopped, current status is " + collectionPlanToStop.getStatus().getStatus());
        }
        DunningStopReason dunningStopReason = dunningStopReasonService.findById(dunningCollectionPlanStop.getDunningStopReason().getId());
        if (dunningStopReason == null) {
            throw new EntityDoesNotExistsException("dunning Pause Reason with id " + dunningCollectionPlanStop.getDunningStopReason().getId() + " does not exits");
        }
        collectionPlanToStop = dunningCollectionPlanService.stopCollectionPlan(collectionPlanToStop, dunningStopReason);

        String origine = (collectionPlanToStop != null) ? collectionPlanToStop.getCollectionPlanNumber() : "";
        auditLogService.trackOperation("STOP Reason : " + dunningStopReason.getStopReason(), new Date(), collectionPlanToStop, origine);
        return of(collectionPlanToStop);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void massStopCollectionPlan(MassStopDunningCollectionPlan massStopDunningCollectionPlan) {
        globalSettingsVerifier.checkActivateDunning();
        DunningStopReason stopReason = dunningStopReasonService.findById(massStopDunningCollectionPlan.getDunningStopReason().getId());
        if (stopReason == null) {
            throw new EntityDoesNotExistsException("Dunning Stop Reason with id " + massStopDunningCollectionPlan.getDunningStopReason().getId() + " does not exits");
        }

        List<Resource> collectionPlanList = massStopDunningCollectionPlan.getCollectionPlans();
        if (collectionPlanList != null) {
            for (Resource collectionPlanResource : collectionPlanList) {
                DunningCollectionPlan collectionPlan = dunningCollectionPlanService.findById(collectionPlanResource.getId());
                if (collectionPlan == null) {
                    throw new EntityDoesNotExistsException("Dunning collection plan with id " + collectionPlanResource.getId() + " does not exits");
                }
                dunningCollectionPlanService.stopCollectionPlan(collectionPlan, stopReason);
                auditLogService.trackOperation("STOP Reason : " + stopReason.getStopReason(), new Date(), collectionPlan, collectionPlan.getCollectionPlanNumber());
            }
        }
    }

    public Optional<DunningCollectionPlan> resumeCollectionPlan(Long id) {
        globalSettingsVerifier.checkActivateDunning();
        var collectionPlanToResume = findById(id).orElseThrow(() -> new EntityDoesNotExistsException(NO_DUNNING_FOUND + id));
        collectionPlanToResume = dunningCollectionPlanService.resumeCollectionPlan(collectionPlanToResume);

        String origine = (collectionPlanToResume != null) ? collectionPlanToResume.getCollectionPlanNumber() : "";
        auditLogService.trackOperation("RESUME", new Date(), collectionPlanToResume, origine);
        return of(collectionPlanToResume);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeDunningLevelInstance(RemoveLevelInstanceInput removeLevelInstanceInput) {
        globalSettingsVerifier.checkActivateDunning();
        try {
            List<Resource> levelInstanceResources = removeLevelInstanceInput.getLevels();

            if (levelInstanceResources != null) {
                for (Resource levelInstanceResource : levelInstanceResources) {

                    Long levelInstanceId = levelInstanceResource.getId();
                    DunningLevelInstance levelInstanceToRemove = dunningLevelInstanceService.findById(levelInstanceId, Arrays.asList("collectionPlan", "dunningLevel", "actions"));
                    if (levelInstanceToRemove == null) {
                        throw new EntityDoesNotExistsException("No Dunning Level Instance found with id : " + levelInstanceId);
                    }
                    // User can not delete the end level
                    if (levelInstanceToRemove.getDunningLevel().isEndOfDunningLevel()) {
                        throw new ActionForbiddenException("Can not delete the end level");
                    }
                    DunningCollectionPlan collectionPlan = levelInstanceToRemove.getCollectionPlan();
                    // User can not the current dunning level instance
                    Integer currentDunningLevelSequence = collectionPlan.getCurrentDunningLevelSequence();
                    if (levelInstanceToRemove.getSequence() == currentDunningLevelSequence) {
                        throw new ActionForbiddenException("Can not delete the current dunning level instance");
                    }
                    // If the dunningLevelInstance status is DONE or IN_PROGRESS
                    if (levelInstanceToRemove.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE
                            || levelInstanceToRemove.getLevelStatus() == DunningLevelInstanceStatusEnum.IN_PROGRESS) {
                        throw new ActionForbiddenException("Can not delete dunningLevelInstance with status DONE or IN_PROGRESS");
                    }
                    if (levelInstanceToRemove.getActions() != null) {
                        for (DunningActionInstance action : levelInstanceToRemove.getActions()) {
                            dunningActionInstanceService.remove(action);
                        }
                    }
                    dunningLevelInstanceService.remove(levelInstanceToRemove);

                    // Update DunningCollectionPlan totalDunningLevels
                    if (collectionPlan.getTotalDunningLevels() == null) {
                        collectionPlan.setTotalDunningLevels(0);
                    }
                    if (collectionPlan.getTotalDunningLevels() > 0) {
                        collectionPlan.setTotalDunningLevels(collectionPlan.getTotalDunningLevels() - 1);
                    }

                    // if the deleted dunningLevelInstance sequence = currentSequence + 1
                    if (currentDunningLevelSequence != null && levelInstanceToRemove.getSequence() == currentDunningLevelSequence + 1) {
                        DunningLevelInstance nextLevelInstance = dunningLevelInstanceService.findBySequence(collectionPlan, currentDunningLevelSequence + 1);
                        String nextLevelAction = null;
                        if (nextLevelInstance != null && nextLevelInstance.getActions() != null && !nextLevelInstance.getActions().isEmpty()) {
                            for (DunningActionInstance nextActionInstance : nextLevelInstance.getActions()) {
                                if (nextActionInstance.getActionMode() == ActionModeEnum.AUTOMATIC) {
                                    nextLevelAction = nextActionInstance.getCode();
                                    break;
                                }
                            }
                            if (nextLevelAction == null) {
                                nextLevelAction = nextLevelInstance.getActions().get(0).getCode();
                            }

                            collectionPlan.setNextAction(nextLevelAction);
                            collectionPlan
                                .setNextActionDate(addDaysToDate(collectionPlan.getStartDate(), nextLevelInstance.getDaysOverdue() + collectionPlan.getPauseDuration()));
                        }
                    }

                    dunningCollectionPlanService.update(collectionPlan);
                    // update the sequence of other levels
                    dunningLevelInstanceService.decrementSequecesGreaterThanDaysOverdue(collectionPlan, levelInstanceToRemove.getDaysOverdue());

                    String origine = (levelInstanceToRemove.getCollectionPlan() != null) ? levelInstanceToRemove.getCollectionPlan().getCollectionPlanNumber() : "";
                    auditLogService.trackOperation("REMOVE DunningLevelInstance", new Date(), levelInstanceToRemove.getCollectionPlan(), origine);
                }
            }
        } catch (MeveoApiException e) {
            throw e;
        } catch (Exception e) {
            throw new MeveoApiException(e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeDunningActionInstance(RemoveActionInstanceInput removeActionInstanceInput) {
        globalSettingsVerifier.checkActivateDunning();
        try {
            List<Resource> actionInstanceResources = removeActionInstanceInput.getActions();

            if (actionInstanceResources != null) {
                for (Resource actionInstanceResource : actionInstanceResources) {
                    Long actionInstanceId = actionInstanceResource.getId();
                    DunningActionInstance dunningActionInstance = dunningActionInstanceService.findById(actionInstanceId, Arrays.asList("collectionPlan", "dunningLevelInstance"));
                    if (dunningActionInstance == null) {
                        throw new EntityDoesNotExistsException("No Dunning Action Instance found with id : " + actionInstanceId);
                    }

                    // 1- User can not either modify or delete the end level!
                    DunningLevelInstance dunningLevelInstance = dunningLevelInstanceService.findById(dunningActionInstance.getDunningLevelInstance().getId(),
                        Arrays.asList("dunningLevel", "actions", "collectionPlan"));
                    if (dunningLevelInstance.getDunningLevel().isEndOfDunningLevel()) {
                        throw new ActionForbiddenException("Can not modify or delete the end level");
                    }
                    // 2- If the dunningActionInstance status is DONE ==> it can not be deleted.
                    if (dunningActionInstance.getActionStatus() != null && dunningActionInstance.getActionStatus() == DunningActionInstanceStatusEnum.DONE) {
                        throw new ActionForbiddenException("Can not delete an action instance with status DONE");
                    }
                    // 3- If the remaining DunningActionInstance of the dunningLevelInstance are DONE
                    List<DunningActionInstance> actions = dunningLevelInstance.getActions();
                    actions.removeIf(a -> a.getId() == dunningActionInstance.getId());

                    boolean remainingActionsAreDone = true;
                    for (DunningActionInstance action : actions) {
                        if (action.getActionStatus() != DunningActionInstanceStatusEnum.DONE) {
                            remainingActionsAreDone = false;
                            break;
                        }
                    }
                    if (remainingActionsAreDone) {
                        // 3.1- Update the dunningLevelInstance status also to
                        dunningLevelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.DONE);
                        dunningLevelInstance.setExecutionDate(new Date());
                        dunningLevelInstanceService.update(dunningLevelInstance);
                        // 3.2- Update DunningCollectionPlan : currentDunningLevelSequence / lastAction / lastActionDate / nextAction /nextActionDate
                        updateCollectionPlanActions(dunningLevelInstance);
                    }

                    dunningActionInstanceService.remove(dunningActionInstance);

                    String origine = (dunningActionInstance.getCollectionPlan() != null) ? dunningActionInstance.getCollectionPlan().getCollectionPlanNumber() : "";
                    auditLogService.trackOperation("REMOVE DunningActionInstance", new Date(), dunningActionInstance.getCollectionPlan(), origine);
                }
            }
        } catch (MeveoApiException e) {
            throw e;
        } catch (Exception e) {
            throw new MeveoApiException(e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Transactional
    public Optional<DunningLevelInstance> addDunningLevelInstance(DunningLevelInstanceInput dunningLevelInstanceInput) {
        globalSettingsVerifier.checkActivateDunning();
        try {
            DunningLevelInstance newDunningLevelInstance = new DunningLevelInstance();

            Long collectionPlanId = dunningLevelInstanceInput.getCollectionPlan().getId();
            DunningCollectionPlan collectionPlan = findById(collectionPlanId).orElseThrow(() -> new EntityDoesNotExistsException(NO_DUNNING_FOUND + collectionPlanId));
            newDunningLevelInstance.setCollectionPlan(collectionPlan);

            // 1- Can not create a new dunning level instance if :
            Long dunningLevelId = dunningLevelInstanceInput.getDunningLevel().getId();
            var dunningLevel = dunningLevelService.findById(dunningLevelId);

            // Check if we can add a new dunning level instance to the collection plan
            validateAddDunningLevelToCollectionPlan(dunningLevel);

            if (dunningLevelInstanceInput.getExecutionDate() != null) {
                // Validate the execution date
                validateExecutionDate(collectionPlan, dunningLevelInstanceInput.getExecutionDate());
                newDunningLevelInstance.setExecutionDate(dunningLevelInstanceInput.getExecutionDate());
            }

            newDunningLevelInstance.setDunningLevel(dunningLevel);

            // check daysOverdue
            Integer daysOverdue = dunningLevelInstanceInput.getDaysOverdue();
            //checkDaysOverdue(collectionPlan, daysOverdue);
            newDunningLevelInstance.setDaysOverdue(daysOverdue);

            if (dunningLevelInstanceInput.getLevelStatus() != null) {
                newDunningLevelInstance.setLevelStatus(dunningLevelInstanceInput.getLevelStatus());
                if (dunningLevelInstanceInput.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE || dunningLevelInstanceInput.getLevelStatus() == DunningLevelInstanceStatusEnum.IN_PROGRESS) {
                    newDunningLevelInstance.setExecutionDate(new Date());
                } else if (dunningLevelInstanceInput.getLevelStatus() == DunningLevelInstanceStatusEnum.IGNORED) {
                    newDunningLevelInstance.setExecutionDate(null);
                }
            }

            // 2- set sequence
            Integer minSequence = dunningLevelInstanceService.getMinSequenceByDaysOverdue(collectionPlan, daysOverdue);
            newDunningLevelInstance.setSequence(minSequence.intValue());

            if (collectionPlan != null) {
                // Check the related invoice and set it to the level instance
                if (collectionPlan.getRelatedInvoice() != null) {
                    newDunningLevelInstance.setInvoice(collectionPlan.getRelatedInvoice());
                }

                // Check the related customer account and set it to the level instance
                if (collectionPlan.getCustomerAccount() != null) {
                    newDunningLevelInstance.setCustomerAccount(collectionPlan.getCustomerAccount());
                } else if (collectionPlan.getBillingAccount() != null && collectionPlan.getBillingAccount().getCustomerAccount() != null) {
                    newDunningLevelInstance.setCustomerAccount(collectionPlan.getBillingAccount().getCustomerAccount());
                }
            }

            dunningLevelInstanceService.create(newDunningLevelInstance);

            // 3- update dunningLevelInstances
            dunningLevelInstanceService.incrementSequecesGreaterThanDaysOverdue(collectionPlan, daysOverdue);

            // 4- update DunningCollectionPlan totalDunningLevels;
            if (collectionPlan.getTotalDunningLevels() == null) {
                collectionPlan.setTotalDunningLevels(0);
            }
            collectionPlan.setTotalDunningLevels(collectionPlan.getTotalDunningLevels() + 1);

            dunningCollectionPlanService.update(collectionPlan);

            // Create actions
            createActions(newDunningLevelInstance, dunningLevelInstanceInput.getActions());

            auditLogService.trackOperation("ADD DunningLevelInstance", new Date(), collectionPlan, collectionPlan.getCollectionPlanNumber());
            return of(newDunningLevelInstance);
        } catch (MeveoApiException e) {
            throw e;
        } catch (Exception e) {
            throw new MeveoApiException(e);
        }
    }

    /**
     * Validate the execution date.
     * @param collectionPlan the collection plan
     * @param executionDate the execution date
     */
    private void validateExecutionDate(DunningCollectionPlan collectionPlan, Date executionDate) {
        DunningLevelInstance firstDunningLevel = getFirstDunningLevelInstance(collectionPlan);
        DunningLevelInstance inProgressDunningLevel = findFirstInProgressDunningLevel(collectionPlan);
        DunningLevelInstance lastDoneInstance = findLastDoneDunningLevel(collectionPlan);
        DunningLevelInstance endDunningLevel = findEndDunningLevel(collectionPlan);

        if (executionDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(collectionPlan.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())) {
            throw new BusinessApiException("The execution date must be after the collection plan start date");
        }

        if (firstDunningLevel != null && firstDunningLevel.getExecutionDate() != null &&
                executionDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(firstDunningLevel.getExecutionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())) {
            throw new BusinessApiException("The execution date must be after the first dunning level instance execution date");
        }

        if (inProgressDunningLevel != null && inProgressDunningLevel.getExecutionDate() != null
                && executionDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(inProgressDunningLevel.getExecutionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())) {
            throw new BusinessApiException("The execution date must be after the current dunning level instance execution date");
        }

        if (lastDoneInstance != null && lastDoneInstance.getExecutionDate() != null
                && executionDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(lastDoneInstance.getExecutionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())) {
            throw new BusinessApiException("The execution date must be after the last done dunning level instance execution date");
        }

        if (endDunningLevel != null && endDunningLevel.getExecutionDate() != null
                && executionDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(endDunningLevel.getExecutionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())) {
            throw new BusinessApiException("The execution date must be before the end dunning level instance execution date");
        }
    }

    /**
     * Get the first dunning level instance.
     * @param collectionPlan the collection plan
     * @return the first dunning level instance
     */
    private DunningLevelInstance getFirstDunningLevelInstance(DunningCollectionPlan collectionPlan) {
        return collectionPlan.getDunningLevelInstances().stream()
                .min(Comparator.comparing(DunningLevelInstance::getDaysOverdue))
                .orElse(null);
    }

    /**
     * Get the first in progress dunning level instance.
     * @param collectionPlan the collection plan
     * @return the first in progress dunning level instance
     */
    private DunningLevelInstance findFirstInProgressDunningLevel(DunningCollectionPlan collectionPlan) {
        return collectionPlan.getDunningLevelInstances().stream()
                .filter(dunningLevelInstance -> dunningLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.IN_PROGRESS)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the last done dunning level instance.
     * @param collectionPlan the collection plan
     * @return the last done dunning level instance
     */
    private DunningLevelInstance findLastDoneDunningLevel(DunningCollectionPlan collectionPlan) {
        return collectionPlan.getDunningLevelInstances().stream()
                .filter(dunningLevelInstance -> dunningLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * Get the end dunning level instance.
     * @param collectionPlan the collection plan
     * @return the end dunning level instance
     */
    private DunningLevelInstance findEndDunningLevel(DunningCollectionPlan collectionPlan) {
        return collectionPlan.getDunningLevelInstances().stream()
                .filter(dunningLevelInstance -> dunningLevelInstance.getDunningLevel().isEndOfDunningLevel())
                .findFirst()
                .orElse(null);
    }


    /**
     * Validate the dunning level instance.
     * @param dunningLevel the dunning level
     */
    private void validateAddDunningLevelToCollectionPlan(DunningLevel dunningLevel) {
        // dunningLevel.isReminderLevel is TRUE
        if (Boolean.TRUE.equals(dunningLevel.isReminder())) {
            throw new ActionForbiddenException("Can not create a new dunning level instance if dunningLevel.isReminderLevel is TRUE");
        }
        // dunningLevel.isEndOfDunningLevel is TRUE
        if (Boolean.TRUE.equals(dunningLevel.isEndOfDunningLevel())) {
            throw new ActionForbiddenException("Can not create a new dunning level instance if dunningLevel.isEndOfDunningLevel is TRUE");
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Transactional
    public Optional<DunningLevelInstance> updateDunningLevelInstance(UpdateLevelInstanceInput updateLevelInstanceInput, Long levelInstanceId) {
        globalSettingsVerifier.checkActivateDunning();
        try {
            DunningLevelInstance levelInstanceToUpdate = dunningLevelInstanceService.findById(levelInstanceId, Arrays.asList("dunningLevel", "actions", "collectionPlan"));
            canUpdateLevelInstance(levelInstanceId, levelInstanceToUpdate);
            DunningCollectionPlan collectionPlan = getCollectionPlanByLevelInstance(levelInstanceToUpdate);

            List<String> fields = new ArrayList<>();

            Integer oldDaysOverdue = levelInstanceToUpdate.getDaysOverdue();
            Integer newDaysOverdue = updateLevelInstanceInput.getDaysOverdue();

            if (!Objects.equals(oldDaysOverdue, newDaysOverdue)) {
                // check daysOverdue
                //checkDaysOverdue(collectionPlan, newDaysOverdue);
                fields.add("daysOverdue");
                levelInstanceToUpdate.setDaysOverdue(newDaysOverdue);
            }

            // Validate the execution date
            validateExecutionDate(collectionPlan, updateLevelInstanceInput.getExecutionDate());

            if (updateLevelInstanceInput.getLevelStatus() != null) {
                if (updateLevelInstanceInput.getLevelStatus() != levelInstanceToUpdate.getLevelStatus()) {
                    fields.add("levelStatus");
                }
                levelInstanceToUpdate.setLevelStatus(updateLevelInstanceInput.getLevelStatus());
                if (levelInstanceToUpdate.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE || levelInstanceToUpdate.getLevelStatus() == DunningLevelInstanceStatusEnum.IN_PROGRESS) {
                    levelInstanceToUpdate.setExecutionDate(new Date());
                    Date executionDate = levelInstanceToUpdate.getExecutionDate();
                    levelInstanceToUpdate.getActions().forEach(dunningActionInstance -> {
                        dunningActionInstance.setExecutionDate(executionDate);
                        if (updateLevelInstanceInput.getLevelStatus() != null && updateLevelInstanceInput.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
                            dunningActionInstance.setActionStatus(DunningActionInstanceStatusEnum.DONE);
                        }
                        dunningActionInstanceService.update(dunningActionInstance);
                    });
                } else if (levelInstanceToUpdate.getLevelStatus() == DunningLevelInstanceStatusEnum.IGNORED) {
                    levelInstanceToUpdate.setExecutionDate(null);
                }
            }

            dunningLevelInstanceService.update(levelInstanceToUpdate);

            if (updateLevelInstanceInput.getActions() != null) {
                fields.add("actions");
                for (DunningActionInstance action : levelInstanceToUpdate.getActions()) {
                    dunningActionInstanceService.remove(action);
                }
                createActions(levelInstanceToUpdate, updateLevelInstanceInput.getActions());
            }
            // If "levelStatus" : "DONE" ==> update all its DunningActionInstance to "DONE".
            else if (updateLevelInstanceInput.getLevelStatus() != null && updateLevelInstanceInput.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
                dunningActionInstanceService.updateStatus(DunningActionInstanceStatusEnum.DONE, levelInstanceToUpdate);
                levelInstanceToUpdate = dunningLevelInstanceService.findById(levelInstanceId, Arrays.asList("dunningLevel", "actions", "collectionPlan"));
            }

            updateCollectionPlanActions(levelInstanceToUpdate);

            // 2- Update sequences
            if (!Objects.equals(oldDaysOverdue, newDaysOverdue)) {
                List<DunningLevelInstance> levelInstances = dunningLevelInstanceService.findByCollectionPlan(collectionPlan);

                int i = 0;
                for (DunningLevelInstance dunningLevelInstance : levelInstances) {
                    dunningLevelInstance.setSequence(i++);
                    dunningLevelInstanceService.update(dunningLevelInstance);
                }
            }

            String origine = (collectionPlan != null) ? collectionPlan.getCollectionPlanNumber() : "";
            auditLogService.trackOperation("UPDATE DunningLevelInstance", new Date(), collectionPlan, origine, fields);
            return of(levelInstanceToUpdate);
        } catch (MeveoApiException e) {
            throw e;
        } catch (Exception e) {
            throw new MeveoApiException(e);
        }
    }

    /**
     * Get the collection plan by level instance.
     * @param levelInstanceToUpdate the level instance to update
     * @return the collection plan
     */
    private DunningCollectionPlan getCollectionPlanByLevelInstance(DunningLevelInstance levelInstanceToUpdate) {
        DunningCollectionPlan collectionPlan = dunningCollectionPlanService.findById(levelInstanceToUpdate.getCollectionPlan().getId());

        if(collectionPlan.getStatus().getStatus().equals(DunningCollectionPlanStatusEnum.STOPPED)) {
            throw new BusinessApiException("Collection Plan with id " + collectionPlan.getId() + " cannot be edited, the current collection plan status is " + collectionPlan.getStatus().getStatus());
        }

        if (collectionPlan.getRelatedInvoice() != null) {
            levelInstanceToUpdate.setInvoice(collectionPlan.getRelatedInvoice());
        }

        if (collectionPlan.getCustomerAccount() != null) {
            levelInstanceToUpdate.setCustomerAccount(collectionPlan.getCustomerAccount());
        } else if (collectionPlan.getBillingAccount() != null && collectionPlan.getBillingAccount().getCustomerAccount() != null) {
            levelInstanceToUpdate.setCustomerAccount(collectionPlan.getBillingAccount().getCustomerAccount());
        }

        return collectionPlan;
    }

    /**
     * Check if we can update the level instance.
     * @param levelInstanceId the level instance id
     * @param levelInstanceToUpdate the level instance to update
     */
    private void canUpdateLevelInstance(Long levelInstanceId, DunningLevelInstance levelInstanceToUpdate) {
        if (levelInstanceToUpdate == null) {
            throw new EntityDoesNotExistsException("No Dunning Level Instance found with id : " + levelInstanceId);
        }

        if (levelInstanceToUpdate.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
            throw new ActionForbiddenException("Can not update a DONE dunningLevelInstance");
        }

        if (Boolean.TRUE.equals(levelInstanceToUpdate.getDunningLevel().isReminder())) {
            throw new ActionForbiddenException("Can not update a new dunning level instance if dunningLevel.isReminderLevel is TRUE");
        }
    }

    /**
     * Add dunning action instance.
     * @param dunningActionInstanceInput the dunning action instance input
     * @return the optional of dunning action instance
     */
    public Optional<DunningActionInstance> addDunningActionInstance(DunningActionInstanceInput dunningActionInstanceInput) {
        globalSettingsVerifier.checkActivateDunning();
        DunningActionInstance dunningActionInstance = new DunningActionInstance();

        if (dunningActionInstanceInput.getDunningLevelInstance() == null || dunningActionInstanceInput.getDunningLevelInstance().getId() == null) {
            throw new ActionForbiddenException("Attribut dunningLevelInstance is mandatory");
        }
        Long dunningLevelInstanceId = dunningActionInstanceInput.getDunningLevelInstance().getId();
        DunningLevelInstance dunningLevelInstance = dunningLevelInstanceService.findById(dunningLevelInstanceId, Arrays.asList("dunningLevel"));
        if (dunningLevelInstance == null) {
            throw new EntityDoesNotExistsException("No Dunning Level found with id : " + dunningLevelInstanceId);
        } else if (dunningLevelInstance.getDunningLevel().isEndOfDunningLevel() == true) {
            throw new ActionForbiddenException("Cant not add actions at the end of dunning level with id : " + dunningLevelInstanceId);
        } else {
            dunningActionInstance.setDunningLevelInstance(dunningLevelInstance);
        }

        if (dunningActionInstanceInput.getCode() != null) {
            DunningActionInstance dunningActionInstanceExist = dunningActionInstanceService.findByCodeAndDunningLevelInstance(dunningActionInstanceInput.getCode(),
                dunningLevelInstanceId);
            if (dunningActionInstanceExist != null) {
                throw new EntityAlreadyExistsException("Dunning Action Instance with code : " + dunningActionInstanceInput.getCode() + " already exist");
            }
        }

        if (dunningActionInstanceInput.getCollectionPlan() == null || dunningActionInstanceInput.getCollectionPlan().getId() == null) {
            throw new ActionForbiddenException("Attribut collectionPlan is mandatory");
        }
        Long collectionPlanId = dunningActionInstanceInput.getCollectionPlan().getId();
        DunningCollectionPlan collectionPlan = dunningCollectionPlanService.findById(collectionPlanId);
        if (collectionPlan == null) {
            throw new EntityDoesNotExistsException("No Dunning Collection Plan found with id : " + collectionPlanId);
        }
        dunningActionInstance.setCollectionPlan(collectionPlan);

        if (dunningActionInstanceInput.getDunningAction() != null && dunningActionInstanceInput.getDunningAction().getId() != null) {
            Long dunningActionId = dunningActionInstanceInput.getDunningAction().getId();
            DunningAction dunningAction = dunningActionService.findById(dunningActionId);
            if (dunningAction == null) {
                throw new EntityDoesNotExistsException("No Dunning action found with id : " + dunningActionId);
            }
            dunningActionInstance.setDunningAction(dunningAction);
        }

        if (dunningActionInstanceInput.getActionOwner() != null && dunningActionInstanceInput.getActionOwner().getId() != null) {
            Long dunningAgentId = dunningActionInstanceInput.getActionOwner().getId();
            DunningAgent dunningAgent = dunningAgentService.findById(dunningAgentId);
            if (dunningAgent == null) {
                throw new EntityDoesNotExistsException("No Dunning agent found with id : " + dunningAgentId);
            }
            dunningActionInstance.setActionOwner(dunningAgent);
        }

        dunningActionInstance.setCode(dunningActionInstanceInput.getCode());
        dunningActionInstance.setDescription(dunningActionInstanceInput.getDescription());
        dunningActionInstance.setActionType(dunningActionInstanceInput.getActionType());
        dunningActionInstance.setActionMode(dunningActionInstanceInput.getMode());
        if (dunningActionInstanceInput.getActionStatus() != null) {
            dunningActionInstance.setActionStatus(dunningActionInstanceInput.getActionStatus());
            updateDunningActionInstanceExecutionDate(dunningActionInstanceInput, dunningActionInstance);
        }
        dunningActionInstance.setActionRestult(dunningActionInstanceInput.getActionRestult());

        dunningActionInstanceService.create(dunningActionInstance);

        auditLogService.trackOperation("ADD DunningActionInstance", new Date(), collectionPlan, collectionPlan.getCollectionPlanNumber());
        return of(dunningActionInstance);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Transactional
    public Optional<DunningActionInstance> updateDunningActionInstance(DunningActionInstanceInput dunningActionInstanceInput, Long actionInstanceId) {
        globalSettingsVerifier.checkActivateDunning();
        try {
            DunningActionInstance dunningActionInstanceToUpdate = dunningActionInstanceService.findById(actionInstanceId, Arrays.asList("collectionPlan", "dunningLevelInstance"));
            if (dunningActionInstanceToUpdate == null) {
                throw new EntityDoesNotExistsException("No Dunning Action Instance found with id : " + actionInstanceId);
            }

            if (dunningActionInstanceToUpdate.getActionStatus() == DunningActionInstanceStatusEnum.DONE) {
                throw new ActionForbiddenException("Can not update a DONE dunningActionInstance");
            }

            List<String> fields = new ArrayList<>();

            Long dunningLevelInstanceIdInput = dunningActionInstanceInput.getDunningLevelInstance() != null ? dunningActionInstanceInput.getDunningLevelInstance().getId() : null;
            Long dunningLevelInstanceIdToUpdate = dunningActionInstanceToUpdate.getDunningLevelInstance() != null ? dunningActionInstanceToUpdate.getDunningLevelInstance().getId()
                    : null;

            if (dunningLevelInstanceIdInput == null) {
                throw new ActionForbiddenException("Attribut dunningLevelInstance is mandatory");
            }
            DunningLevelInstance dunningLevelInstance = dunningLevelInstanceService.findById(dunningLevelInstanceIdInput,
                Arrays.asList("dunningLevel", "collectionPlan", "actions"));
            if (dunningLevelInstance == null) {
                throw new EntityDoesNotExistsException("No Dunning Level Instance found with id : " + dunningLevelInstanceIdInput);
            }

            if (!Objects.equals(dunningLevelInstanceIdInput, dunningLevelInstanceIdToUpdate)) {
                fields.add("dunningLevelInstance");
                dunningActionInstanceToUpdate.setDunningLevelInstance(dunningLevelInstance);
            }

            Long dunningActionIdInput = dunningActionInstanceInput.getDunningAction() != null ? dunningActionInstanceInput.getDunningAction().getId() : null;
            Long dunningActionIdToUpdate = dunningActionInstanceToUpdate.getDunningAction() != null ? dunningActionInstanceToUpdate.getDunningAction().getId() : null;
            if (!Objects.equals(dunningActionIdInput, dunningActionIdToUpdate)) {

                fields.add("dunningAction");
                if (dunningActionIdInput != null) {
                    DunningAction dunningAction = dunningActionService.findById(dunningActionIdInput);
                    if (dunningAction == null) {
                        throw new EntityDoesNotExistsException("No Dunning action found with id : " + dunningActionIdInput);
                    }
                    dunningActionInstanceToUpdate.setDunningAction(dunningAction);
                } else {
                    dunningActionInstanceToUpdate.setDunningAction(null);
                }
            }

            Long actionOwnerIdInput = dunningActionInstanceInput.getActionOwner() != null ? dunningActionInstanceInput.getActionOwner().getId() : null;
            Long actionOwnerIdToUpdate = dunningActionInstanceToUpdate.getActionOwner() != null ? dunningActionInstanceToUpdate.getActionOwner().getId() : null;
            if (!Objects.equals(actionOwnerIdInput, actionOwnerIdToUpdate)) {

                fields.add("actionOwner");
                if (actionOwnerIdInput != null) {
                    DunningAgent dunningAgent = dunningAgentService.findById(actionOwnerIdInput);
                    if (dunningAgent == null) {
                        throw new EntityDoesNotExistsException("No Dunning agent found with id : " + actionOwnerIdInput);
                    }
                    dunningActionInstanceToUpdate.setActionOwner(dunningAgent);
                } else {
                    dunningActionInstanceToUpdate.setActionOwner(null);
                }

            }
            if (StringUtils.isNotBlank(dunningActionInstanceInput.getCode()) && !Objects.equals(dunningActionInstanceInput.getCode(), dunningActionInstanceToUpdate.getCode())) {
                fields.add("code");
                dunningActionInstanceToUpdate.setCode(dunningActionInstanceInput.getCode());
            }
            if (!Objects.equals(dunningActionInstanceInput.getDescription(), dunningActionInstanceToUpdate.getDescription())) {
                fields.add("description");
                dunningActionInstanceToUpdate.setDescription(dunningActionInstanceInput.getDescription());
            }
            if (dunningActionInstanceInput.getActionType() != null && !Objects.equals(dunningActionInstanceInput.getActionType(), dunningActionInstanceToUpdate.getActionType())) {
                fields.add("actionType");
                dunningActionInstanceToUpdate.setActionType(dunningActionInstanceInput.getActionType());
            }
            if (dunningActionInstanceInput.getMode() != null && !Objects.equals(dunningActionInstanceInput.getMode(), dunningActionInstanceToUpdate.getActionMode())) {
                fields.add("actionMode");
                dunningActionInstanceToUpdate.setActionMode(dunningActionInstanceInput.getMode());
            }
            if (dunningActionInstanceInput.getActionStatus() != null) {
                if (!Objects.equals(dunningActionInstanceInput.getActionStatus(), dunningActionInstanceToUpdate.getActionStatus())) {
                    fields.add("actionStatus");
                }
                dunningActionInstanceToUpdate.setActionStatus(dunningActionInstanceInput.getActionStatus());
                updateDunningActionInstanceExecutionDate(dunningActionInstanceInput, dunningActionInstanceToUpdate);

                // 2- If the DunningActionInstance status is changed to DONE:
                if (dunningActionInstanceInput.getActionStatus() == DunningActionInstanceStatusEnum.DONE) {

                    List<DunningActionInstance> actions = dunningLevelInstance.getActions();
                    actions.removeIf(a -> a.getId().equals(dunningActionInstanceToUpdate.getId()));

                    // check if all actions of the dunningLevelInstance are DONE:
                    boolean remainingActionsAreDone = true;
                    for (DunningActionInstance action : actions) {
                        if (action.getActionStatus() != DunningActionInstanceStatusEnum.DONE) {
                            remainingActionsAreDone = false;
                            break;
                        }
                    }
                    if (remainingActionsAreDone) {
                        // Update the dunningLevelInstance status also to
                        dunningLevelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.DONE);
                        // Update DunningCollectionPlan : currentDunningLevelSequence / lastAction / lastActionDate / nextAction /nextActionDate
                        updateCollectionPlanActions(dunningLevelInstance);
                    } else {
                        dunningLevelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.IN_PROGRESS);
                        dunningLevelInstance.setExecutionDate(new Date());
                    }
                    dunningLevelInstanceService.update(dunningLevelInstance);
                }
            }

            if (!Objects.equals(dunningActionInstanceInput.getActionRestult(), dunningActionInstanceToUpdate.getActionRestult())) {
                fields.add("actionRestult");
                dunningActionInstanceToUpdate.setActionRestult(dunningActionInstanceInput.getActionRestult());
            }

            dunningActionInstanceService.update(dunningActionInstanceToUpdate);

            String origine = (dunningActionInstanceToUpdate.getCollectionPlan() != null) ? dunningActionInstanceToUpdate.getCollectionPlan().getCollectionPlanNumber() : "";
            auditLogService.trackOperation("UPDATE DunningActionInstance", new Date(), dunningActionInstanceToUpdate.getCollectionPlan(), origine, fields);
            return of(dunningActionInstanceToUpdate);
        } catch (MeveoApiException e) {
            throw e;
        } catch (Exception e) {
            throw new MeveoApiException(e);
        }
    }

    /**
     * Update dunning action instance execution date based on the action status
     * @param dunningActionInstanceInput Dunning action instance input {@link DunningActionInstanceInput}
     * @param dunningActionInstanceToUpdate Dunning action instance to update {@link DunningActionInstance}
     */
    private void updateDunningActionInstanceExecutionDate(DunningActionInstanceInput dunningActionInstanceInput, DunningActionInstance dunningActionInstanceToUpdate) {
        // Update dunning action instance execution date
        if (dunningActionInstanceInput.getActionStatus() == DunningActionInstanceStatusEnum.DONE) {
            dunningActionInstanceToUpdate.setExecutionDate(new Date());
        } else if (dunningActionInstanceInput.getActionStatus() == DunningActionInstanceStatusEnum.IGNORED) {
            dunningActionInstanceToUpdate.setExecutionDate(null);
        }
    }

    /**
     * Create actions
     * @param dunningLevelInstance Dunning level instance {@link DunningLevelInstance}
     * @param actionInstanceInputs List of dunning action instance input {@link DunningActionInstanceInput}
     */
    private void createActions(DunningLevelInstance dunningLevelInstance, List<DunningActionInstanceInput> actionInstanceInputs) {
        List<DunningActionInstance> actions = new ArrayList<>();

        for (DunningActionInstanceInput actionInput : actionInstanceInputs) {
            DunningActionInstance dunningActionInstance = new DunningActionInstance();
            Long dunningActionId = actionInput.getDunningAction().getId();
            DunningAction dunningAction = dunningActionService.findById(dunningActionId);
            if (dunningAction == null) {
                throw new EntityDoesNotExistsException("No Dunning action found with id : " + dunningActionId);
            }

            if (actionInput.getCode() != null) {
                dunningActionInstance.setCode(actionInput.getCode());
            } else {
                dunningActionInstance.setCode(dunningAction.getCode());
            }

            if (actionInput.getDescription() != null) {
                dunningActionInstance.setDescription(actionInput.getDescription());
            } else {
                dunningActionInstance.setDescription(dunningAction.getDescription());
            }

            if (actionInput.getActionType() != null) {
                dunningActionInstance.setActionType(actionInput.getActionType());
            } else {
                dunningActionInstance.setActionType(dunningAction.getActionType());
            }

            if (actionInput.getMode() != null) {
                dunningActionInstance.setActionMode(actionInput.getMode());
            } else {
                dunningActionInstance.setActionMode(dunningAction.getActionMode());
            }

            if (actionInput.getActionOwner() != null && actionInput.getActionOwner().getId() != null) {
                Long dunningAgentId = actionInput.getActionOwner().getId();
                DunningAgent dunningAgent = dunningAgentService.findById(dunningAgentId);
                if (dunningAgent == null) {
                    throw new EntityDoesNotExistsException("No Dunning agent found with id : " + dunningAgentId);
                }
                dunningActionInstance.setActionOwner(dunningAgent);
            } else {
                dunningActionInstance.setActionOwner(dunningAction.getAssignedTo());
            }

            dunningActionInstance.setActionRestult(actionInput.getActionRestult());
            if (dunningLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
                dunningActionInstance.setActionStatus(DunningActionInstanceStatusEnum.DONE);
                dunningLevelInstance.setExecutionDate(new Date());
            } else {
                dunningActionInstance.setActionStatus(actionInput.getActionStatus());
                updateDunningActionInstanceExecutionDate(actionInput, dunningActionInstance);
            }
            dunningActionInstance.setCollectionPlan(dunningLevelInstance.getCollectionPlan());
            dunningActionInstance.setDunningLevelInstance(dunningLevelInstance);
            dunningActionInstance.setDunningAction(dunningAction);
            dunningActionInstanceService.create(dunningActionInstance);
            actions.add(dunningActionInstance);
        }

        dunningLevelInstance.setActions(actions);
    }

    /**
     * Update collection plan actions
     * @param dunningLevelInstance Dunning level instance {@link DunningLevelInstance}
     */
    private void updateCollectionPlanActions(DunningLevelInstance dunningLevelInstance) {
        if (dunningLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
            DunningCollectionPlan collectionPlan = dunningLevelInstance.getCollectionPlan();

            if (Boolean.FALSE.equals(dunningLevelInstance.getDunningLevel().isEndOfDunningLevel())) {
                Integer currentDunningLevelSequence = collectionPlan.getCurrentDunningLevelSequence();
                if (currentDunningLevelSequence == null) {
                    currentDunningLevelSequence = 0;
                }

                collectionPlan.setCurrentDunningLevelSequence(++currentDunningLevelSequence);
                setDunningCollectionPlanLastAction(dunningLevelInstance, collectionPlan);
                DunningLevelInstance nextLevelInstance = dunningLevelInstanceService.findByCurrentLevelSequence(collectionPlan);
                String nextLevelAction = null;
                if (nextLevelInstance != null && nextLevelInstance.getActions() != null && !nextLevelInstance.getActions().isEmpty()) {
                    for (DunningActionInstance nextActionInstance : nextLevelInstance.getActions()) {
                        if (nextActionInstance.getActionMode() == ActionModeEnum.AUTOMATIC) {
                            nextLevelAction = nextActionInstance.getCode();
                            break;
                        }
                    }
                    if (nextLevelAction == null) {
                        nextLevelAction = nextLevelInstance.getActions().get(0).getCode();
                    }

                    collectionPlan.setNextAction(nextLevelAction);

                    Integer days = nextLevelInstance.getDaysOverdue();
                    if (collectionPlan.getPauseDuration() != null) {
                        days += collectionPlan.getPauseDuration();
                    }
                    collectionPlan.setNextActionDate(addDaysToDate(collectionPlan.getStartDate(), days));
                }
            } else {
                setDunningCollectionPlanLastAction(dunningLevelInstance, collectionPlan);
                collectionPlan.setStatus(dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.FAILED));
                collectionPlan.setNextAction(null);
                collectionPlan.setNextActionDate(null);

                // Ignore levels and actions after stopping dunning collection plan
                dunningCollectionPlanService.ignoreLevelsAndActionsAfterStoppingDunningCollectionPlanOrPayingInvoice(collectionPlan);
            }

            dunningCollectionPlanService.update(collectionPlan);
        }
    }

    /**
     * Set Dunning collection plan last action
     * @param dunningLevelInstance Dunning level instance {@link DunningLevelInstance}
     * @param collectionPlan Dunning collection plan {@link DunningCollectionPlan}
     */
    private static void setDunningCollectionPlanLastAction(DunningLevelInstance dunningLevelInstance, DunningCollectionPlan collectionPlan) {
        List<DunningActionInstance> lastLevelActions = dunningLevelInstance.getActions();
        if (lastLevelActions != null && !lastLevelActions.isEmpty()) {
            String levelActionCode = lastLevelActions
                                        .stream()
                                        .sorted(Comparator.comparing(a -> a.getAuditable().getLastModified(), Comparator.reverseOrder()))
                                        .findFirst()
                                        .get()
                                        .getCode();
            collectionPlan.setLastAction(levelActionCode);
            collectionPlan.setLastActionDate(new Date());
        }
    }

    private void checkDaysOverdue(DunningCollectionPlan collectionPlan, Integer newDaysOverdue) {
        // daysOverdue is already exist at one of dunningLevelInstance of the current collection Plan
        boolean daysOverdueIsAlreadyExist = dunningLevelInstanceService.checkDaysOverdueIsAlreadyExist(collectionPlan, newDaysOverdue);
        if (daysOverdueIsAlreadyExist) {
            throw new ActionForbiddenException("DaysOverdue is already exist at one of dunningLevelInstance of the current collection Plan");
        }

        DunningLevelInstance lastLevelInstance = dunningLevelInstanceService.findLastLevelInstance(collectionPlan);
        if (lastLevelInstance != null) {
            // the daysOverdue is greater than the endLevel daysOverdue of the current collectionPlan
            if (newDaysOverdue > lastLevelInstance.getDaysOverdue()) {
                throw new ActionForbiddenException("The sequence is greater than the endLevel");
            }
        }

        DunningLevelInstance currentLevelInstance = dunningLevelInstanceService.findByCurrentLevelSequence(collectionPlan);
        // the daysOverdue is less than the current dunningLevelInstance daysOverdue
        if (currentLevelInstance != null && newDaysOverdue < currentLevelInstance.getDaysOverdue()) {
            throw new ActionForbiddenException("The daysOverdue is less than the current dunningLevelInstance daysOverdue");
        }
    }
    
    private void checkPreferredMethodPayment (DunningCollectionPlan collectionPlan) {
    	// customerAccount can be directly retrieved from collectionPlan : INTRD-21493
    	BillingAccount billingAccount = billingAccountService.refreshOrRetrieve(collectionPlan.getBillingAccount());
        if (billingAccount != null && billingAccount.getCustomerAccount() != null) {
            PaymentMethod preferredPaymentMethod = customerAccountService.getPreferredPaymentMethod(billingAccount.getCustomerAccount().getId());

            if (preferredPaymentMethod == null) {
            	throw new MeveoApiException("No preferred payment method found for billing account " + billingAccount.getCode());
            }
            
            if (!(preferredPaymentMethod.getPaymentType().equals(DIRECTDEBIT) || preferredPaymentMethod.getPaymentType().equals(CARD))) {
            	throw new MeveoApiException("retryPaymentOnResumeDate can be true only if payment method is CARD or DIRECT DEBIT for collection plan " + collectionPlan.getId());
            }
        }
    }

    /**
     * Update the execution date of the dunning action instance
     * @param updatedDunningActionInstance the updated dunning action instance
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Transactional
    public void updateExecutionDate(DunningActionInstance updatedDunningActionInstance) {
        DunningActionInstanceStatusEnum actionStatus = updatedDunningActionInstance.getActionStatus();
        DunningLevelInstance dunningLevelInstance = updatedDunningActionInstance.getDunningLevelInstance();
        DunningCollectionPlan collectionPlan = dunningCollectionPlanService.findById(dunningLevelInstance.getCollectionPlan().getId());;

        if(actionStatus == DunningActionInstanceStatusEnum.DONE && dunningLevelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.DONE)) {
            updatedDunningActionInstance.setExecutionDate(new Date());
            dunningLevelInstance.setExecutionDate(new Date());
            dunningLevelInstanceService.update(dunningLevelInstance);

            collectionPlan.getDunningLevelInstances().stream()
                    .filter(dunningLevelInstance1 -> dunningLevelInstance1.getLevelStatus().equals(DunningLevelInstanceStatusEnum.TO_BE_DONE))
                    .forEach(dunningLevelInstance1 -> {
                        dunningLevelInstance1.setExecutionDate(addDaysToDate(dunningLevelInstance.getExecutionDate(), dunningLevelInstance1.getDaysOverdue() - dunningLevelInstance.getDaysOverdue()));
                        dunningLevelInstance1.getActions().forEach(dunningActionInstance -> {
                            dunningActionInstance.setExecutionDate(addDaysToDate(dunningLevelInstance.getExecutionDate(), dunningLevelInstance1.getDaysOverdue() - dunningLevelInstance.getDaysOverdue()));
                            dunningActionInstanceService.update(dunningActionInstance);
                        });
                        dunningLevelInstanceService.update(dunningLevelInstance1);
                    });
        } else if (actionStatus == DunningActionInstanceStatusEnum.IGNORED) {
            updatedDunningActionInstance.setExecutionDate(null);
            dunningLevelInstance.setExecutionDate(null);
            dunningLevelInstanceService.update(dunningLevelInstance);
        }


    }

    /**
     * Execute dunning action instance
     * @param actionInstanceId the action instance id
     * @return the map of object
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Transactional
    public Optional<DunningActionInstance> executeDunningActionInstance(Long actionInstanceId) {
        // check actionInstanceId
        if (actionInstanceId == null) {
            throw new ActionForbiddenException("Attribute actionInstanceId is mandatory");
        }

        // check if the dunningActionInstance is already exist
        DunningActionInstance dunningActionInstance = dunningActionInstanceService.findById(actionInstanceId);
        if (dunningActionInstance == null) {
            throw new EntityDoesNotExistsException("No Dunning Action Instance found with id : " + actionInstanceId);
        }

        // check if the dunningActionInstance is already done
        if (dunningActionInstance.getActionStatus() != DunningActionInstanceStatusEnum.TO_BE_DONE) {
            throw new ActionForbiddenException("Can not execute a dunningActionInstance with status different from TO_BE_DONE");
        }

        // check if the dunningActionInstance is already done
        DunningCollectionPlan collectionPlan = null;
        if (dunningActionInstance.getCollectionPlan() != null) {
            collectionPlan = dunningCollectionPlanService.findById(dunningActionInstance.getCollectionPlan().getId());

            if (collectionPlan.getStatus().getStatus().equals(DunningCollectionPlanStatusEnum.STOPPED)) {
                throw new BusinessApiException("Collection Plan with id " + collectionPlan.getId() + " cannot be edited, the current collection plan status is " + collectionPlan.getStatus().getStatus());
            }
        }

        // trigger the action to execute dunning action instance
        dunningActionInstanceService.triggerAction(dunningActionInstance, collectionPlan);

        // update the dunningActionInstance information's
        dunningActionInstance.setActionStatus(DunningActionInstanceStatusEnum.DONE);
        dunningActionInstance.setActionMode(ActionModeEnum.MANUAL);
        dunningActionInstance.setExecutionDate(new Date());
        dunningActionInstance.setActionRestult("Action executed manually");

        // update dunning level instance status if all actions are done
        DunningLevelInstance dunningLevelInstance = dunningActionInstance.getDunningLevelInstance();
        if (dunningLevelInstance.getActions().stream().allMatch(action -> action.getActionStatus() == DunningActionInstanceStatusEnum.DONE)) {
            dunningLevelInstance.setLevelStatus(DunningLevelInstanceStatusEnum.DONE);
            dunningLevelInstance.setExecutionDate(new Date());
            dunningLevelInstanceService.update(dunningLevelInstance);
        }

        // update the dunningActionInstance
        dunningActionInstanceService.update(dunningActionInstance);

        // return the dunningActionInstance
        return of(dunningActionInstance);
    }
}