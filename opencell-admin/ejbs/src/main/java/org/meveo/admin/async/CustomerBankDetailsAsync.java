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
package org.meveo.admin.async;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.billing.BankCoordinates;
import org.meveo.model.jaxb.customer.bankdetails.Modification;
import org.meveo.model.payments.DDPaymentMethod;
import org.meveo.model.payments.MandateChangeAction;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.service.payments.impl.PaymentMethodService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Class CustomerBankDetailsAsync.
 *
 * @author anasseh
 */

@Stateless
public class CustomerBankDetailsAsync {

	/** The param bean factory. */
	@Inject
	protected ParamBeanFactory paramBeanFactory;
	
    @Inject
    private PaymentMethodService paymentMethodService;
    
    protected Logger log = LoggerFactory.getLogger(this.getClass());
	
	
    private int nbModifications;
    private int nbModificationsError;
    private int nbModificationsIgnored;
    private int nbModificationsCreated;
    private String msgModifications="";

	
	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Future<Map<String,Object>> launchAndForgetImportDeatails( List<Modification> modificationList) throws BusinessException,Exception {
				
		Map<String,Object> result = new HashMap<String, Object>();
		initialiserCompteur();

		for (Modification newModification : modificationList) {
            //IBAN du client et BIC dans l'établissement de départ
            String ibanDepart = newModification.getOrgPartyAndAccount().getAccount().getiBAN();
            String encryptIbanDepart = encryptIban(ibanDepart);
            String bicDepart = newModification.getOrgPartyAndAccount().getAgent().getFinInstnId().getBicFi();
            //IBAN du client et BIC dans l'établissement d'arrivée
            String ibanArrivee = newModification.getUpdatedPartyAndAccount().getAccount().getiBAN();
            String encryptIbanArrivee = encryptIban(ibanArrivee);
            String bicArrivee = newModification.getUpdatedPartyAndAccount().getAgent().getFinInstnId().getBicFi();
            log.debug("(ibanDepart: [{}] ibanDepart: [{}] ibanDepart: [{}] ibanDepart: [{}])", ibanDepart, bicDepart, ibanArrivee, bicArrivee);
            List<PaymentMethod> paymentMethods = paymentMethodService.listByIbanAndBicFi(encryptIbanDepart, bicDepart, false);
            log.debug("paymentMethodsDepart.size(): {}", paymentMethods.size());
            List<PaymentMethod> paymentMethodsArrivee = paymentMethodService.listByIbanAndBicFi(encryptIbanArrivee, bicArrivee);
            log.debug("paymentMethodsArrivee.size(): {}", paymentMethodsArrivee.size());
            dupPmDepartArrivee(ibanDepart, bicDepart, ibanArrivee, bicArrivee, paymentMethods, paymentMethodsArrivee);
        }
			
		result.put("nbModifications",nbModifications);
		result.put("nbModificationsError",nbModificationsError);
		result.put("nbModificationsIgnored",nbModificationsIgnored);
		result.put("nbModificationsCreated",nbModificationsCreated);
		result.put("msgModifications",msgModifications);

		return new AsyncResult<Map<String,Object>>(result);
		
	}

	private String encryptIban(String ibanDepart) throws Exception {
		BankCoordinates bankCoordinates = new BankCoordinates();
		
		return bankCoordinates.encryptIban(bankCoordinates.encryptIban(ibanDepart));
	}
	
	private void dupPmDepartArrivee(String ibanDepart, String bicDepart, String ibanArrivee, String bicArrivee,
			List<PaymentMethod> paymentMethodsDepart, List<PaymentMethod> paymentMethodsArrivee)
			throws CloneNotSupportedException {

		for (PaymentMethod paymentMethod : paymentMethodsDepart) {
			dupDDPaymentMethode(ibanArrivee, bicArrivee, paymentMethod);
			nbModificationsCreated++;
			log.debug("(ibanDepart: [{}] ibanDepart: [{}] ibanDepart: [{}] ibanDepart: [{}] - OK)", ibanDepart,
					bicDepart, ibanArrivee, bicArrivee);
		}
		if (paymentMethodsDepart.isEmpty()) {
			nbModificationsIgnored++;
			msgModifications += "[(Warning) Original bank account (iban=" + ibanDepart + "; bic=" + bicDepart
					+ ") does not exist in opencell]  ";
			log.debug("(Warning) Original bank account iban: [{}] bic: [{}] does not exist in opencell..", ibanDepart,
					bicDepart);
		}
	}
	
	  private void dupDDPaymentMethode(String ibanArrivee, String bicArrivee, PaymentMethod paymentMethod) throws CloneNotSupportedException {
	        DDPaymentMethod dDPaymentMethod = (DDPaymentMethod) paymentMethod;
	        DDPaymentMethod newDDPaymentMethod = dDPaymentMethod.copieDDPaymentMethod();              
	        newDDPaymentMethod.getBankCoordinates().setIban(ibanArrivee);
	        newDDPaymentMethod.getBankCoordinates().setBic(bicArrivee); 
	        
	        if (newDDPaymentMethod.getBankCoordinates() != null && ((DDPaymentMethod) paymentMethod).getBankCoordinates() == null) {
	            newDDPaymentMethod.setMandateChangeAction(MandateChangeAction.TO_ADVERTISE);
	        } else if (newDDPaymentMethod.getBankCoordinates() != null && ((DDPaymentMethod) paymentMethod).getBankCoordinates() != null
	                && !newDDPaymentMethod.getBankCoordinates().getIban().equals(((DDPaymentMethod) paymentMethod).getBankCoordinates().getIban())) {
	            newDDPaymentMethod.setMandateChangeAction(MandateChangeAction.TO_ADVERTISE);
	        }
	        paymentMethodService.create(newDDPaymentMethod);
	        
	        paymentMethod.setPreferred(false);            
	        paymentMethod.setDisabled(true);
	        paymentMethodService.update(paymentMethod);
	    }
	  
	  private void initialiserCompteur() {
	        nbModifications = 0;
	        nbModificationsError = 0;
	        nbModificationsIgnored = 0;
	        nbModificationsCreated = 0;
	        msgModifications = "";
	    }
	
}
