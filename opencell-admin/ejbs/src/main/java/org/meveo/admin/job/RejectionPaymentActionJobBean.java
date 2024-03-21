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

import static java.util.Optional.of;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.payments.PaymentRejectionAction;
import org.meveo.model.payments.PaymentRejectionActionReport;
import org.meveo.model.payments.PaymentRejectionActionStatus;
import org.meveo.model.payments.RejectedPayment;
import org.meveo.model.payments.RejectionActionStatus;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.script.ScriptInterface;

/**
 * Job implementation to process pending rejectedPayment
 * 
 */
@Stateless
public class RejectionPaymentActionJobBean extends IteratorBasedJobBean<RejectedPayment> {

	private static final long serialVersionUID = 1L;

    @Inject
    private AccountOperationService accountOperationService;
    
    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, this::processRejectedPayment, null, null, null);
    }

    /**
     * Initialize job settings and retrieve data to process
     * 
     * @param jobExecutionResult Job execution result
     * @return An iterator over a list of RejectedPayment
     */
    private Optional<Iterator<RejectedPayment>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        List<RejectedPayment> rejectedPayments = accountOperationService.findRejectedPaymentByStatus(RejectionActionStatus.PENDING);
        return of(new SynchronizedIterator<>(rejectedPayments));
    }

    /**
     * process a rejected payment having rejectionActionsStatus PENDING
     * 
     * @param rejectedPayment
     * @param jobExecutionResult Job execution result
     */
    private void processRejectedPayment(RejectedPayment rejectedPayment, JobExecutionResultImpl jobExecutionResult) {
    	boolean processNextAction = true;
    	rejectedPayment.setRejectionActionsStatus(RejectionActionStatus.RUNNING);
    	List<PaymentRejectionActionReport> rejectionActionsReport = rejectedPayment.getPaymentRejectionActionReports();
    	rejectionActionsReport.sort((a,b) -> a.getAction().getSequence() - b.getAction().getSequence());
    	for (PaymentRejectionActionReport actionReport :  rejectionActionsReport) {
    		if (processNextAction) {
    			processPaymentRejectionActionReport(actionReport);
				processNextAction = !(PaymentRejectionActionStatus.FAILED.equals(actionReport.getStatus()));
    		} else {
    			actionReport.setStatus(PaymentRejectionActionStatus.CANCELED);
    			actionReport.setEndDate(new Date());
    		}
    	}

    	if (processNextAction) {
    		rejectedPayment.setRejectionActionsStatus(RejectionActionStatus.COMPLETED);
    	} else {
    		rejectedPayment.setRejectionActionsStatus(RejectionActionStatus.FAILED);
    	}
    }
    
	private void processPaymentRejectionActionReport(PaymentRejectionActionReport actionReport) {
		// initialisation context
		Map<String, Object> methodContext = new HashMap<>();
		methodContext.put(Script.CONTEXT_CURRENT_USER, currentUser);
		methodContext.put(Script.CONTEXT_APP_PROVIDER, appProvider);
		
		actionReport.setStartDate(new Date());
		actionReport.setStatus(PaymentRejectionActionStatus.RUNNING);
		try {
			ScriptInterface rejectionPaymentActionScript = injectScriptParams(methodContext, actionReport.getAction());
			rejectionPaymentActionScript.execute(methodContext);
			actionReport.setStatus((Boolean) methodContext.get(Script.REJECTION_ACTION_RESULT) ? PaymentRejectionActionStatus.COMPLETED	: PaymentRejectionActionStatus.FAILED);
			actionReport.setReport((String) methodContext.get(Script.REJECTION_ACTION_REPORT));
		} catch (Exception e) {
			actionReport.setStatus(PaymentRejectionActionStatus.FAILED);
			actionReport.setReport(e.getMessage());
		}
		actionReport.setEndDate(new Date());
	}
	
	private ScriptInterface injectScriptParams(Map<String, Object> methodContext, PaymentRejectionAction action) {
		ScriptInstance scriptInstance = scriptInstanceService.refreshOrRetrieve(action.getScript());
		if (scriptInstance != null && !MapUtils.isEmpty(action.getScriptParameters())) {
			scriptInstance.getScriptParameters().stream().forEach(sp -> {
				if (action.getScriptParameters().containsKey(sp.getCode())) {
					methodContext.put(sp.getCode(),
							(sp.isCollection())
									? scriptInstanceService.parseListFromString(String.valueOf(action.getScriptParameters().get(sp.getCode())), sp.getClassName(), sp.getValuesSeparator())
									: scriptInstanceService.parseObjectFromString(String.valueOf(action.getScriptParameters().get(sp.getCode())), sp.getClassName()));
				}
			});
		}

		return scriptInstanceService.getScriptInstance(scriptInstance.getCode());
	}

}