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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.billing.BatchEntity;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.ThresholdOptionsEnum;
import org.meveo.model.crm.Provider;
import org.meveo.service.base.PersistenceService;
import org.meveo.util.ApplicationProvider;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

/**
 * A class for invoicing threshold persistence services.
 *
 * @author Abdellatif BARI
 * @since 16.0.0
 */
@Stateless
public class InvoicingThresholdService extends PersistenceService<BatchEntity> {

    @Inject
    @ApplicationProvider
    protected Provider appProvider;

    @Inject
    private InvoiceLineService invoiceLineService;

    @Inject
    private RejectedBillingAccountService rejectedBillingAccountService;

    @Inject
    private RatedTransactionService ratedTransactionService;

    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void applyThresholdByInvoice(BillingRun billingRun, Set<Long> billingAccountsIds,
			BigDecimal invoicingThreshold, ThresholdOptionsEnum checkThreshold) {
            List<Object[]> ids; // List of array of billingAccountsIds and invoiceLinesIds
		ids = invoiceLineService.checkThreshold(billingRun.getId(), invoicingThreshold, checkThreshold, appProvider.isEntreprise());
		String billingAccountReason = "Billing account did not reach invoicing threshold";
            Set<Long> invoiceLinesIds = new HashSet<>();
            ids.forEach(item -> {
                BigInteger billingAccountId = (BigInteger) item[0];
                String ilIds = (String) item[1];
                if (billingAccountId != null) {
                    billingAccountsIds.add(billingAccountId.longValue());
                }
                if (!StringUtils.isBlank(ilIds)) {
                    invoiceLinesIds.addAll(stream((ilIds).split(",")).map(Long::parseLong).collect(toSet()));
                }
            });
            if (!invoiceLinesIds.isEmpty()) {
                // reopen invoice lines RTs
                ratedTransactionService.detachRatedTransactions(invoiceLinesIds);
                // cancel invoice lines
                invoiceLineService.cancelInvoiceLines(invoiceLinesIds);
            }
		/* Create rejected billing accounts for invoice and each entity (BA, CA, C) */
            billingAccountsIds.forEach(baIdToReject -> {
                BillingAccount ba = getEntityManager().getReference(BillingAccount.class, baIdToReject);
                rejectedBillingAccountService.create(ba, getEntityManager().getReference(BillingRun.class, billingRun.getId()), billingAccountReason);
            });
        }

    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void applyThresholdByEntity(BillingRun billingRun, Set<Long> billingAccountsIds, String entityLevel) {
		// Check the threshold by billing account
		List<Object> idsListAsString=invoiceLineService.checkThreshold(billingRun.getId(), appProvider.isEntreprise(), entityLevel);
		
		Set<Long> currentBillingAccountsIds = "ByBA".equals(entityLevel) ? new HashSet<>((List<Long>) (List<?>) idsListAsString)
			    : idsListAsString.stream().flatMap(str -> Arrays.stream(((String) str).split(","))).map(Long::valueOf).collect(Collectors.toSet());
		
		String billingAccountReason = "Billing account did not reach invoicing threshold "+entityLevel;
		//applyThreshold by entity (BA, CA, C) 
		if (!currentBillingAccountsIds.isEmpty()) {
		    // reopen billing accounts RTs
		    ratedTransactionService.reopenRTs(billingRun.getId(), currentBillingAccountsIds);
		    // cancel invoice lines by billing accounts and billing run
		    invoiceLineService.cancelInvoiceLines(billingRun.getId(), currentBillingAccountsIds);
		    currentBillingAccountsIds.removeAll(billingAccountsIds);
		    currentBillingAccountsIds.forEach(baIdToReject -> {
                BillingAccount ba = getEntityManager().getReference(BillingAccount.class, baIdToReject);
                rejectedBillingAccountService.create(ba, getEntityManager().getReference(BillingRun.class, billingRun.getId()), billingAccountReason);
            });
		    billingAccountsIds.addAll(currentBillingAccountsIds);
    }
	}

}