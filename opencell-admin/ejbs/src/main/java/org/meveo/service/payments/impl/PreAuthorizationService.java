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
package org.meveo.service.payments.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

import org.meveo.model.payments.CardPaymentMethod;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.PaymentGateway;
import org.meveo.model.payments.PreAuthorization;
import org.meveo.model.payments.PreAuthorizationStatusEnum;
import org.meveo.service.base.PersistenceService;

/**
 * PreAuthorization service implementation.
 * 
 * @author anasseh
 */
@Stateless
public class PreAuthorizationService extends PersistenceService<PreAuthorization> {

    /**
     * Gets preAuthorization to cancel.
     * @param dateToCancel
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PreAuthorization> getPreAuthorizationToCancel(Date dateToCancel) {
        try {
            return (List<PreAuthorization>) getEntityManager().createNamedQuery("PreAuthorization.listToCancel")
                .setParameter("dateToCancelIN", dateToCancel).getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }
    /**
     * Get PreAuthorization To Capture.
     * 
     * @param card
     * @return
     */
    public PreAuthorization getPreAuthorizationToCapture(CardPaymentMethod card) {
        try {
            return (PreAuthorization) getEntityManager().createNamedQuery("PreAuthorization.PaToCapture")
                .setParameter("cardPmIdIN", card.getId()).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public void cancel(PreAuthorization preAuthorization) {
    	 preAuthorization.setStatus(PreAuthorizationStatusEnum.CANCELED);
		 preAuthorization.setCancelDate(new Date());
		 update(preAuthorization);
    }
    public void create(BigDecimal amount,PaymentGateway paymentGateway,CardPaymentMethod cardPaymentMethod,CustomerAccount customerAccount,String paymentId) {
    	PreAuthorization preAuthorization = new PreAuthorization();
    	preAuthorization.setAmount(amount);
    	preAuthorization.setPaymentGateway(paymentGateway);
    	preAuthorization.setReference(paymentId);
    	preAuthorization.setStatus(PreAuthorizationStatusEnum.AUTORISED);
    	preAuthorization.setCardPaymentMethod(cardPaymentMethod);
    	preAuthorization.setTransactionDate(new Date());
    	preAuthorization.setCustomerAccount(customerAccount);
    	create(preAuthorization);
    }
    
    public void capture(PreAuthorization preAuthorization,BigDecimal amountCaptured) {
   	     preAuthorization.setStatus(PreAuthorizationStatusEnum.CAPTURED);
		 preAuthorization.setCaptureDate(new Date());
		 preAuthorization.setAmountCaptured(amountCaptured == null ? preAuthorization.getAmount() : amountCaptured);
		 update(preAuthorization);    	
    }
    public void rejectCapture(PreAuthorization preAuthorization,BigDecimal amountCaptured) {
  	     preAuthorization.setStatus(PreAuthorizationStatusEnum.REJECTED);
		 preAuthorization.setCaptureDate(new Date());
		 preAuthorization.setAmountCaptured(amountCaptured == null ? preAuthorization.getAmount() : amountCaptured);
		 update(preAuthorization);    	
   }
    
}
