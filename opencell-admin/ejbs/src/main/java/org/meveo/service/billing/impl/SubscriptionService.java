/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */
package org.meveo.service.billing.impl;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.meveo.apiv2.accounts.ApplyOneShotChargeListModeEnum.PROCESS_ALL;
import static org.meveo.apiv2.accounts.ApplyOneShotChargeListModeEnum.ROLLBACK_ON_ERROR;
import static org.meveo.model.billing.SubscriptionStatusEnum.ACTIVE;
import static org.meveo.model.billing.SubscriptionStatusEnum.CANCELED;
import static org.meveo.model.billing.SubscriptionStatusEnum.CLOSED;
import static org.meveo.model.billing.SubscriptionStatusEnum.RESILIATED;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ElementNotResiliatedOrCanceledException;
import org.meveo.admin.exception.IncorrectServiceInstanceException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.admin.exception.RatingException;
import org.meveo.admin.exception.ValidationException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.account.ApplyOneShotChargeInstanceRequestDto;
import org.meveo.api.dto.billing.AttributeInstanceDto;
import org.meveo.api.dto.billing.WalletOperationDto;
import  org.meveo.api.dto.response.PagingAndFiltering.SortOrder;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.apiv2.accounts.AppliedChargeResponseDto;
import org.meveo.apiv2.accounts.ApplyOneShotChargeListInput;
import org.meveo.apiv2.accounts.ApplyOneShotChargeListModeEnum;
import org.meveo.apiv2.accounts.ProcessApplyChargeListResult;
import org.meveo.audit.logging.annotations.MeveoAudit;
import org.meveo.commons.utils.ListUtils;
import org.meveo.commons.utils.MethodCallingUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.PersistenceUtils;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.RatingResult;
import org.meveo.model.billing.AttributeInstance;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingCycle;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.ChargeApplicationModeEnum;
import org.meveo.model.billing.DiscountPlanInstance;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.OneShotChargeInstance;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.Renewal;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionRenewal;
import org.meveo.model.billing.SubscriptionRenewal.RenewalPeriodUnitEnum;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.billing.SubscriptionTerminationReason;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.catalog.DiscountPlanItem;
import org.meveo.model.catalog.OfferServiceTemplate;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.OneShotChargeTemplateTypeEnum;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.WalletTemplate;
import org.meveo.model.cpq.Product;
import org.meveo.model.cpq.ProductVersion;
import org.meveo.model.cpq.commercial.CommercialOrder;
import org.meveo.model.cpq.offer.OfferComponent;
import org.meveo.model.crm.Customer;
import org.meveo.model.mediation.Access;
import org.meveo.model.order.OrderItemActionEnum;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.MatchingStatusEnum;
import org.meveo.model.payments.OperationCategoryEnum;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.BusinessService;
import org.meveo.service.catalog.impl.CalendarService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.meveo.service.catalog.impl.OneShotChargeTemplateService;
import org.meveo.service.cpq.AttributeService;
import org.meveo.service.cpq.ProductService;
import org.meveo.service.cpq.order.CommercialOrderService;
import org.meveo.service.crm.impl.SubscriptionActivationException;
import org.meveo.service.medina.impl.AccessService;
import org.meveo.service.order.OrderHistoryService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.PaymentMethodService;
import org.meveo.service.script.offer.OfferModelScriptService;
import org.meveo.service.securityDeposit.impl.FinanceSettingsService;
import jakarta.enterprise.event.Event;
import org.meveo.event.qualifier.StatusUpdated;


import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;


/**
 * @author Edward P. Legaspi
 * @author khalid HORRI
 * @author Mounir BAHIJE
 * @author Abdellatif BARI
 * @lastModifiedVersion 7.0
 */
@Stateless
public class SubscriptionService extends BusinessService<Subscription> {

    @Inject
    private OfferModelScriptService offerModelScriptService;

    @EJB
    private ServiceInstanceService serviceInstanceService;

    @Inject
    private AccessService accessService;

    @Inject
    private OrderHistoryService orderHistoryService;

    @Inject
    private OfferTemplateService offerTemplateService;

    @Inject
    private DiscountPlanInstanceService discountPlanInstanceService;

    @Inject
    private OneShotChargeInstanceService oneShotChargeInstanceService;

    @Inject
    private PaymentMethodService paymentMethodService;
    
    @Inject
    private CustomerAccountService customerAccountService;

    @Inject
    private FinanceSettingsService financeSettingsService;
    @Inject
    private AttributeService attributeService;
    @Inject
    private ProductService productService;
    @Inject
    private RatedTransactionService ratedTransactionService;
    @Inject
    private OneShotChargeTemplateService oneShotChargeTemplateService;
    @Inject
    private CommercialOrderService commercialOrderService;
    @Inject
    private WalletTemplateService walletTemplateService;
    @Resource(lookup = "java:jboss/ee/concurrency/executor/job_executor")
    protected ManagedExecutorService executor;
    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private MethodCallingUtils methodCallingUtils;
    @Inject
    private BillingAccountService billingAccountService;
    
    @Inject
    @StatusUpdated
    protected Event<Subscription> subscriptionStatusUpdatedEvent;

    @MeveoAudit
    @Override
    public void create(Subscription subscription) throws BusinessException {
    	
        OfferTemplate offerTemplate = offerTemplateService.refreshOrRetrieve(subscription.getOffer());
        if (offerTemplate.isDisabled() && subscription.getOrder() == null) {
			throw new BusinessException(String.format("OfferTemplate[code=%s] is disabled and cannot be subscribed to. Please select another offer.", offerTemplate.getCode()));
		}
        List<PaymentMethod> paymentMethods =
                paymentMethodService.listByCustomerAccount(subscription.getUserAccount().getBillingAccount().getCustomerAccount(), null, null);
        checkSubscriptionPaymentMethod(subscription, paymentMethods);
        updateSubscribedTillAndRenewalNotifyDates(subscription);

        subscription.createAutoRenewDate();
        subscription.setVersionNumber(1);
        super.create(subscription);
        
        // execute subscription script
        if (offerTemplate.getBusinessOfferModel() != null && offerTemplate.getBusinessOfferModel().getScript() != null) {
            try {
                offerModelScriptService.subscribe(subscription, offerTemplate.getBusinessOfferModel().getScript().getCode());
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", offerTemplate.getBusinessOfferModel().getScript().getCode(), e);
            }
        }
        checkAndApplyDiscount(offerTemplate, subscription);
    }
    
    private void checkAndApplyDiscount(OfferTemplate offerTemplate, Subscription subscription) {
        if(offerTemplate != null ) {
            if(CollectionUtils.isNotEmpty(offerTemplate.getAllowedDiscountPlans())) {
                offerTemplate.getAllowedDiscountPlans().stream()
                                            .filter(DiscountPlan::isAutomaticApplication)
                                            .forEach(dp -> instantiateDiscountPlan(subscription, dp));
                                            
            }
        }
    }

    @MeveoAudit
    @Override
    public void createWithoutNotif(Subscription subscription) throws BusinessException {
    	
        OfferTemplate offerTemplate = offerTemplateService.refreshOrRetrieve(subscription.getOffer());
        if (offerTemplate.isDisabled() && subscription.getOrder() == null) {
			throw new BusinessException(String.format("OfferTemplate[code=%s] is disabled and cannot be subscribed to. Please select another offer.", offerTemplate.getCode()));
		}
        checkSubscriptionPaymentMethod(subscription, subscription.getUserAccount().getBillingAccount().getCustomerAccount().getPaymentMethods());
        updateSubscribedTillAndRenewalNotifyDates(subscription);

        subscription.createAutoRenewDate();
        subscription.setVersionNumber(1);
        super.createWithoutNotif(subscription);
        
        // execute subscription script
        if (offerTemplate.getBusinessOfferModel() != null && offerTemplate.getBusinessOfferModel().getScript() != null) {
            try {
                offerModelScriptService.subscribe(subscription, offerTemplate.getBusinessOfferModel().getScript().getCode());
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", offerTemplate.getBusinessOfferModel().getScript().getCode(), e);
            }
        }
        checkAndApplyDiscount(offerTemplate, subscription);
    }

    @MeveoAudit
    @Override
    public Subscription update(Subscription subscription) throws BusinessException {
    	Subscription subscriptionOld = this.findById(subscription.getId());
    	OfferTemplate offerTemplate = offerTemplateService.retrieveIfNotManaged(subscription.getOffer());
        if (offerTemplate.isDisabled() && subscription.getOrder() == null) {
            throw new BusinessException(String.format("OfferTemplate[code=%s] is disabled and cannot be subscribed to. Please select another offer.", offerTemplate.getCode()));
        }
    	CustomerAccount customerAccount = customerAccountService.refreshOrRetrieve(subscription.getUserAccount().getBillingAccount().getCustomerAccount());
        checkSubscriptionPaymentMethod(subscription, customerAccount.getPaymentMethods());
        updateSubscribedTillAndRenewalNotifyDates(subscription);
       
        subscription.updateAutoRenewDate(subscriptionOld);

        return super.update(subscription);
    }

    private void checkSubscriptionPaymentMethod(Subscription subscription, List<PaymentMethod> paymentMethods) {
        if (Objects.nonNull(subscription.getPaymentMethod()) && (paymentMethods.isEmpty() || paymentMethods.stream()
                .filter(PaymentMethod::isActive)
                .noneMatch(paymentMethod -> paymentMethod.getId().equals(subscription.getPaymentMethod().getId())))) {
            log.error("the payment method should be reference to an active PaymentMethod defined on the CustomerAccount");
            throw new BusinessException("the payment method should be reference to an active PaymentMethod defined on the CustomerAccount");
        }
    }

    @MeveoAudit
    public Subscription subscriptionCancellation(Subscription subscription, Date cancelationDate) throws IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {
        if (cancelationDate == null) {
            cancelationDate = new Date();
        }
        /*
         * List<ServiceInstance> serviceInstances = subscription .getServiceInstances(); for (ServiceInstance serviceInstance : serviceInstances) { if
         * (InstanceStatusEnum.ACTIVE.equals(serviceInstance.getStatus())) { serviceInstanceService.serviceCancellation(serviceInstance, terminationDate); } }
         */
        subscription.setTerminationDate(cancelationDate);
        subscription.setStatus(CANCELED);
        subscription = update(subscription);
        subscriptionStatusUpdatedEvent.fire(subscription);

        return subscription;
    }

    @MeveoAudit
    public Subscription subscriptionSuspension(Subscription subscription, Date suspensionDate) throws IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {
        if (suspensionDate == null) {
            suspensionDate = new Date();
        }

        OfferTemplate offerTemplate = offerTemplateService.refreshOrRetrieve(subscription.getOffer());
        if (offerTemplate.getBusinessOfferModel() != null && offerTemplate.getBusinessOfferModel().getScript() != null) {
            try {
                offerModelScriptService.suspendSubscription(subscription, offerTemplate.getBusinessOfferModel().getScript().getCode(), suspensionDate);
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", offerTemplate.getBusinessOfferModel().getScript().getCode(), e);
            }
        }

        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        for (ServiceInstance serviceInstance : serviceInstances) {
            if (InstanceStatusEnum.ACTIVE.equals(serviceInstance.getStatus())) {
                serviceInstanceService.serviceSuspension(serviceInstance, suspensionDate);
            }
        }

        subscription.setTerminationDate(suspensionDate);
        subscription.setStatus(SubscriptionStatusEnum.SUSPENDED);
        subscription = update(subscription);
        subscriptionStatusUpdatedEvent.fire(subscription);
        for (Access access : subscription.getAccessPoints()) {
            accessService.disable(access);
        }

        return subscription;
    }

    @MeveoAudit
    public Subscription subscriptionReactivation(Subscription subscription, Date reactivationDate)
            throws IncorrectSusbcriptionException, ElementNotResiliatedOrCanceledException, IncorrectServiceInstanceException, BusinessException {

        if (reactivationDate == null) {
            reactivationDate = new Date();
        }

        if (subscription.getStatus() != SubscriptionStatusEnum.RESILIATED && subscription.getStatus() != CANCELED && subscription.getStatus() != SubscriptionStatusEnum.SUSPENDED) {
            throw new ElementNotResiliatedOrCanceledException("subscription", subscription.getCode());
        }

        subscription.setTerminationDate(null);
        subscription.setSubscriptionTerminationReason(null);
        subscription.setStatus(ACTIVE);

        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        for (ServiceInstance serviceInstance : serviceInstances) {
            if (InstanceStatusEnum.SUSPENDED.equals(serviceInstance.getStatus())) {
                serviceInstanceService.serviceReactivation(serviceInstance, reactivationDate, true, false);
            }
        }

        subscription = update(subscription);
        subscriptionStatusUpdatedEvent.fire(subscription);

        for (Access access : subscription.getAccessPoints()) {
            accessService.enable(access);
        }

        OfferTemplate offerTemplate = offerTemplateService.refreshOrRetrieve(subscription.getOffer());
        if (offerTemplate.getBusinessOfferModel() != null && offerTemplate.getBusinessOfferModel().getScript() != null) {
            try {
                offerModelScriptService.reactivateSubscription(subscription, offerTemplate.getBusinessOfferModel().getScript().getCode(), reactivationDate);
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", offerTemplate.getBusinessOfferModel().getScript().getCode(), e);
            }
        }

        return subscription;
    }

    /**
     * Terminate subscription. If termination date is not provided, a current date will be used. If termination date is a future date, subscription's subscriptionRenewal will be
     * updated with a termination date and a reason.
     *
     * @param subscription Subscription to terminate
     * @param terminationDate Termination date
     * @param terminationReason Termination reason
     * @param orderNumber Order number that requested subscription termination
     * @return Updated subscription entity
     * @throws BusinessException General business exception
     */
    @MeveoAudit
    public Subscription terminateSubscription(Subscription subscription, Date terminationDate, SubscriptionTerminationReason terminationReason, String orderNumber) throws BusinessException {
        return terminateSubscription(subscription, terminationDate, terminationReason, orderNumber, null, null);
    }

    /**
     * Terminate subscription. If termination date is not provided, a current date will be used. If termination date is a future date, subscription's subscriptionRenewal will be
     * updated with a termination date and a reason.
     *
     * @param subscription Subscription to terminate
     * @param terminationDate Termination date
     * @param terminationReason Termination reason
     * @param orderNumber Order number that requested subscription termination
     * @param orderItemId Order item's identifier in the order that requested subscription termination
     * @param orderItemAction Order item's action that requested subscription termination
     * @return Updated subscription entity
     * @throws BusinessException General business exception
     */
    @MeveoAudit
    public Subscription terminateSubscription(Subscription subscription, Date terminationDate, SubscriptionTerminationReason terminationReason, String orderNumber, Long orderItemId, OrderItemActionEnum orderItemAction)
            throws BusinessException {

        if (subscription.getStatus()==SubscriptionStatusEnum.RESILIATED || subscription.getStatus()== CANCELED ||subscription.getStatus()== CLOSED ) {
            return subscription;
        }
        
        if (terminationDate == null) {
            terminationDate = new Date();
        }
        // point termination date to the end of the day
        Date terminationDateTime = DateUtils.setDateToEndOfDay(terminationDate);

        if (terminationReason == null) {
            throw new ValidationException("Termination reason not provided", "subscription.error.noTerminationReason");

        } else if (subscription.getSubscriptionDate().after(terminationDateTime)) {
            throw new ValidationException("Termination date can not be before the subscription date", "subscription.error.terminationDateBeforeSubscriptionDate");
        }

        log.info("Terminating subscription {} for {} with reason {}", subscription.getId(), terminationDate, terminationReason);

        // checks if termination date is > now (do not ignore time, as subscription time is time sensitive)
        Date now = new Date();
        if (terminationDateTime.compareTo(now) <= 0) {
            return terminateSubscriptionWithPastDate(subscription, terminationDate, terminationReason, orderNumber, orderItemId, orderItemAction);
        } else {
            // if future date/time set subscription termination
            return terminateSubscriptionWithFutureDate(subscription, terminationDate, terminationReason);
        }
    }

    private Subscription terminateSubscriptionWithFutureDate(Subscription subscription, Date terminationDate, SubscriptionTerminationReason terminationReason) throws BusinessException {

        SubscriptionRenewal subscriptionRenewal = subscription.getSubscriptionRenewal();
        subscriptionRenewal.setTerminationReason(PersistenceUtils.initializeAndUnproxy(subscriptionRenewal.getTerminationReason()));
        Renewal renewal = new Renewal(subscriptionRenewal, subscription.getSubscribedTillDate());
        subscription.setInitialSubscriptionRenewal(JacksonUtil.toString(renewal));

        subscription.setSubscribedTillDate(terminationDate);
        subscription.setToValidity(terminationDate);
        subscriptionRenewal.setTerminationReason(terminationReason);
        subscriptionRenewal.setInitialTermType(SubscriptionRenewal.InitialTermTypeEnum.FIXED);
        subscriptionRenewal.setAutoRenew(false);
        subscriptionRenewal.setEndOfTermAction(SubscriptionRenewal.EndOfTermActionEnum.TERMINATE);

        subscription = update(subscription);

        return subscription;
    }

    @MeveoAudit
    private Subscription terminateSubscriptionWithPastDate(Subscription subscription, Date terminationDate, SubscriptionTerminationReason terminationReason, String orderNumber, Long orderItemId,
                                                           OrderItemActionEnum orderItemAction) throws BusinessException {

        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        for (ServiceInstance serviceInstance : serviceInstances) {
            if (InstanceStatusEnum.ACTIVE.equals(serviceInstance.getStatus()) || InstanceStatusEnum.SUSPENDED.equals(serviceInstance.getStatus())) {
                serviceInstanceService.terminateService(serviceInstance, terminationDate, terminationReason, orderNumber);
                // INTRD-5666: for services with subscription dates in futurs, they should be passed to terminated
                // immediately since the whole sub is terminated
                if (serviceInstance.getStatus() != InstanceStatusEnum.TERMINATED) {
                    serviceInstance.setStatus(InstanceStatusEnum.TERMINATED);
                    serviceInstanceService.update(serviceInstance);
                }

                orderHistoryService.create(orderNumber, orderItemId, serviceInstance, orderItemAction);
            }
        }
        // Apply oneshot charge of type=Other refunding
        if (terminationReason.isReimburseOneshots()) {
            List<OneShotChargeInstance> oneShotChargeInstances = oneShotChargeInstanceService.findOneShotChargeInstancesBySubscriptionId(subscription.getId());
            for (OneShotChargeInstance oneShotChargeInstance : oneShotChargeInstances) {
                if (oneShotChargeInstance.getChargeDate() != null && terminationDate.compareTo(oneShotChargeInstance.getChargeDate()) <= 0) {
                    OneShotChargeTemplate chargeTemplate = (OneShotChargeTemplate) PersistenceUtils.initializeAndUnproxy(oneShotChargeInstance.getChargeTemplate());
                    if (chargeTemplate == null || chargeTemplate.getOneShotChargeTemplateType() == null || !chargeTemplate.getOneShotChargeTemplateType().equals(OneShotChargeTemplateTypeEnum.OTHER)) {
                        continue;
                    }
                    log.info("Reimbursing the OTHER type subscription charge {}", oneShotChargeInstance.getId());
                    oneShotChargeInstanceService.applyOneShotCharge(oneShotChargeInstance, terminationDate, oneShotChargeInstance.getQuantity().negate(), orderNumber, ChargeApplicationModeEnum.REIMBURSMENT);
                    oneShotChargeInstance.setStatus(InstanceStatusEnum.TERMINATED);
                    oneShotChargeInstanceService.update(oneShotChargeInstance);
                }

            }

        }

        subscription.setSubscriptionTerminationReason(terminationReason);
        subscription.setTerminationDate(terminationDate);
        subscription.setToValidity(terminationDate);
        subscription.setStatus(SubscriptionStatusEnum.RESILIATED);
        subscription = update(subscription);
        subscriptionStatusUpdatedEvent.fire(subscription);

        for (Access access : subscription.getAccessPoints()) {
            access.setEndDate(terminationDate);
            accessService.update(access);
        }

        // execute termination script
        OfferTemplate offerTemplate = offerTemplateService.refreshOrRetrieve(subscription.getOffer());
        if (offerTemplate.getBusinessOfferModel() != null && offerTemplate.getBusinessOfferModel().getScript() != null) {
            offerModelScriptService.terminateSubscription(subscription, offerTemplate.getBusinessOfferModel().getScript().getCode(), terminationDate, terminationReason);
        }

        return subscription;
    }

    public boolean hasSubscriptions(OfferTemplate offerTemplate) {
        try {
            QueryBuilder qb = new QueryBuilder(Subscription.class, "s");
            qb.addCriterionEntity("offer", offerTemplate);

            return ((Long) qb.getCountQuery(getEntityManager()).getSingleResult()).longValue() > 0;
        } catch (NoResultException e) {
            return false;
        }
    }

    public List<Subscription> listByUserAccount(UserAccount userAccount) {
        return listByUserAccount(userAccount, "code", SortOrder.ASCENDING);
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> listByUserAccount(UserAccount userAccount, String sortBy, SortOrder sortOrder) {
        QueryBuilder qb = new QueryBuilder(Subscription.class, "c");
        qb.addCriterionEntity("userAccount", userAccount);
        boolean ascending = true;
        if (sortOrder != null) {
            ascending = sortOrder.equals(SortOrder.ASCENDING);
        }
        qb.addOrderCriterion(sortBy, ascending);

        try {
            return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
        } catch (NoResultException e) {
            log.warn("error while getting list subscription by user account", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> listByCustomer(Customer customer) {
        try {
            return getEntityManager().createNamedQuery("Subscription.listByCustomer").setParameter("customer", customer).getResultList();
        } catch (NoResultException e) {
            log.warn("error while getting list subscription by customer", e);
            return null;
        }
    }

    /**
     * Get a list of subscription ids that are about to expire or have expired already
     *
     * @return A list of subscription ids
     */
    public List<Long> getSubscriptionsToRenewOrNotify() {

        return getSubscriptionsToRenewOrNotify(new Date());
    }

    /**
     * Get a list of subscription ids that are about to expire or have expired already
     *
     * @param untillDate the subscription till date
     * @return A list of subscription ids
     */
    public List<Long> getSubscriptionsToRenewOrNotify(Date untillDate) {

        List<Long> ids = getEntityManager().createNamedQuery("Subscription.getExpired", Long.class).setParameter("date", untillDate)
                .setParameter("statuses", Arrays.asList(ACTIVE, SubscriptionStatusEnum.CREATED, SubscriptionStatusEnum.WAITING_MANDATORY, SubscriptionStatusEnum.SUSPENDED)).getResultList();
        ids.addAll(getEntityManager().createNamedQuery("Subscription.getToNotifyExpiration", Long.class).setParameter("date", untillDate)
                .setParameter("statuses", Arrays.asList(ACTIVE, SubscriptionStatusEnum.CREATED, SubscriptionStatusEnum.WAITING_MANDATORY)).getResultList());

        return ids;
    }

    @SuppressWarnings("unchecked")
    public List<ServiceInstance> listBySubscription(Subscription subscription) {
        QueryBuilder qb = new QueryBuilder(ServiceInstance.class, "c");
        qb.addCriterionEntity("subscription", subscription);

        try {
            return (List<ServiceInstance>) qb.getQuery(getEntityManager()).getResultList();
        } catch (NoResultException e) {
            log.warn("error while getting user account list by billing account", e);
            return null;
        }
    }

    public RatingResult activateInstantiatedService(Subscription sub) throws BusinessException {
        // using a new ArrayList (cloning the original one) to avoid ConcurrentModificationException
    	RatingResult ratingResult = new RatingResult();
    	Set<DiscountPlanItem> fixedDiscountItems = new HashSet<>();
        if(CANCELED == sub.getStatus() || CLOSED == sub.getStatus() || RESILIATED == sub.getStatus()) {
            throw new IncorrectServiceInstanceException("The subscription status is " + sub.getStatus());
        }
        for (ServiceInstance si : new ArrayList<>(emptyIfNull(sub.getServiceInstances()))) {
            if (si.getStatus().equals(InstanceStatusEnum.INACTIVE)) {
            	ratingResult = serviceInstanceService.serviceActivation(si);
            	if(ratingResult != null && !ratingResult.getEligibleFixedDiscountItems().isEmpty())
            		fixedDiscountItems.addAll(ratingResult.getEligibleFixedDiscountItems());
            }
        }
        boolean emptySubscriptionActivationEnabled =
                financeSettingsService.getFinanceSetting().isEnableEmptySubscriptionActivation();
        if((sub.getServiceInstances() == null || sub.getServiceInstances().isEmpty())
                && !emptySubscriptionActivationEnabled) {
            throw new SubscriptionActivationException("EMPTY_SUB_NOT_ENABLED",
                    "Allow empty subscription activation option is set to false, subscription cannot be activated");
        }
        if((sub.getServiceInstances() == null || sub.getServiceInstances().isEmpty())
                && emptySubscriptionActivationEnabled) {
            boolean mandatoryProducts = sub.getOffer()
                    .getOfferComponents()
                    .stream()
                    .anyMatch(OfferComponent::isMandatory);
            if(mandatoryProducts) {
                throw new SubscriptionActivationException("MANDATORY_PRODUCTS_CHECK",
                        "The subscription cannot be activated as its offer includes mandatory products.");
            } else {
                sub.setStatus(ACTIVE);
                update(sub);
                subscriptionStatusUpdatedEvent.fire(sub);
            }
        }
        return ratingResult;
    }

    /**
     * Return all subscriptions with status not equal to CREATED or ACTIVE and initialAgreement date more than n years old
     *
     * @param nYear age of the subscription
     * @return Filtered list of subscriptions
     */
    @SuppressWarnings("unchecked")
    public List<Subscription> listInactiveSubscriptions(int nYear) {
        QueryBuilder qb = new QueryBuilder(Subscription.class, "e");
        Date higherBound = DateUtils.addYearsToDate(new Date(), -1 * nYear);

        qb.addCriterionDateRangeToTruncatedToDay("subscriptionDate", higherBound, true, false);
        qb.addCriterionEnum("status", SubscriptionStatusEnum.CREATED, "<>", false);
        qb.addCriterionEnum("status", ACTIVE, "<>", false);

        return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
    }

    public void cancelSubscriptionRenewal(Subscription entity) throws BusinessException {
        entity.getSubscriptionRenewal().setAutoRenew(false);
    }

    /**
     * Subscription balance due.
     *
     * @param subscription the Subscription
     * @param to the to
     * @return the big decimal
     * @throws BusinessException the business exception
     */
    public BigDecimal subscriptionBalanceDue(Subscription subscription, Date to) throws BusinessException {
        return computeBalance(subscription, to, true, MatchingStatusEnum.O, MatchingStatusEnum.P, MatchingStatusEnum.I);
    }

    /**
     * Subscription balance exigible without litigation.
     *
     * @param subscription the Subscription
     * @param to the to
     * @return the big decimal
     * @throws BusinessException the business exception
     */
    public BigDecimal subscriptionBalanceExigibleWithoutLitigation(Subscription subscription, Date to) throws BusinessException {
        return computeBalance(subscription, to, true, MatchingStatusEnum.O, MatchingStatusEnum.P);
    }

    /**
     * Compute balance.
     *
     * @param subscription the Subscription
     * @param to the to
     * @param isDue the is due
     * @param status the status
     * @return the big decimal
     * @throws BusinessException the business exception
     */
    private BigDecimal computeBalance(Subscription subscription, Date to, boolean isDue, MatchingStatusEnum... status) throws BusinessException {
        return computeBalance(subscription, to, false, isDue, status);
    }

    /**
     * Computes a balance given a subscription. to and isDue parameters are ignored when isFuture is true.
     *
     * @param subscription of the customer
     * @param to compare the invoice due or transaction date here
     * @param isFuture includes the future due or transaction date
     * @param isDue if true filter via dueDate else transactionDate
     * @param status can be a list of MatchingStatusEnum
     * @return the computed balance
     * @throws BusinessException when an error in computation is encoutered
     */
    private BigDecimal computeBalance(Subscription subscription, Date to, boolean isFuture, boolean isDue, MatchingStatusEnum... status) throws BusinessException {
        log.trace("start computeBalance subscription:{}, toDate:{}, isDue:{}", (subscription == null ? "null" : subscription.getCode()), to, isDue);
        if (subscription == null) {
            log.warn("Error when subscription is null!");
            throw new BusinessException("subscription is null");
        }
        if (!isFuture && to == null) {
            log.warn("Error when toDate is null!");
            throw new BusinessException("toDate is null");
        }
        BigDecimal balance = null, balanceDebit = null, balanceCredit = null;
        try {
            balanceDebit = computeOccAmount(subscription, OperationCategoryEnum.DEBIT, isFuture, isDue, to, status);
            balanceCredit = computeOccAmount(subscription, OperationCategoryEnum.CREDIT, isFuture, isDue, to, status);
            if (balanceDebit == null) {
                balanceDebit = BigDecimal.ZERO;
            }
            if (balanceCredit == null) {
                balanceCredit = BigDecimal.ZERO;
            }
            balance = balanceDebit.subtract(balanceCredit);
            ParamBean param = paramBeanFactory.getInstance();
            int balanceFlag = Integer.parseInt(param.getProperty("balance.multiplier", "1"));
            balance = balance.multiply(new BigDecimal(balanceFlag));
            log.debug("computeBalance subscription code:{} , balance:{}", subscription.getCode(), balance);
        } catch (Exception e) {
            throw new BusinessException("Internal error");
        }
        return balance;

    }

    /**
     * Compute occ amount.
     *
     * @param subscription the Subscription
     * @param operationCategoryEnum the operation category enum
     * @param isFuture the is future
     * @param isDue the is due
     * @param to the to
     * @param status the status
     * @return the big decimal
     * @throws Exception the exception
     */
    private BigDecimal computeOccAmount(Subscription subscription, OperationCategoryEnum operationCategoryEnum, boolean isFuture, boolean isDue, Date to, MatchingStatusEnum... status) throws Exception {
        BigDecimal balance = null;
        QueryBuilder queryBuilder = new QueryBuilder("select sum(unMatchingAmount) from AccountOperation");
        queryBuilder.addCriterionEnum("transactionCategory", operationCategoryEnum);

        if (!isFuture) {
            if (isDue) {
                queryBuilder.addCriterion("dueDate", "<=", to, false);

            } else {
                queryBuilder.addCriterion("transactionDate", "<=", to, false);
            }
        }

        queryBuilder.addCriterionEntity("subscription", subscription);
        if (status.length == 1) {
            queryBuilder.addCriterionEnum("matchingStatus", status[0]);
        } else {
            queryBuilder.startOrClause();
            for (MatchingStatusEnum st : status) {
                queryBuilder.addCriterionEnum("matchingStatus", st);
            }
            queryBuilder.endOrClause();
        }
        Query query = queryBuilder.getQuery(getEntityManager());
        balance = (BigDecimal) query.getSingleResult();
        return balance;
    }

    /**
     * Returns all subscriptions to the given offer by code
     *
     * @param offerCode code of the Offer to search
     * @param sortBy sort criteria
     * @param sortOrder sort order
     * @return list of Subscription
     */
    @SuppressWarnings("unchecked")
    public List<Subscription> listByOffer(String offerCode, String sortBy, SortOrder sortOrder) {
        QueryBuilder qb = new QueryBuilder(Subscription.class, "c");
        qb.addCriterionEntity("offer.code", offerCode);

        boolean ascending = true;
        if (sortOrder != null) {
            ascending = sortOrder.equals(SortOrder.ASCENDING);
        }
        qb.addOrderCriterion(sortBy, ascending);

        try {
            return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
        } catch (NoResultException e) {
            log.trace("No subscription found for offer code " + offerCode, e);
            return null;
        }
    }

    public Subscription instantiateDiscountPlan(Subscription entity, DiscountPlan dp) throws BusinessException {
       
    	if(CollectionUtils.isNotEmpty(entity.getDiscountPlanInstances())) {
    		boolean discountPlanInstancExist = entity.getDiscountPlanInstances().stream().anyMatch(discountPlanInstance -> discountPlanInstance.getDiscountPlan().getCode().equalsIgnoreCase(dp.getCode()));
    		if(discountPlanInstancExist) return entity;
    	}
        BillingAccount billingAccount = entity.getUserAccount().getBillingAccount();
        for (DiscountPlanInstance discountPlanInstance : billingAccount.getDiscountPlanInstances()) {
            if (dp.getCode().equals(discountPlanInstance.getDiscountPlan().getCode())) {
                throw new BusinessException("DiscountPlan " + dp.getCode() + " is already instantiated in Billing Account " + billingAccount.getCode() + ".");
            }
        }
        return (Subscription) discountPlanInstanceService.instantiateDiscountPlan(entity, dp, null, false);
    }

    public void terminateDiscountPlan(Subscription entity, DiscountPlanInstance dpi) throws BusinessException {
        discountPlanInstanceService.terminateDiscountPlan(entity, dpi);
    }

    /**
     * check if the subscription will be terminated in future
     *
     * @param subscription the subscription
     * @return true is the subscription will be terminated in future.
     */
    public boolean willBeTerminatedInFuture(Subscription subscription) {
        SubscriptionRenewal subscriptionRenewal = subscription != null ? subscription.getSubscriptionRenewal() : null;
        return (subscription != null && (subscription.getStatus() == SubscriptionStatusEnum.CREATED || subscription.getStatus() == ACTIVE) && subscription.getSubscribedTillDate() != null
                && subscription.getSubscribedTillDate().compareTo(new Date()) > 0 && subscriptionRenewal != null && !subscriptionRenewal.isAutoRenew() && subscriptionRenewal.getTerminationReason() != null
                && subscriptionRenewal.getEndOfTermAction() == SubscriptionRenewal.EndOfTermActionEnum.TERMINATE);
    }

    /**
     * cancel subscription termination
     *
     * @param subscription the subscription
     * @throws BusinessException business exception
     */
    public void cancelSubscriptionTermination(Subscription subscription) throws BusinessException {
        SubscriptionRenewal subscriptionRenewal = null;
        Date subscribedTillDate = null;

        String initialRenewal = subscription.getInitialSubscriptionRenewal();
        if (!StringUtils.isBlank(initialRenewal)) {

            Renewal renewal = JacksonUtil.fromString(initialRenewal, Renewal.class);
            subscriptionRenewal = renewal.getValue();
            subscriptionRenewal.setTerminationReason(subscriptionRenewal.getTerminationReason() != null && subscriptionRenewal.getTerminationReason().getId() != null ? subscriptionRenewal.getTerminationReason() : null);
            subscribedTillDate = renewal.getSubscribedTillDate();

        }
        subscription.setSubscriptionRenewal(subscriptionRenewal);
        subscription.setSubscribedTillDate(subscribedTillDate);
        subscription.setToValidity(null);
        update(subscription);
    }

    /**
     * check compatibility of services before instantiation
     *
     * @param subscription
     * @param selectedItemsAsList
     * @throws BusinessException
     */
    public void checkCompatibilityOfferServices(Subscription subscription, List<ServiceTemplate> selectedItemsAsList) throws BusinessException {

        if (subscription == null) {
            throw new BusinessException("subscription is Null in checkCompatibilityOfferServices ");
        }
        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        OfferTemplate offerTemplate = subscription.getOffer();
        
		if (offerTemplate.isDisabled() && subscription.getOrder() == null) {
			throw new BusinessException(String.format("OfferTemplate[code=%s] is disabled and cannot be subscribed to. Please select another offer.", offerTemplate.getCode()));
		}

        // loop in selected Available services for subscription
        for (ServiceTemplate serviceTemplate : selectedItemsAsList) {
            OfferServiceTemplate offerServiceTemplate = getOfferServiceTemplate(serviceTemplate.getCode(), offerTemplate);
            if (offerServiceTemplate == null) {
                throw new BusinessException("No offerServiceTemplate corresponds to " + serviceTemplate.getCode());
            }

            // list of incompatible services of an element of current Available services selected
            List<ServiceTemplate> serviceTemplateIncompatibles = offerServiceTemplate.getIncompatibleServices();

            // check if other selected Available services are part of incompatible services
            for (ServiceTemplate serviceTemplateOther : selectedItemsAsList) {
                if (!serviceTemplateOther.getCode().equals(serviceTemplate.getCode())) {
                    for (ServiceTemplate serviceTemplateIncompatible : serviceTemplateIncompatibles) {
                        if (serviceTemplateOther.getCode().equals(serviceTemplateIncompatible.getCode())) {
                            throw new BusinessException("Services Incompatibility between " + serviceTemplateIncompatible.getCode() + " and " + serviceTemplate.getCode());
                        }
                    }
                }
            }

            // check if subscribed service's are part of incompatible services of selected available services
            for (ServiceInstance subscribedService : serviceInstances) {
                for (ServiceTemplate serviceTemplateIncompatible : serviceTemplateIncompatibles) {
                    if (subscribedService.getCode().equals(serviceTemplateIncompatible.getCode())) {
                        throw new BusinessException("Services Incompatibility between " + serviceTemplateIncompatible.getCode() + " and " + serviceTemplate.getCode());
                    }
                }
            }
        }

        // check if selected available services are part of incompatible services of subscribed service's
        for (ServiceInstance subscribedService : serviceInstances) {
            if(subscribedService.getServiceTemplate() != null) {
                OfferServiceTemplate offerServiceTemplateSubscribedService = getOfferServiceTemplate(subscribedService.getServiceTemplate().getCode(), offerTemplate);
                // list of incompatible services of an element of current subscribed service's
                List<ServiceTemplate> serviceTemplateSubscribedServiceIncompatibles = offerServiceTemplateSubscribedService.getIncompatibleServices();

                for (ServiceTemplate serviceTemplateSelectedItem : selectedItemsAsList) {
                    for (ServiceTemplate serviceTemplateSubscribedServiceIncompatible : serviceTemplateSubscribedServiceIncompatibles) {
                        if (serviceTemplateSelectedItem.getCode().equals(serviceTemplateSubscribedServiceIncompatible.getCode())) {
                            throw new BusinessException("Services Incompatibility between " + serviceTemplateSelectedItem.getCode() + " and " + subscribedService.getCode());
                        }
                    }
                }
            }
        }
    }

    /**
     * Get OfferServiceTemplate which corresponds to serviceCode and offerTemplate
     *
     * @param serviceCode
     * @param offerTemplate
     * @return offerServiceTemplate
     */
    public OfferServiceTemplate getOfferServiceTemplate(String serviceCode, OfferTemplate offerTemplate) {
        OfferServiceTemplate offerServiceTemplateResult = null;
        List<OfferServiceTemplate> offerServiceTemplates = offerTemplate.getOfferServiceTemplates();
        for (OfferServiceTemplate offerServiceTemplate : offerServiceTemplates) {
            List<ServiceTemplate> serviceTemplates = offerServiceTemplate.getIncompatibleServices();
            if (serviceCode.equals(offerServiceTemplate.getServiceTemplate().getCode())) {
                offerServiceTemplateResult = offerServiceTemplate;
            }
        }
        return offerServiceTemplateResult;
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> findSubscriptions(BillingCycle billingCycle) {
        try {
            QueryBuilder qb = new QueryBuilder(Subscription.class, "s", null);
            if(billingCycle.getFilters() != null && !billingCycle.getFilters().isEmpty()) {
                qb.addPaginationConfiguration(new PaginationConfiguration(billingCycle.getFilters()));
            } else {
                qb.addCriterionEntity("s.billingCycle.id", billingCycle.getId());
            }
            qb.addOrderCriterionAsIs("id", true);

            return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
        } catch (Exception ex) {
            log.error("failed to find subscriptions", ex);
        }

        return null;
    }

    /**
     * List subscriptions that are associated with a given billing run
     *
     * @param billingRun Billing run
     * @return A list of Subscriptions
     */
    public List<Subscription> findSubscriptions(BillingRun billingRun) {
        return getEntityManager().createNamedQuery("Subscription.listByBillingRun", Subscription.class).setParameter("billingRunId", billingRun.getId()).getResultList();
    }


    /**
     * Update subscribedTillDate field in subscription while it was not renewed yet. Also calculate Notify of renewal date
     */
    public void updateSubscribedTillAndRenewalNotifyDates(Subscription subscription) {
        if (subscription.isRenewed()) {
            return;
        }
        if (subscription.getSubscriptionRenewal().getInitialTermType().equals(SubscriptionRenewal.InitialTermTypeEnum.RECURRING)) {
            if (subscription.getSubscriptionDate() != null && subscription.getSubscriptionRenewal() != null && subscription.getSubscriptionRenewal().getInitialyActiveFor() != null) {
                if (subscription.getSubscriptionRenewal().getInitialyActiveForUnit() == null) {
                    subscription.getSubscriptionRenewal().setInitialyActiveForUnit(RenewalPeriodUnitEnum.MONTH);
                }
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(subscription.getSubscriptionDate());
                calendar.add(subscription.getSubscriptionRenewal().getInitialyActiveForUnit().getCalendarField(), subscription.getSubscriptionRenewal().getInitialyActiveFor());
                subscription.setSubscribedTillDate(calendar.getTime());

            } else {
                subscription.setSubscribedTillDate(null);
            }
        } else if (subscription.getSubscriptionRenewal().getInitialTermType().equals(SubscriptionRenewal.InitialTermTypeEnum.CALENDAR)) {
            if (subscription.getSubscriptionDate() != null && subscription.getSubscriptionRenewal() != null && subscription.getSubscriptionRenewal().getCalendarInitialyActiveFor() != null) {
                org.meveo.model.catalog.Calendar calendar = CalendarService.initializeCalendar(subscription.getSubscriptionRenewal().getCalendarInitialyActiveFor(), subscription.getSubscriptionDate(), subscription);
                Date date = calendar.nextCalendarDate(subscription.getSubscriptionDate());
                subscription.setSubscribedTillDate(date);
            } else {
                subscription.setSubscribedTillDate(null);
            }
        }

        if (subscription.getSubscribedTillDate() != null && subscription.getSubscriptionRenewal().isAutoRenew() && subscription.getSubscriptionRenewal().getDaysNotifyRenewal() != null) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(subscription.getSubscribedTillDate());
            calendar.add(Calendar.DAY_OF_MONTH, subscription.getSubscriptionRenewal().getDaysNotifyRenewal() * (-1));
            subscription.setNotifyOfRenewalDate(calendar.getTime());
        } else {
            subscription.setNotifyOfRenewalDate(null);
        }
        subscription.autoUpdateEndOfEngagementDate();
    }

    public Subscription findByCodeAndValidityDate(String subscriptionCode, Date date) {
        if(date == null)
            return findByCode(subscriptionCode);

        List<Subscription> subscriptions = getEntityManager().createNamedQuery("Subscription.findByValidity", Subscription.class)
                .setParameter("code", subscriptionCode.toLowerCase())
                .setParameter("validityDate", date)
                .getResultList();

        return getActiveOrLastUpdated(subscriptions);
    }

    public Subscription getLastVersionSubscriptionForPatch(String subCode) {
        TypedQuery<Subscription> query = getEntityManager()
                .createQuery("select s from Subscription s "
                        + "left join fetch s.offer o "
                        + "left join fetch o.allowedOffersChange a "
                        + "where lower(s.code)=:code "
                        + "and (s.validity is null or s.validity.to is null) "
                        + "order by s.validity.to desc", entityClass)
                .setParameter("code", subCode.toLowerCase())
                .setMaxResults(1);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            log.debug("No {} of code {} found", getEntityClass().getSimpleName(), subCode);
            return null;
        }
    }
    
    public Subscription getLastVersionSubscription(String subCode) {
        TypedQuery<Subscription> query = getEntityManager()
                .createQuery("select s from Subscription s "
                        + "where lower(s.code)=:code "
                        + "and (s.validity is null or s.validity.to is null) "
                        + "order by s.validity.to desc", entityClass)
                .setParameter("code", subCode.toLowerCase())
                .setMaxResults(1);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            log.debug("No {} of code {} found", getEntityClass().getSimpleName(), subCode);
            return null;
        }
    }

    private Subscription getActiveOrLastUpdated(List<Subscription> subscriptions) {
        if(subscriptions.isEmpty()) {
            return null;
            
        } else if (subscriptions.size()==1) {
            return subscriptions.get(0);
        }

        Optional<Subscription> activeSubscription = subscriptions.stream()
                .filter(s -> ACTIVE.equals(s.getStatus()))
                .findFirst();

        return activeSubscription.orElseGet(() -> subscriptions.stream()
                .sorted(Comparator.comparing(this::getUpdated).reversed())
                .collect(Collectors.toList())
                .get(0));
    }

    private Date getUpdated(Subscription subscription) {
        return subscription.getAuditable().getUpdated() != null ? subscription.getAuditable().getUpdated() : subscription.getAuditable().getCreated();
    }

    @Override
    public Subscription findByCode(String code) {
        List<Subscription> subscriptions = findListByCode(code);
        return getActiveOrLastUpdated(subscriptions);
    }

    public List<Subscription> findListByCode(String code) {
        TypedQuery<Subscription> query = getEntityManager().createQuery("select be from Subscription be where lower(code)=:code", entityClass)
                .setParameter("code", code.toLowerCase()).setHint("org.hibernate.cacheable", true);
        try {
            return query.getResultList();
        } catch (NoResultException e) {
            log.debug("No {} of code {} found", getEntityClass().getSimpleName(), code);
            return new ArrayList<>();
        }


    }

    /**
     * Get a count of subscriptions by a parent user account
     * 
     * @param parent Parent user account
     * @return A number of child subscriptions
     */
    public long getCountByParent(UserAccount parent) {

        return getEntityManager().createNamedQuery("Subscription.getCountByParent", Long.class).setParameter("parent", parent).getSingleResult();
    }

    public List<Subscription> findListByCodeAndValidityDate(String code, Date date) {
        return getEntityManager().createNamedQuery("Subscription.findByValidity", Subscription.class)
                .setParameter("code", code.toLowerCase())
                .setParameter("validityDate", date)
                .getResultList();
    }

    /**
     * Find matching or overlapping versions for a given subscription code and date range
     * 
     * @param code Subscription code
     * @param from Date period start date
     * @param to Date period end date
     * @param entityId Identifier of an entity to ignore (as not to match itself in case of update)
     * @return Matched subscriptions
     */
    @SuppressWarnings("unchecked")
    public List<Subscription> getMatchingVersions(String code, Date from, Date to, Long entityId) {
    	
    	QueryBuilder qb = new QueryBuilder(Subscription.class, "s");
    	
    	if(entityId == null) {
    		qb.addSql("s.id <> -1000L");
    	}else {
    		qb.addSqlCriterion("s.id <> :id", "id", entityId);
    	}
    	
    	qb.addSqlCriterion("s.code = :code","code", code);
    	
    	from = DateUtils.setTimeToZero(from);
    	to = DateUtils.setTimeToZero(to);
    	
    	if (from != null && to != null) {
            qb.addSqlCriterionMultiple("((date(s.validity.from) is null or date(s.validity.from)<:endDate) AND (:startDate<date(s.validity.to) or s.validity.to is null))", "startDate", from, "endDate", to);
        } else if (from != null) {
            qb.addSqlCriterion("(:startDate<date(s.validity.to) or s.validity.to is null)", "startDate", from);
        } else if (to != null) {
            qb.addSqlCriterion("(s.validity.from is null or date(s.validity.from)<:endDate)", "endDate", to);
        }
    	 
    	 try {
             return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
         } catch (NoResultException e) {
             return null;
         }

    }
    
    @SuppressWarnings("unchecked")
	public List<Subscription> listByBillingAccount(BillingAccount billingAccount) {
    	 QueryBuilder qb = new QueryBuilder(Subscription.class, "s", null);
         qb.addCriterionEntity("s.userAccount.billingAccount.code", billingAccount.getCode());
         try {
             return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
         } catch (NoResultException e) {
             return null;
         }
    }

    public List<Subscription> findByBA(BillingAccount billingAccount) {
        return getEntityManager().createNamedQuery("Subscription.getBillingAccountSubscriptions", Subscription.class)
                                 .setParameter("billingAccount", billingAccount)
                                 .getResultList();
    }
    
    public void removePaymentMethodLink(BillingAccount billingAccount) {
		getEntityManager().createNamedQuery("Subscription.unlinkPaymentMehtodByBA")
				.setParameter("billingAccount", billingAccount)
				.executeUpdate();
    }

    /**
     * Apply an one shot charge on a subscription
     *
     * @param postData The apply one shot charge instance request dto
     * @return OneShotChargeInstance
     * @throws MeveoApiException Meveo api exception
     */
    public OneShotChargeInstance applyOneShotChargeInstance(ApplyOneShotChargeInstanceRequestDto postData,
                                                            boolean isVirtual) throws MeveoApiException {
        checkOneShotChargeInstancePrice(postData);
        Date operationDate = postData.getOperationDate();
        if (operationDate == null) {
            postData.setOperationDate(new Date());
        }
        OneShotChargeTemplate oneShotChargeTemplate = oneShotChargeTemplateService.findByCode(postData.getOneShotCharge());
        if (oneShotChargeTemplate == null) {
            throw new EntityDoesNotExistsException(OneShotChargeTemplate.class, postData.getOneShotCharge());
        }
        Subscription subscription = findByCodeAndValidityDate(postData.getSubscription(), postData.getSubscriptionValidityDate());
        if (subscription == null) {
            throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscription(), postData.getSubscriptionValidityDate());
        }
        CommercialOrder commercialOrder = null;
        if (postData.getCommercialOrderId() != null) {
            commercialOrder = commercialOrderService.findById(postData.getCommercialOrderId());
            if (commercialOrder == null) {
                throw new EntityDoesNotExistsException(CommercialOrder.class, postData.getOneShotCharge());
            }
        }
        if (postData.getWallet() != null) {
            WalletTemplate walletTemplate = walletTemplateService.findByCode(postData.getWallet());
            if (walletTemplate == null) {
                throw new EntityDoesNotExistsException(WalletTemplate.class, postData.getWallet());
            }
            if ((!postData.getWallet().equals("PRINCIPAL")) && !subscription.getUserAccount().getPrepaidWallets().containsKey(postData.getWallet())) {
                if (postData.getCreateWallet() != null && postData.getCreateWallet()) {
                    subscription.getUserAccount().getWalletInstance(postData.getWallet());
                } else {
                    throw new MeveoApiException("Wallet " + postData.getWallet() + " is not attached to the user account, but were instructed not to create it");
                }
            }
        }
        OneShotChargeInstance oneShotChargeInstance = new OneShotChargeInstance();
        try {
            ServiceInstance serviceInstance = buildServiceInstanceForOSO(postData, subscription);

            OneShotChargeInstance osho = oneShotChargeInstanceService
                    .instantiateAndApplyOneShotCharge(subscription, serviceInstance, oneShotChargeTemplate, postData.getWallet(), postData.getOperationDate(),
                            postData.getAmountWithoutTax(), postData.getAmountWithTax(), postData.getQuantity(), postData.getCriteria1(), postData.getCriteria2(),
                            postData.getCriteria3(), postData.getDescription(), null, oneShotChargeInstance.getCfValues(), true, ChargeApplicationModeEnum.SUBSCRIPTION, isVirtual, commercialOrder);

            if (StringUtils.isNotBlank(postData.getBusinessKey())) {
                osho.getWalletOperations().stream().forEach(wo -> {
                    wo.setBusinessKey(postData.getBusinessKey());
                });
            }

            if (Boolean.TRUE.equals(postData.getGenerateRTs())) {
                osho.getWalletOperations().stream().forEach(wo -> {
                    RatedTransaction ratedTransaction = ratedTransactionService.createRatedTransaction(wo, isVirtual);
                    ratedTransaction.setBusinessKey(wo.getBusinessKey());
                    ratedTransaction.setContract(wo.getContract());
                    ratedTransaction.setContractLine(wo.getContractLine());
                });
            }

            return osho;
        } catch (RatingException e) {
            log.trace("Failed to apply one shot charge {}: {}", oneShotChargeTemplate.getCode(), e.getRejectionReason());
            throw new MeveoApiException(e.getMessage());

        } catch (BusinessException e) {
            log.error("Failed to apply one shot charge {}: {}", oneShotChargeTemplate.getCode(), e.getMessage(), e);
            throw e;
        }

    }

    private void checkOneShotChargeInstancePrice(ApplyOneShotChargeInstanceRequestDto postData) {
        if (appProvider.isEntreprise()) {
            if (postData.getUnitPrice() != null && postData.getAmountWithoutTax() != null && postData.getUnitPrice().compareTo(postData.getAmountWithoutTax()) != 0) {
                throw new MeveoApiException("unitPrice and amountWithoutTax must be equal. Futhermore, ‘amountWithoutTax' is deprecated, you should only send 'unitPrice’.");
            }
            if (postData.getUnitPrice() == null && postData.getAmountWithoutTax() == null && postData.getAmountWithTax() != null) {
                throw new MeveoApiException("Provider is in B2B mode (entreprise=true). This means that unit prices are without tax. Furthermore, ‘amountWithTax’ is deprecated, you should send ‘unitPrice’.");
            }
            if (postData.getUnitPrice() != null) {
                postData.setAmountWithoutTax(postData.getUnitPrice());
            }
            postData.setAmountWithTax(null);
        } else {
            if (postData.getUnitPrice() != null && postData.getAmountWithTax() != null && postData.getUnitPrice().compareTo(postData.getAmountWithTax()) != 0) {
                throw new MeveoApiException("unitPrice and amountWithTax must be equal. Furthermore, ‘amountWithTax' is deprecated, you should only send 'unitPrice’.");
            }
            if (postData.getUnitPrice() == null && postData.getAmountWithTax() == null && postData.getAmountWithoutTax() != null) {
                throw new MeveoApiException("Provider is in B2C mode (entreprise=false). This means that unit prices are with tax. Furthermore, ‘amountWithoutTax’ is deprecated, you should send ‘unitPrice’.");
            }
            if (postData.getUnitPrice() != null) {
                postData.setAmountWithTax(postData.getUnitPrice());
            }
            postData.setAmountWithoutTax(null);
        }
    }

    /**
     * Build ServiceInstance from OSO Payload
     *
     * @param postData     OSO payload
     * @param subscription subscription
     * @return Virtual or Real ServiceInstance
     */
    private ServiceInstance buildServiceInstanceForOSO(ApplyOneShotChargeInstanceRequestDto postData, Subscription subscription) {
        ServiceInstance serviceInstance = null;
        if (postData.getAttributes() != null) {
            serviceInstance = new ServiceInstance(); // Create a virtual ServiceInstance
            serviceInstance.setSubscription(subscription);

            // Product data
            if (StringUtils.isNotBlank(postData.getProductCode())) {
                Product product = productService.findByCode(postData.getProductCode());
                ProductVersion pVersion = new ProductVersion();
                pVersion.setProduct(product);
                serviceInstance.setCode(product.getCode());
                serviceInstance.setProductVersion(pVersion);
            }
            // add attributes
            for (AttributeInstanceDto attributeInstanceDto : postData.getAttributes()) {
                AttributeInstance attributeInstance = new AttributeInstance();
                attributeInstance.setAttribute(attributeService.findByCode(attributeInstanceDto.getAttributeCode()));
                attributeInstance.setServiceInstance(serviceInstance);
                attributeInstance.setSubscription(subscription);
                attributeInstance.setDoubleValue(attributeInstanceDto.getDoubleValue());
                attributeInstance.setStringValue(attributeInstanceDto.getStringValue());
                attributeInstance.setBooleanValue(attributeInstanceDto.getBooleanValue());
                attributeInstance.setDateValue(attributeInstanceDto.getDateValue());
                serviceInstance.addAttributeInstance(attributeInstance);
            }
        } else { // no attributs provided in payload (OSO case for example)
            List<ServiceInstance> alreadyInstantiatedServices = null;
            if (StringUtils.isNotBlank(postData.getProductCode())) {
                alreadyInstantiatedServices = serviceInstanceService.findByCodeSubscriptionAndStatus(postData.getProductCode(), subscription,
                        InstanceStatusEnum.ACTIVE);
                if (alreadyInstantiatedServices == null || alreadyInstantiatedServices.isEmpty()) {
                    throw new BusinessException("The product instance " + postData.getProductCode() + " doest not exist for this subscription or is not active");
                }
            } else {
                alreadyInstantiatedServices = subscription.getServiceInstances().stream()
                        .filter(si -> si.getStatus() == InstanceStatusEnum.ACTIVE)
                        .collect(Collectors.toList());
            }
            if (alreadyInstantiatedServices.size() > 1) {
                if (postData.getProductInstanceId() == null) {
                    throw new BusinessException("More than one Product Instance found for Product '" + postData.getProductCode()
                            + "' and Subscription '" + subscription.getCode() + "'. Please provide productInstanceId field");
                } else {
                    serviceInstance = serviceInstanceService.findById(postData.getProductInstanceId());
                    if (serviceInstance == null) {
                        throw new BusinessException("No Product Instance found with id=" + postData.getProductInstanceId());
                    }
                }
            } else {
                serviceInstance = alreadyInstantiatedServices.get(0);
            }
        }
        return serviceInstance;
    }

    public ProcessApplyChargeListResult applyOneShotChargeList(ApplyOneShotChargeListInput postData) {
        if(postData == null) {
            throw new InvalidParameterException("The input parameters are required");
        }
        if(ListUtils.isEmtyCollection(postData.getChargesToApply())) {
            throw new InvalidParameterException("The charges to apply are required");
        }
        postData.getChargesToApply().forEach(c -> c.setGenerateRTs(postData.isGenerateRTs()));

        SynchronizedIterator<ApplyOneShotChargeInstanceRequestDto> syncCharges = new SynchronizedIterator<>(postData.getChargesToApply());

        ProcessApplyChargeListResult result = new ProcessApplyChargeListResult(postData.getMode(), syncCharges.getSize());

        int nbThreads = (postData.getMode() == PROCESS_ALL) ? Runtime.getRuntime().availableProcessors() : 1;
        if (nbThreads > postData.getChargesToApply().size()) {
            nbThreads = postData.getChargesToApply().size();
        }

        List<Runnable> tasks = new ArrayList<>(nbThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int k = 0; k < nbThreads; k++) {
            tasks.add(() ->
                    this.subscriptionService.applyOneShotChargeInstance(syncCharges, result, postData.isGenerateRTs(), postData.isReturnWalletOperations(),
                            postData.isReturnWalletOperationDetails(), postData.isVirtual())
            );
        }

        for (Runnable task : tasks) {
            futures.add(executor.submit(task));
        }

        for (Future<?> future : futures) {
            try {
                future.get();

            } catch (InterruptedException | CancellationException e) {
                log.error("Failed to execute Mediation API async method", e);
            } catch (ExecutionException e) {
                log.error("Failed to execute Mediation API async method", e);
            }
        }

        // Summary
        AtomicReference<BigDecimal> amountWithTax = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> amountWithoutTax = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> amountTax = new AtomicReference<>(BigDecimal.ZERO);
        AtomicInteger walletOperationCount = new AtomicInteger(0);
        Arrays.stream(result.getAppliedCharges()).forEach(charge -> {
            if (charge != null) {
                amountWithTax.accumulateAndGet(Optional.ofNullable(charge.getAmountWithTax()).orElse(BigDecimal.ZERO), BigDecimal::add);
                amountWithoutTax.accumulateAndGet(Optional.ofNullable(charge.getAmountWithoutTax()).orElse(BigDecimal.ZERO), BigDecimal::add);
                amountTax.accumulateAndGet(Optional.ofNullable(charge.getAmountTax()).orElse(BigDecimal.ZERO), BigDecimal::add);

                walletOperationCount.addAndGet(Optional.ofNullable(charge.getWalletOperationCount()).orElse(0));
            } else {
                log.warn("cdrProcessingResult amouts and WOCount will have default 0 value, due to charge null");
            }
        });

        result.setAmountWithTax(amountWithTax.get());
        result.setAmountWithoutTax(amountWithoutTax.get());
        result.setAmountTax(amountTax.get());
        result.setWalletOperationCount(walletOperationCount.get());

        return result;
    }

    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void applyOneShotChargeInstance(SynchronizedIterator<ApplyOneShotChargeInstanceRequestDto> syncCharges, ProcessApplyChargeListResult result,
                                           boolean generateRTs, boolean returnWalletOperations, boolean returnWalletOperationDetails,
                                           boolean isVirtual) {

        while(true) {
            SynchronizedIterator<ApplyOneShotChargeInstanceRequestDto>.NextItem<ApplyOneShotChargeInstanceRequestDto> nextWPosition = syncCharges.nextWPosition();

            if(nextWPosition == null) {
                break;
            }

            int chargePosition = nextWPosition.getPosition();
            ApplyOneShotChargeInstanceRequestDto chargeToApply = nextWPosition.getValue();

            try {
                log.info("applyOneShotChargeInstance #{}", chargePosition);
                AppliedChargeResponseDto oshoDto;
                if (result.getMode() == ROLLBACK_ON_ERROR) {
                    OneShotChargeInstance osho = applyOneShotChargeInstance(chargeToApply, isVirtual);
                    oshoDto = createAppliedChargeResponseDto(osho, returnWalletOperations, returnWalletOperationDetails);
                } else {
                    oshoDto = methodCallingUtils.callCallableInNewTx(() -> {
                        OneShotChargeInstance osho = applyOneShotChargeInstance(chargeToApply, isVirtual);
                        return createAppliedChargeResponseDto(osho, returnWalletOperations, returnWalletOperationDetails);
                    });
                }

                result.addAppliedCharge(chargePosition, oshoDto);
                result.getStatistics().addSuccess();
            } catch (Exception e) {
                log.error("Error when applying OSO at position #["+chargePosition+"]" , e);
                result.getStatistics().addFail();
                result.addAppliedCharge(chargePosition, createAppliedChargeResponseErrorDto(e.getMessage()));
                if(result.getMode() == PROCESS_ALL) {
                    continue;
                } else if (result.getMode() == ApplyOneShotChargeListModeEnum.STOP_ON_FIRST_FAIL) {
                    result.setAppliedCharges(Arrays.copyOf(result.getAppliedCharges(), chargePosition + 1));
                    break;
                } else {
                    result.setAppliedCharges(new AppliedChargeResponseDto[] {createAppliedChargeResponseErrorDto(e.getMessage())});
                    throw new BusinessApiException(e);
                }
            }
        }

    }

    private AppliedChargeResponseDto createAppliedChargeResponseDto(OneShotChargeInstance osho,
                                                                    boolean returnWalletOperation, boolean returnWallerOperationDetails) {
        AppliedChargeResponseDto lDto = new AppliedChargeResponseDto();

        lDto.setWalletOperationCount(osho.getWalletOperations().size());
        BigDecimal amountWithTax = BigDecimal.ZERO;
        BigDecimal amountWithoutTax = BigDecimal.ZERO;
        BigDecimal amountTax = BigDecimal.ZERO;
        for (WalletOperation wo : osho.getWalletOperations()) {
            if(returnWallerOperationDetails) {
                lDto.getWalletOperations().add(new WalletOperationDto(wo, wo.getAccountingArticle()));
            } else if(returnWalletOperation) {
                WalletOperationDto woDto = new WalletOperationDto();
                woDto.setId(wo.getId());
                lDto.getWalletOperations().add(woDto);
            }
            amountWithTax = amountWithTax.add(wo.getAmountWithTax() != null ? wo.getAmountWithTax() : BigDecimal.ZERO);
            amountWithoutTax = amountWithoutTax.add(wo.getAmountWithoutTax() != null ? wo.getAmountWithoutTax() : BigDecimal.ZERO);
            amountTax = amountTax.add(wo.getAmountTax() != null ? wo.getAmountTax() : BigDecimal.ZERO);
        }
        lDto.setAmountTax(amountTax);
        lDto.setAmountWithoutTax(amountWithoutTax);
        lDto.setAmountWithTax(amountWithTax);

        return lDto;
    }

    private AppliedChargeResponseDto createAppliedChargeResponseErrorDto(String errorMessage) {
        AppliedChargeResponseDto lDto = new AppliedChargeResponseDto();

        lDto.setError(new AppliedChargeResponseDto.CdrError(errorMessage));

        return lDto;
    }
	
	public List<Subscription> listByOfferAndOrProduct(String offerCode, String productCode) {
		QueryBuilder qb = new QueryBuilder(Subscription.class, "c");
		if(StringUtils.isNotBlank(productCode)) {
			qb = new QueryBuilder(Subscription.class, "c", List.of("serviceInstances") );
			qb.addCriterionEntity("c_serviceInstances.productVersion.product.code", productCode);
		}
		
		if(StringUtils.isNotBlank(offerCode)) {
			qb.addCriterionEntity("c.offer.code", offerCode);
		}
		qb.addCriterion("c.status", "=",SubscriptionStatusEnum.ACTIVE, false);
		// criterion for subscription does not have discount plan
		qb.addSql("c.code not in (select distinct s.code from Subscription s join s.discountPlanInstances dpi)");
			return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
	}
	
	public List<Subscription> findByListOfCodes(List<String> codes) {
		QueryBuilder qb = new QueryBuilder(Subscription.class, "c");
		qb.addSqlCriterion("c.code in (:codes)", "codes", codes);
		return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
	}

    /**
     * Updates the Monthly Recurring Revenue (MRR) for all Subscriptions.
     *
     * This method calculates the MRR for each Subscription by summing the MRR of all
     * active ServiceInstances associated with the Subscription. The calculated MRR
     * is then updated in the Subscription.
     */
    public void massCalculateMRR() {
        
        Query query = getEntityManager().createQuery("update Subscription s set s.mrr = (select sum(si.mrr) from ServiceInstance si where si.subscription.id = s.id and si.status = :status and si.mrr is not null)");
        query.setParameter("status", InstanceStatusEnum.ACTIVE);
        query.executeUpdate();
        
    }

    public void calculateMrr(Subscription subscription) {
        BigDecimal mrr = subscription.getServiceInstances()
                                     .stream()
                                     .filter(si -> InstanceStatusEnum.ACTIVE.equals(si.getStatus()) && si.getMrr() != null)
                                     .map(ServiceInstance::getMrr)
                                     .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if(!mrr.equals(subscription.getMrr())) {
            subscription.setMrr(mrr);
            update(subscription);
            subscriptionService.getEntityManager().flush();
            billingAccountService.calculateMrr(subscription.getUserAccount().getBillingAccount());
            offerTemplateService.calculateArr(subscription.getOffer());
        }
    }
}