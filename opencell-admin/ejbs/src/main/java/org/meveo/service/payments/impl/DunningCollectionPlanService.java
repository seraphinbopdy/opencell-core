package org.meveo.service.payments.impl;

import org.hibernate.proxy.HibernateProxy;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.dunning.*;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.payments.ActionModeEnum;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.CardPaymentMethod;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.DunningCollectionPlanStatusEnum;
import org.meveo.model.payments.PaymentGateway;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.communication.impl.EmailSender;
import org.meveo.service.communication.impl.EmailTemplateService;
import org.meveo.service.communication.impl.InternationalSettingsService;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static org.meveo.model.dunning.DunningLevelInstanceStatusEnum.*;
import static org.meveo.model.payments.PaymentMethodEnum.CARD;
import static org.meveo.model.payments.PaymentMethodEnum.DIRECTDEBIT;
import static org.meveo.model.shared.DateUtils.addDaysToDate;
import static org.meveo.model.shared.DateUtils.daysBetween;
import static org.meveo.service.base.ValueExpressionWrapper.evaluateExpression;

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

    private static final String STOP_REASON = "Changement de politique de recouvrement";

    public DunningCollectionPlan findByPolicy(DunningPolicy dunningPolicy) {
        List<DunningCollectionPlan> result = getEntityManager()
                                                .createNamedQuery("DunningCollectionPlan.findByPolicy", entityClass)
                                                .setParameter("dunningPolicy", dunningPolicy)
                                                .getResultList();
        return result != null && !result.isEmpty() ? result.get(0) : null;
    }

    public DunningCollectionPlan switchCollectionPlan(DunningCollectionPlan oldCollectionPlan, DunningPolicy policy, DunningPolicyLevel selectedPolicyLevel) {
        DunningStopReason stopReason = dunningStopReasonsService.findByStopReason(STOP_REASON);
        policy = policyService.refreshOrRetrieve(policy);
        DunningCollectionPlanStatus collectionPlanStatusStop
        = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.STOPPED);

        oldCollectionPlan.setStopReason(stopReason);
        oldCollectionPlan.setCloseDate(new Date());
        oldCollectionPlan.setStatus(collectionPlanStatusStop);

        DunningCollectionPlanStatus collectionPlanStatusActif
                = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.ACTIVE);
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
        create(newCollectionPlan);
       
        if (policy.getDunningLevels() != null && !policy.getDunningLevels().isEmpty()) {
            List<DunningLevelInstance> levelInstances = new ArrayList<>();
            for (DunningPolicyLevel policyLevel : policy.getDunningLevels()) {
                DunningLevelInstance levelInstance;
                if (policyLevel.getSequence() < selectedPolicyLevel.getSequence()) {
                    levelInstance = dunningLevelInstanceService.createDunningLevelInstanceWithCollectionPlan(newCollectionPlan, collectionPlanStatusActif, policyLevel, DONE);
                } else {
                    levelInstance = dunningLevelInstanceService.createDunningLevelInstanceWithCollectionPlan(newCollectionPlan, collectionPlanStatusActif, policyLevel, TO_BE_DONE);
                    if (policyLevel.getSequence() == selectedPolicyLevel.getSequence()) {
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
     * @return created DunningCollectionPlan
     */
    public DunningCollectionPlan createCollectionPlanFrom(Invoice invoice, DunningPolicy policy, DunningCollectionPlanStatus collectionPlanStatus) {
        invoice = invoiceService.refreshOrRetrieve(invoice);
        DunningCollectionPlan collectionPlan = new DunningCollectionPlan();
        collectionPlan.setRelatedPolicy(policy);
        collectionPlan.setBillingAccount(invoice.getBillingAccount());
        collectionPlan.setRelatedInvoice(invoice);
        collectionPlan.setCurrentDunningLevelSequence(1);
        collectionPlan.setTotalDunningLevels(policy.getTotalDunningLevels());
        collectionPlan.setStartDate(new Date());
        collectionPlan.setStatus(collectionPlanStatus);
        collectionPlan.setDaysOpen(abs((int) daysBetween(new Date(), collectionPlan.getStartDate())));
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
            if (firstDunningLevelInstance.isPresent()) {
                if (firstDunningLevelInstance.get().getLevelStatus().equals(DONE)) {
                    collectionPlan.setLastActionDate(addDaysToDate(collectionPlan.getStartDate(), firstDunningLevelInstance.get().getDaysOverdue()));
                    collectionPlan.setLastAction(firstDunningLevelInstance.get().getActions().get(0).getDunningAction().getActionType().toString());
                    collectionPlan.setCurrentDunningLevelSequence(collectionPlan.getCurrentDunningLevelSequence() + 1);

                    if (nextDunningLevelInstance.isPresent()) {
                        collectionPlan.setNextAction(nextDunningLevelInstance.get().getActions().get(0).getDunningAction().getActionType().toString());
                        collectionPlan.setNextActionDate(addDaysToDate(collectionPlan.getStartDate(), nextDunningLevelInstance.get().getDaysOverdue()));
                    }
                } else if (firstDunningLevelInstance.get().getLevelStatus().equals(IGNORED)) {
                    collectionPlan.setLastActionDate(null);
                    collectionPlan.setLastAction(null);
                    collectionPlan.setCurrentDunningLevelSequence(collectionPlan.getCurrentDunningLevelSequence() + 1);

                    if (nextDunningLevelInstance.isPresent()) {
                        collectionPlan.setNextAction(nextDunningLevelInstance.get().getActions().get(0).getDunningAction().getActionType().toString());
                        collectionPlan.setNextActionDate(addDaysToDate(collectionPlan.getStartDate(), nextDunningLevelInstance.get().getDaysOverdue()));
                    }
                }
            }
        }

        return update(collectionPlan);
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
    
    public DunningCollectionPlan pauseCollectionPlan(boolean forcePause, Date pauseUntil,
			DunningCollectionPlan collectionPlanToPause, DunningPauseReason dunningPauseReason, boolean retryPaymentOnResumeDate) {
    	collectionPlanToPause = dunningCollectionPlanService.refreshOrRetrieve(collectionPlanToPause);
		collectionPlanToPause = refreshLevelInstances(collectionPlanToPause);
		DunningCollectionPlanStatus dunningCollectionPlanStatus = dunningCollectionPlanStatusService.refreshOrRetrieve(collectionPlanToPause.getStatus());
		if(!dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.ACTIVE)) {
			throw new BusinessApiException("Collection Plan with id "+collectionPlanToPause.getId()+" cannot be paused, the collection plan status is not active");
		}

		if(dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.STOPPED)) {
			throw new BusinessApiException("Collection Plan with id "+collectionPlanToPause.getId()+" cannot be paused, the collection plan status is not stoped");
		}

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
		return collectionPlanToPause; 
	}
	
	public DunningCollectionPlan stopCollectionPlan(DunningCollectionPlan collectionPlanToStop, DunningStopReason dunningStopReason) {
		collectionPlanToStop = dunningCollectionPlanService.refreshOrRetrieve(collectionPlanToStop);
		collectionPlanToStop = refreshLevelInstances(collectionPlanToStop);

		DunningCollectionPlanStatus dunningCollectionPlanStatus = dunningCollectionPlanStatusService.refreshOrRetrieve(collectionPlanToStop.getStatus());

		if(dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.SUCCESS)) {
			throw new BusinessApiException("Collection Plan with id "+collectionPlanToStop.getId()+" cannot be stopped, the collection plan status is success");
		}
		if(dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.FAILED)) {
			throw new BusinessApiException("Collection Plan with id "+collectionPlanToStop.getId()+" cannot be stopped, the collection plan status is failed");
		}
		
		DunningCollectionPlanStatus collectionPlanStatus = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.STOPPED);
		collectionPlanToStop.setStatus(collectionPlanStatus);
		collectionPlanToStop.setCloseDate(new Date());
		collectionPlanToStop.setDaysOpen((int) daysBetween(collectionPlanToStop.getCloseDate(), new Date()) + 1);
		collectionPlanToStop.setStopReason(dunningStopReason);
		update(collectionPlanToStop);
		return collectionPlanToStop; 
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

    	if (validate) {
			if(!dunningCollectionPlanStatus.getStatus().equals(DunningCollectionPlanStatusEnum.PAUSED)) {
				throw new BusinessApiException("Collection Plan with id "+collectionPlanToResume.getId()+" cannot be resumed, the collection plan is not paused");
			}
			if(collectionPlanToResume.getPausedUntilDate() != null && collectionPlanToResume.getPausedUntilDate().before(new Date())) {
				throw new BusinessApiException("Collection Plan with id "+collectionPlanToResume.getId()+" cannot be resumed, the field pause until is in the past");
			}
		}
		
		Optional<DunningLevelInstance> dunningLevelInstance = collectionPlanToResume.getDunningLevelInstances()
				.stream().max(Comparator.comparing(DunningLevelInstance::getId));
		if(dunningLevelInstance.isEmpty()) {
			throw new BusinessApiException("No dunning level instances found for the collection plan with id "+collectionPlanToResume.getId());
		}
		DunningCollectionPlanStatus collectionPlanStatus=null;
		if(collectionPlanToResume.getPausedUntilDate() != null && collectionPlanToResume.getPausedUntilDate().after(DateUtils.addDaysToDate(collectionPlanToResume.getStartDate(), dunningLevelInstance.get().getDaysOverdue()))) {
			collectionPlanStatus = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.FAILED);
            collectionPlanToResume.setDaysOpen((int) daysBetween(collectionPlanToResume.getCloseDate(), new Date()) + 1);
		} else {
			collectionPlanStatus = dunningCollectionPlanStatusService.findByStatus(DunningCollectionPlanStatusEnum.ACTIVE);
			collectionPlanToResume.setPauseReason(null);
		}
		collectionPlanToResume.setStatus(collectionPlanStatus);
		collectionPlanToResume.addPauseDuration((int) daysBetween(collectionPlanToResume.getPausedUntilDate(), new Date()));
        collectionPlanToResume.setNextActionDate(addDaysToDate(collectionPlanToResume.getNextActionDate(), (int) daysBetween(collectionPlanToResume.getPausedUntilDate(), new Date())));
        update(collectionPlanToResume);
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

	public List<Long> getActiveCollectionPlansIds() {
        return getEntityManager()
                .createNamedQuery("DunningCollectionPlan.activeCollectionPlansIds", Long.class)
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
     * @param collectionPlan
     */
    public void launchPaymentAction(DunningCollectionPlan collectionPlan) {
        BillingAccount billingAccount = collectionPlan.getBillingAccount();
        if (billingAccount != null && billingAccount.getCustomerAccount() != null
                && billingAccount.getCustomerAccount().getPaymentMethods() != null) {
            PaymentMethod preferredPaymentMethod = billingAccount.getCustomerAccount()
                    .getPaymentMethods()
                    .stream()
                    .filter(PaymentMethod::isPreferred)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("No preferred payment method found for customer account"
                            + billingAccount.getCustomerAccount().getCode()));
            CustomerAccount customerAccount = billingAccount.getCustomerAccount();
            //PaymentService.doPayment consider amount to pay in cent so amount should be * 100
            long amountToPay = collectionPlan.getRelatedInvoice().getNetToPay().multiply(BigDecimal.valueOf(100)).longValue();
            Invoice invoice = collectionPlan.getRelatedInvoice();
            if (invoice.getRecordedInvoice() == null) {
                throw new BusinessException("No getRecordedInvoice for the invoice "
                        + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getTemporaryInvoiceNumber()));
            }
            List<Long> ids = new ArrayList<>();
            ids.add(invoice.getRecordedInvoice().getId());
            PaymentGateway paymentGateway = paymentGatewayService.getPaymentGateway(customerAccount, preferredPaymentMethod, null);
            doPayment(preferredPaymentMethod, customerAccount, amountToPay, ids, paymentGateway);
        }
    }

    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void doPayment(PaymentMethod preferredPaymentMethod, CustomerAccount customerAccount, long amountToPay, List<Long> accountOperationsToPayIds, PaymentGateway paymentGateway) {
        if (preferredPaymentMethod.getPaymentType().equals(DIRECTDEBIT) || preferredPaymentMethod.getPaymentType().equals(CARD)) {
            try {
                if (accountOperationsToPayIds != null && !accountOperationsToPayIds.isEmpty()) {
                    if (preferredPaymentMethod.getPaymentType().equals(CARD)) {
                        if (preferredPaymentMethod instanceof HibernateProxy) {
                            preferredPaymentMethod = (PaymentMethod) ((HibernateProxy) preferredPaymentMethod).getHibernateLazyInitializer().getImplementation();
                        }
                        CardPaymentMethod paymentMethod = (CardPaymentMethod) preferredPaymentMethod;
                        paymentService.doPayment(customerAccount, amountToPay, accountOperationsToPayIds,
                                true, true, paymentGateway, paymentMethod.getCardNumber(),
                                paymentMethod.getCardNumber(), paymentMethod.getHiddenCardNumber(),
                                paymentMethod.getExpirationMonthAndYear(), paymentMethod.getCardType(),
                                true, preferredPaymentMethod.getPaymentType());
                    } else {
                        paymentService.doPayment(customerAccount, amountToPay, accountOperationsToPayIds,
                                true, true, paymentGateway, null, null,
                                null, null, null, true, preferredPaymentMethod.getPaymentType());
                    }
                }
            } catch (Exception exception) {
                throw new BusinessException("Error occurred during payment process for customer " + customerAccount.getCode(), exception);
            }
        }
    }
}