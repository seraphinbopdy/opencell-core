package org.meveo.api.invoice;

import org.apache.commons.collections.MapUtils;
import org.meveo.api.BaseApi;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.security.Interceptor.SecuredBusinessEntityMethodInterceptor;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.payments.PaymentTerm;
import org.meveo.service.billing.impl.PaymentTermService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

@Stateless
@Interceptors(SecuredBusinessEntityMethodInterceptor.class)
public class PaymentTermApi extends BaseApi {
	
	@Inject
	private PaymentTermService paymentTermService;

	public PaymentTerm create(PaymentTerm postData) {
		if(StringUtils.isBlank(postData.getCode())){
			missingParameters.add("code");
		}
		if(StringUtils.isBlank(postData.getDescription())){
			missingParameters.add("description");
		}
		handleMissingParameters();
		if(paymentTermService.findBusinessEntityByCode(postData.getCode()) != null){
			throw new EntityAlreadyExistsException(PaymentTerm.class, postData.getCode());
		}
		paymentTermService.create(postData);
		return postData;
	}
	
	public PaymentTerm update(PaymentTerm postData, String paymentTermCode) {
		PaymentTerm paymentTerm = paymentTermService.findByCode(paymentTermCode);
		if(paymentTerm == null){
			throw new EntityDoesNotExistsException(PaymentTerm.class, postData.getCode());
		}
		if(StringUtils.isBlank(postData.getCode())){
			missingParameters.add("code");
		}
		if(StringUtils.isBlank(postData.getDescription())){
			missingParameters.add("description");
		}
		handleMissingParameters();
		paymentTerm.setDescription(postData.getDescription());
		if(MapUtils.isNotEmpty(postData.getDescriptionI18n())){
			paymentTerm.setDescriptionI18n(postData.getDescriptionI18n());
		}
		paymentTermService.update(paymentTerm);
		return paymentTerm;
	}
	
	public PaymentTerm find(String paymentTermCode) throws EntityDoesNotExistsException {
		return findByCode(paymentTermCode);
	}
	
	public void enableOrDisable(String paymentTermCode, boolean enable) throws EntityDoesNotExistsException {
		PaymentTerm paymentTerm = (PaymentTerm) paymentTermService.findBusinessEntityByCode(paymentTermCode);
		if(paymentTerm == null){
			throw new EntityDoesNotExistsException(PaymentTerm.class, paymentTermCode);
		}
		paymentTerm.setDisabled(!enable);
		paymentTermService.update(paymentTerm);
	}
	
	public void delete(String paymentTermCode) throws EntityDoesNotExistsException {
		PaymentTerm paymentTerm = findByCode(paymentTermCode);
		paymentTermService.remove(paymentTerm);
	}
	
	private PaymentTerm findByCode(String paymentTermCode) {
		PaymentTerm paymentTerm =  paymentTermService.findByCode(paymentTermCode);
		if(paymentTerm == null){
			throw new EntityDoesNotExistsException(PaymentTerm.class, paymentTermCode);
		}
		return paymentTerm;
	}
	
}
