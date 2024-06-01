package org.meveo.service.script;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.catalog.DiscountPlanStatusEnum;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.catalog.impl.DiscountPlanService;
import org.meveo.service.catalog.impl.OfferTemplateService;

import java.util.Map;

public class MassInstantiateDiscountScript  extends Script{
	
	private static final String RECORD_VARIABLE_NAME = "record";
	private static final String DISCOUNT_CODE = "DISCOUNT_CODE";
	private static final String OFFER_CODE = "OFFER_CODE";
	private static final String PRODUCT_CODE = "PRODUCT_CODE";
	private static final String TARGET_TYPE = "TARGET_TYPE";
	private static final String TARGET_CODES = "TARGET_CODES";
	
	
	private DiscountPlanService discountPlanService = getServiceInterface(DiscountPlanService.class.getSimpleName());
	private BillingAccountService billingAccountService = getServiceInterface(BillingAccountService.class.getSimpleName());
	private OfferTemplateService offerTemplateService = getServiceInterface(OfferTemplateService.class.getSimpleName());
	private SubscriptionService subscriptionService = getServiceInterface(SubscriptionService.class.getSimpleName());
	
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
		if(discountPlan.getStatus() != DiscountPlanStatusEnum.ACTIVE) {
			throw new BusinessException("the discount code : " + discountCode + "  must be active!");
		}
	}
}
