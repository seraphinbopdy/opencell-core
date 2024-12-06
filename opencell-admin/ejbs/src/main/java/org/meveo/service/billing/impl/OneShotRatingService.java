package org.meveo.service.billing.impl;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.RatingException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.RatingResult;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.ChargeApplicationModeEnum;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.OneShotChargeInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.cpq.AttributeValue;
import org.meveo.model.cpq.commercial.CommercialOrder;
import org.meveo.model.rating.EDR;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.catalog.impl.OneShotChargeTemplateService;
import org.meveo.service.tax.TaxClassService;

import jakarta.ejb.EJBTransactionRolledbackException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class OneShotRatingService extends RatingService implements Serializable {

    private static final long serialVersionUID = 6554942821072192230L;

    @Inject
    private OneShotChargeTemplateService oneShotChargeTemplateService;

    @Inject
    private BillingAccountService billingAccountService;

    @Inject
    private WalletOperationService walletOperationService;

    @Inject
    private EdrService edrService;

    @Inject
    private TaxClassService taxClassService;

    /**
     * Apply/rate a one shot charge. Change charge instance status to CLOSED.
     * 
     * @param chargeInstance Charge instance to apply
     * @param inputQuantity Quantity to apply
     * @param quantityInChargeUnits Quantity to apply in charge units
     * @param applicationDate Charge application date
     * @param orderNumberOverride Order number to override
     * @param chargeMode Charge mode
     * @param isVirtual Is it a virtual charge
     * @param failSilently If true, any error will be reported and returned in the rating result instead of throwing an exception
     * @param commercialOrder commercial order
     * @return Rating result containing a rated wallet operation (persisted) and triggered EDRs (persisted)
     * @throws BusinessException General exception.
     * @throws RatingException EDR rejection due to lack of funds, data validation, inconsistency or other rating related failure
     */
    public RatingResult rateOneShotCharge(OneShotChargeInstance chargeInstance, BigDecimal inputQuantity, BigDecimal quantityInChargeUnits, Date applicationDate, String orderNumberOverride,
            ChargeApplicationModeEnum chargeMode, boolean isVirtual, boolean failSilently, CommercialOrder commercialOrder) throws BusinessException, RatingException {

        if (applicationDate == null) {
            applicationDate = new Date();
        }

        if (chargeMode == null) {
            chargeMode = ChargeApplicationModeEnum.SUBSCRIPTION;
        }
        if(commercialOrder != null) {
            chargeInstance.setOrderNumber(commercialOrder.getCode());
        }

        Subscription subscription = chargeInstance.getSubscription();

        if(StringUtils.isBlank(chargeInstance.getOrderNumber()) ) {
            if(subscription.getOrder() != null) {
                chargeInstance.setOrderNumber(chargeInstance.getSubscription().getOrder().getCode());
            }
        }

        if (!RatingService.isORChargeMatch(chargeInstance)) {
            log.debug("Not rating oneshot chargeInstance {}/{}, filter expression or service attributes evaluated to FALSE", chargeInstance.getId(), chargeInstance.getCode());
            return new RatingResult();
        }
        if (chargeInstance.getChargeTemplate() != null
                && chargeInstance.getChargeTemplate().getQuantityAttribute() != null) {
            BigDecimal quantityAttribute = getQuantityAttribute(chargeInstance.getServiceInstance(),
                    chargeInstance.getChargeTemplate().getQuantityAttribute().getCode());
            if (quantityAttribute.compareTo(ZERO) >= 0) {
                inputQuantity = chargeInstance.getQuantity().multiply(quantityAttribute);
            }
        }


        log.debug("Will rate a one shot charge subscription {}, quantity {}, applicationDate {}, chargeInstance {}/{}/{}", subscription.getId(), quantityInChargeUnits, applicationDate, chargeInstance.getId(),
            chargeInstance.getCode(), chargeInstance.getDescription());

        // Check if there is any attribute with value FALSE, indicating that service instance is not active
        if (anyFalseAttributeMatch(chargeInstance)) {
            return new RatingResult();
        }

        RatingResult ratingResult = null;
        try {
            ratingResult = rateChargeAndInstantiateTriggeredEDRs(chargeInstance, applicationDate, inputQuantity, quantityInChargeUnits, orderNumberOverride,
                    null, null, null, chargeMode, null, null, false, isVirtual, commercialOrder);

            final List<WalletOperation> walletOperations = ratingResult.getWalletOperations();
			incrementAccumulatorCounterValues(walletOperations, ratingResult, isVirtual);
            chargeInstance.setWalletOperations(walletOperations);
            if (!isVirtual && !walletOperations.isEmpty()) {
                if (ratingResult.getTriggeredEDRs() != null) {
                    for (EDR triggeredEdr : ratingResult.getTriggeredEDRs()) {
                        edrService.create(triggeredEdr);
                    }
                }
                for (WalletOperation walletOperation : walletOperations) {
	                checkDiscountedWalletOpertion(walletOperation, walletOperations);
                    walletOperationService.chargeWalletOperation(walletOperation);
                }

                OneShotChargeTemplate oneShotChargeTemplate = null;

                ChargeTemplate chargeTemplate = chargeInstance.getChargeTemplate();

                if (chargeTemplate instanceof OneShotChargeTemplate) {
                    oneShotChargeTemplate = (OneShotChargeTemplate) chargeInstance.getChargeTemplate();
                } else {
                    oneShotChargeTemplate = oneShotChargeTemplateService.findById(chargeTemplate.getId());
                }

                boolean immediateInvoicing = (oneShotChargeTemplate != null && oneShotChargeTemplate.getImmediateInvoicing() != null) ? oneShotChargeTemplate.getImmediateInvoicing() : false;

                if (Boolean.TRUE.equals(immediateInvoicing)) {

                    BillingAccount billingAccount = subscription.getUserAccount().getBillingAccount();

                    int delay = 0;
                    if (billingAccount.getBillingCycle().getInvoiceDateDelayEL() != null) {
                        delay = InvoiceService.resolveImmediateInvoiceDateDelay(billingAccount.getBillingCycle().getInvoiceDateDelayEL(), walletOperations.get(0), billingAccount);
                    }

                    Date nextInvoiceDate = DateUtils.addDaysToDate(billingAccount.getNextInvoiceDate(), -delay);
                    nextInvoiceDate = DateUtils.setTimeToZero(nextInvoiceDate);
                    applicationDate = DateUtils.setTimeToZero(applicationDate);

                    if (nextInvoiceDate == null || applicationDate.after(nextInvoiceDate)) {
                        billingAccount.setNextInvoiceDate(applicationDate);
                        billingAccountService.update(billingAccount);
                    }
                }

                // OSO SI Virtual : clean WalletOperation.SI if it is a SI Virtual (id==null)
                ratingResult.getWalletOperations().forEach(walletOperation -> walletOperation.setServiceInstance(
                        walletOperation.getServiceInstance() != null && walletOperation.getServiceInstance().getId() == null
                                ? null : walletOperation.getServiceInstance()));

            }

            // Mark charge instance as closed
            chargeInstance.setStatus(InstanceStatusEnum.CLOSED);

            // DIRTY FIX : pression sur une livraison OSO
            // Le service retournait bien le proxy hibernate pour recuperer la TaxClass, mais apres un merge du 15/02, ca ne fonctionne plus
            // Donc je re-recuperer l'objet par son ID et on eteint le feux
            Optional.ofNullable(ratingResult.getWalletOperations()).orElse(Collections.emptyList())
                    .forEach(wo -> {
                        if (wo.getTaxClass() != null) {
                            wo.setTaxClass(taxClassService.findById(wo.getTaxClass().getId()));
                        }
                    });

            return ratingResult;
        
        } catch (EJBTransactionRolledbackException e) {
            if (ratingResult != null) {
                revertCounterChanges(ratingResult.getCounterChanges());
            }
            throw e;
            
        } catch (Exception e) {
            if (ratingResult != null) {
                revertCounterChanges(ratingResult.getCounterChanges());
            }

            if (failSilently) {
                log.debug("Failed to rate a one shot charge subscription {}, quantity {}, applicationDate {}, chargeInstance {}/{}/{}", subscription.getId(), quantityInChargeUnits, applicationDate,
                    chargeInstance.getId(), chargeInstance.getCode(), chargeInstance.getDescription(), e);

                return new RatingResult(e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Get quantity attribute value from service instance.
     *
     * @param serviceInstance Service instance
     * @param quantityAttribute Quantity attribute code
     * @return Quantity attribute value
     */
    private BigDecimal getQuantityAttribute(ServiceInstance serviceInstance, String quantityAttribute) {
        BigDecimal quantityAttributeValue = ONE;
        Map<String, Object> attributeValues = fromAttributeValue(fromAttributeInstances(serviceInstance));
        Object quantityObject = attributeValues.get(quantityAttribute);

        if (quantityObject != null) {
            try {
                quantityAttributeValue = valueOf(Double.parseDouble(quantityObject.toString()));
            } catch (NumberFormatException e) {
                log.debug("wrong value format when formating quantity attribute cannot cast '{}' to double value", quantityObject);
            }
        }

        return quantityAttributeValue;
    }

    private List<AttributeValue> fromAttributeInstances(ServiceInstance serviceInstance) {
        if (serviceInstance == null) {
            return Collections.emptyList();
        }
        return serviceInstance.getAttributeInstances().stream()
                .map(attributeInstance -> (AttributeValue) attributeInstance)
                .collect(toList());
    }

    private Map<String, Object> fromAttributeValue(List<AttributeValue> attributeValues) {
        return attributeValues
                .stream()
                .filter(attributeValue -> attributeValue.getAttribute().getAttributeType().getValue(attributeValue) != null)
                .collect(toMap(key -> key.getAttribute().getCode(),
                        value -> value.getAttribute().getAttributeType().getValue(value)));
    }

}