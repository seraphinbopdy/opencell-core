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

package org.meveo.admin.job;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;


import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.PreAuthorization;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.payments.impl.PaymentService;
import org.meveo.service.payments.impl.PreAuthorizationService;

/**
 * Job implementation to cancel a preAuthorization after X days..
 * 
 * @author anasseh
 */
@Stateless
public class CancelPreAuthorizationJobBean extends IteratorBasedJobBean<PreAuthorization> {

    private static final long serialVersionUID = -1117556720357725851L;

    /** The PreAuthorization Service. */
    @Inject
    private PreAuthorizationService preAuthorizationService;
    
    
    @Inject
    private PaymentService paymentService;

    /**
     * Number days to cancel preAuthorization - Job execution parameter
     */
    private Long nbDays = 7L;

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, this::cancelPreAuthorization, null, null, null);       
    }

    /**
     * Initialize job settings and retrieve data to process
     * 
     * @param jobExecutionResult Job execution result
     * @return An iterator over a list of Account operation ids
     */
    private Optional<Iterator<PreAuthorization>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        nbDays = 7L;

        JobInstance jobInstance = jobExecutionResult.getJobInstance();
        try{
        	nbDays = (Long) this.getParamOrCFValue(jobInstance, "CancelPreAuthorizationJob_nbDays");
             
        } catch (Exception e) {
            log.warn("Cant get customFields for " + jobInstance.getJobTemplate(), e.getMessage());
        }

        if (nbDays == null) {
        	nbDays = 7L;
        }
        
        List<PreAuthorization> pas = preAuthorizationService.getPreAuthorizationToCancel(DateUtils.addDaysToDate(new Date(), nbDays.intValue()*-1));

        
        return Optional.of(new SynchronizedIterator<PreAuthorization>(pas));
    }

    /**
     * cancelPreAuthorization
     * 
     * @param cancelPreAuthorizationItem
     * @param jobExecutionResult Job execution result
     */
    private void cancelPreAuthorization(PreAuthorization preAuthorization, JobExecutionResultImpl jobExecutionResult) {       
        try {
             paymentService.cancelPayment(preAuthorizationService.findById(preAuthorization.getId(), Arrays.asList("paymentGateway","cardPaymentMethod")));            
        } catch (Exception e) {
        	log.error(" Error on cancelPreAuthorization [{}]", e.getMessage());        	
        	 jobExecutionResult.unRegisterSucces();// Reduce success as success is added automatically in main loop of IteratorBasedJobBean
             jobExecutionResult.registerError(preAuthorization.getReference(), e.getMessage());

        }
    }

   
}