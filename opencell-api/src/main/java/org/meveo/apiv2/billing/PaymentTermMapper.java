package org.meveo.apiv2.billing;

import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.api.dto.invoice.ImmutablePaymentTerm;
import org.meveo.apiv2.generic.ResourceMapper;
import org.meveo.model.payments.PaymentTerm;
import org.meveo.service.billing.impl.TradingLanguageService;

import static org.meveo.commons.utils.EjbUtils.getServiceInterface;

public class PaymentTermMapper extends ResourceMapper<org.meveo.api.dto.invoice.PaymentTerm, PaymentTerm> {
	
	private TradingLanguageService tradingLanguageService = (TradingLanguageService) getServiceInterface(TradingLanguageService.class.getSimpleName());;
	@Override
	public org.meveo.api.dto.invoice.PaymentTerm toResource(PaymentTerm entity) {
		return ImmutablePaymentTerm.builder()
				.code(entity.getCode())
				.description(entity.getDescription())
				.languageDescriptions(LanguageDescriptionDto.convertMultiLanguageFromMapOfValues(entity.getDescriptionI18n()))
				.build();
	}
	
	@Override
	public PaymentTerm toEntity(org.meveo.api.dto.invoice.PaymentTerm resource) {
		PaymentTerm entity = new PaymentTerm();
		entity.setCode(resource.getCode());
		entity.setDescription(resource.getDescription());
		entity.setDescriptionI18n(LanguageDescriptionDto.convertMultiLanguageToMapOfValues(resource.getLanguageDescriptions(), null, tradingLanguageService.listLanguageCodes()));
		return entity;
	}
}
