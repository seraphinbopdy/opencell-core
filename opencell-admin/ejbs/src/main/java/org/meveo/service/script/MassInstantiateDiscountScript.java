package org.meveo.service.script;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Subscription;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.catalog.DiscountPlanStatusEnum;
import org.meveo.model.catalog.DiscountPlanTypeEnum;
import org.meveo.model.catalog.OfferProductTemplate;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.cpq.Product;
import org.meveo.model.cpq.offer.OfferComponent;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.DiscountPlanInstanceService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.catalog.impl.DiscountPlanService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.meveo.service.cpq.ProductService;

import java.util.List;
import java.util.Map;

public class MassInstantiateDiscountScript  extends Script{
	
	private static final String RECORD_VARIABLE_NAME = "record";
	private static final String DISCOUNT_CODE = "DISCOUNT_CODE";
	private static final String OFFER_CODE = "OFFER_CODE";
	private static final String PRODUCT_CODE = "PRODUCT_CODE";
	private static final String TARGET_TYPE = "TARGET_TYPE";
	private static final String TARGET_CODES = "TARGET_CODES";
	
	
	private DiscountPlanService discountPlanService = getServiceInterface(DiscountPlanService.class.getSimpleName());
	private ProductService productService = getServiceInterface(ProductService.class.getSimpleName());
	private BillingAccountService billingAccountService = getServiceInterface(BillingAccountService.class.getSimpleName());
	private OfferTemplateService offerTemplateService = getServiceInterface(OfferTemplateService.class.getSimpleName());
	private SubscriptionService subscriptionService = getServiceInterface(SubscriptionService.class.getSimpleName());
	private DiscountPlanInstanceService discountPlanInstanceService = getServiceInterface(DiscountPlanInstanceService.class.getSimpleName());
	
	enum  TargetType {
		SUB, BA
	}
	
	@Override
	public void execute(Map<String, Object> context) throws BusinessException {
		Map<String, Object> recordMap = (Map<String, Object>) context.get(RECORD_VARIABLE_NAME);
		if(MapUtils.isEmpty(recordMap)) {
			log.warn("The list of discount is empty!");
			return;
		}
		
		checkDiscountCode((String) recordMap.get(DISCOUNT_CODE));
		checkTargetType((String) recordMap.get(TARGET_TYPE));
		checkTaregetAll((String) recordMap.get(TARGET_TYPE), (String) recordMap.get(TARGET_CODES), (String) recordMap.get(OFFER_CODE), (String) recordMap.get(PRODUCT_CODE));
		if(TargetType.SUB.name().equals(recordMap.get(TARGET_TYPE)))
			checkIfProductIsLinkedToOffer((String) recordMap.get(OFFER_CODE), (String) recordMap.get(PRODUCT_CODE));
		processDiscount(recordMap);
	}
	
	private void checkIfProductIsLinkedToOffer(String offerCode, String productCode) {
		if(StringUtils.isBlank(offerCode) && StringUtils.isBlank(productCode)) {
			throw new BusinessException("Please provide the offer code or product code!");
		}
		if(StringUtils.isNotBlank(offerCode) && StringUtils.isNotBlank(productCode)) {
			OfferTemplate offerTemplate = offerTemplateService.findByCode(offerCode);
			if(offerTemplate == null) {
				throw new EntityDoesNotExistsException(OfferTemplate.class, offerCode);
			}
			OfferComponent offerComponent = offerTemplate.getOfferComponents().stream().filter(offerProduct -> productCode.equals(offerProduct.getProduct().getCode())).findFirst().orElse(null);
			if(offerComponent == null) {
				throw new BusinessException("The product code : " + productCode + " is not linked to the offer code : " + offerCode);
			}
		}
	}
	
	private void processDiscount(Map<String, Object> recordMap) {
		String targetType = (String) recordMap.get(TARGET_TYPE);
		String targetCodes = (String) recordMap.get(TARGET_CODES);
		String discountCode = (String) recordMap.get(DISCOUNT_CODE);
		String offerCode = (String) recordMap.get(OFFER_CODE);
		String productCode = (String) recordMap.get(PRODUCT_CODE);
		
		
		DiscountPlan discountPlan = discountPlanService.findByCode(discountCode);
		if(discountPlan == null) {
			throw new EntityDoesNotExistsException(DiscountPlan.class, discountCode);
		}
		// check if discount is activated
		if(discountPlan.getStatus() != DiscountPlanStatusEnum.ACTIVE && discountPlan.getStatus() != DiscountPlanStatusEnum.IN_USE){
			throw new BusinessException("The discount code : " + discountCode + discountPlan.getStatus().name());
		}
		Product product = productService.findByCode(productCode);
		OfferTemplate offerProductTemplate = offerTemplateService.findByCode(offerCode);
		
		processSubscriptionDiscount(TargetType.SUB.name().equals(targetType), targetCodes, discountPlan, offerProductTemplate != null ? offerProductTemplate.getCode() : null, product != null ? product.getCode() : null);
		processBillingAccountDiscount(TargetType.BA.name().equals(targetType), targetCodes, discountPlan);
	}
	
	private void processBillingAccountDiscount(boolean isBA, String targetCodes, DiscountPlan discountPlan) {
		if(!isBA) return;
		List<String> billingAccountsCode = List.of(targetCodes.split("\\|"));
		// check if the discount plan is one of PROMO_CODE, INVOICE, PRODUCT, OFFER
		List<DiscountPlanTypeEnum> discountPlanTypeList = List.of(DiscountPlanTypeEnum.PRODUCT, DiscountPlanTypeEnum.OFFER, DiscountPlanTypeEnum.PROMO_CODE, DiscountPlanTypeEnum.INVOICE);
		if(!discountPlanTypeList.contains(discountPlan.getDiscountPlanType())) {
			throw new BusinessException("The discount plan type : " + discountPlan.getDiscountPlanType().name() + " is not supported!");
		}
		billingAccountsCode.forEach( billingAccountCode -> {
			BillingAccount billingAccount = billingAccountService.findByCode(billingAccountCode);
			if(billingAccount == null) {
				throw new EntityDoesNotExistsException(BillingAccount.class, billingAccountCode);
			}
			billingAccountService.instantiateDiscountPlan(billingAccount, discountPlan);
		});
	}
	private void processSubscriptionDiscount(boolean isBA, String targetCodes, DiscountPlan discountPlan, String offerProductTemplateCode, String productCode) {
		if(!isBA) return;
		List<Subscription> subscriptions = null;
		if("ALL".equals(targetCodes)) {
				subscriptions = subscriptionService.listByOfferAndOrProduct(offerProductTemplateCode, productCode);
		} else {
			// split the target codes by pipe  and store on subscription list
			List<String> subscriptionsCode = List.of(targetCodes.split("\\|"));
			subscriptions = subscriptionService.findByListOfCodes(subscriptionsCode);
		}
		if(CollectionUtils.isEmpty(subscriptions)) {
			log.warn("No subscription found for the discount!");
			return;
		}
		subscriptions.forEach( subscription -> {
			if(productCode != null && !subscription.getOffer().getOfferComponents().stream().anyMatch(offerComponent -> productCode.equals(offerComponent.getProduct().getCode()))) {
				throw new BusinessException("The product code : " + productCode + " is not linked to the subscription : " + subscription.getCode());
			}
			if(offerProductTemplateCode != null && !subscription.getOffer().getCode().equals(offerProductTemplateCode)) {
				throw new BusinessException("The offer code : " + offerProductTemplateCode + " is not linked to the subscription : " + subscription.getCode());
			}
			if(CollectionUtils.isNotEmpty(subscription.getDiscountPlanInstances())){
				throw new BusinessException("The subscription : " + subscription.getCode() + " already has a discount plan instance!");
			}
			if(discountPlan.getDiscountPlanType() == DiscountPlanTypeEnum.PRODUCT && productCode != null) {
				subscription.getServiceInstances().forEach(serviceInstance -> discountPlanInstanceService.instantiateDiscountPlan(serviceInstance, discountPlan, null, false));
			}if(discountPlan.getDiscountPlanType() == DiscountPlanTypeEnum.OFFER && offerProductTemplateCode != null) {
				discountPlanInstanceService.instantiateDiscountPlan(subscription, discountPlan, null, false);
			}else{
				throw new BusinessException("The discount plan type : " + discountPlan.getDiscountPlanType().name() + " is not supported!");
			}
		});
	}
	
	/**
	 * Check if the target type is BA and the target codes is "ALL" then throw an exception.
	 * check if the target type is SUB and the target codes is "ALL" and the offer code or product code is null then throw an exception.
	 * @param targetType the target type
	 * @param targetCodes the target codes
	 */
	private void checkTaregetAll(String targetType, String targetCodes, String offerCode, String productCode) {
		if(TargetType.BA.name().equals(targetType) && "ALL".equals(targetCodes)) {
			throw new BusinessException("The target type is BA and the target codes is ALL, this combination is prohibited and rejected directly!");
		}
		if(TargetType.SUB.name().equals(targetType) && "ALL".equals(targetCodes)) {
			if(StringUtils.isBlank(offerCode) && StringUtils.isBlank(productCode)) {
				throw new BusinessException("The target type is SUB and the target codes is ALL, please provide the offer code or product code!");
			}
		}
	}
	
	private void checkTargetType(String targetType) {
		if(StringUtils.isBlank(targetType)) {
			throw new BusinessException("Target type is required!");
		}
		if(!TargetType.SUB.name().equals(targetType) && !TargetType.BA.name().equals(targetType)) {
			throw new BusinessException("Target type must be SUB or BA!");
		}
	}
	
	private void checkDiscountCode(String discountCode) {
		if(StringUtils.isBlank(discountCode)) {
			throw new BusinessException("Discount code is required!");
		}
		DiscountPlan discountPlan = discountPlanService.findByCode(discountCode);
		if(discountPlan == null) {
			throw new EntityDoesNotExistsException(DiscountPlan.class, discountCode);
		}
		if(discountPlan.getStatus() != DiscountPlanStatusEnum.ACTIVE && discountPlan.getStatus() != DiscountPlanStatusEnum.IN_USE) {
			throw new BusinessException("the discount code : " + discountCode + "  must be active!");
		}
	}
}
