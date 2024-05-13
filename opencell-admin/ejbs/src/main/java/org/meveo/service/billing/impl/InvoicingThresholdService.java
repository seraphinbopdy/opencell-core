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

import org.meveo.commons.utils.StringUtils;
import org.meveo.model.billing.BatchEntity;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingCycle;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.ThresholdOptionsEnum;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.service.base.PersistenceService;
import org.meveo.util.ApplicationProvider;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

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
    private BillingRunService billingRunService;

    @Inject
    private InvoiceLineService invoiceLineService;

    @Inject
    private RejectedBillingAccountService rejectedBillingAccountService;

    @Inject
    private RatedTransactionService ratedTransactionService;

    /**
     * Apply threshold
     *
     * @param jobExecutionResult the job execution result
     */
    public void applyThreshold(JobExecutionResultImpl jobExecutionResult) {
        List<BillingRun> billingRuns = billingRunService.getBillingRuns(BillingRunStatusEnum.INVOICE_LINES_CREATED);
        for (BillingRun billingRun : billingRuns) {
            applyThreshold(jobExecutionResult, billingRun);
        }
    }


    /**
     * Apply threshold
     *
     * @param jobExecutionResult he job execution result
     * @param billingRun         the billingRun
     */
    private void applyThreshold(JobExecutionResultImpl jobExecutionResult, BillingRun billingRun) {
        try {
            List<Object[]> ids; // List of array of billingAccountsIds and invoiceLinesIds
            Set<Long> billingAccountsIds = new HashSet<>();
            Set<Long> invoiceLinesIds = new HashSet<>();
            BillingCycle billingCycle = billingRun.getBillingCycle();
            BigDecimal invoicingThreshold = billingCycle != null ? billingCycle.getInvoicingThreshold() : null;
            ThresholdOptionsEnum checkThreshold = billingCycle != null ? billingCycle.getCheckThreshold() : null;
            String billingAccountReason = "Billing account did not reach invoicing threshold";

            // Provider is in B2B mode
            if (appProvider.isEntreprise()) {
                // Check the threshold by invoice
                ids = invoiceLineService.checkThresholdB2B(billingRun.getId(), invoicingThreshold, checkThreshold);
                // Check the threshold by billing account
                billingAccountsIds.addAll(invoiceLineService.checkThresholdB2BByBA(billingRun.getId()));
                // Check the threshold by customer account
                String baIds = invoiceLineService.checkThresholdB2BByCA(billingRun.getId());
                if (!StringUtils.isBlank(baIds)) {
                    billingAccountsIds.addAll(stream((baIds).split(",")).map(Long::parseLong).collect(toSet()));
                }
                // Check the threshold by customer
                baIds = invoiceLineService.checkThresholdB2BByC(billingRun.getId());
                if (!StringUtils.isBlank(baIds)) {
                    billingAccountsIds.addAll(stream((baIds).split(",")).map(Long::parseLong).collect(toSet()));
                }
            } else { // Provider is in B2C mode
                // Check the threshold by invoice
                ids = invoiceLineService.checkThresholdB2C(billingRun.getId(), invoicingThreshold, checkThreshold);
                // Check the threshold by billing account
                billingAccountsIds.addAll(invoiceLineService.checkThresholdB2CByBA(billingRun.getId()));
                // Check the threshold by customer account
                String baIds = invoiceLineService.checkThresholdB2CByCA(billingRun.getId());
                if (!StringUtils.isBlank(baIds)) {
                    billingAccountsIds.addAll(stream((baIds).split(",")).map(Long::parseLong).collect(toSet()));
                }
                // Check the threshold by customer
                baIds = invoiceLineService.checkThresholdB2CByC(billingRun.getId());
                if (!StringUtils.isBlank(baIds)) {
                    billingAccountsIds.addAll(stream((baIds).split(",")).map(Long::parseLong).collect(toSet()));
                }
            }

            /* 1- applyThreshold by entity (BA, CA, C) */
            if (!billingAccountsIds.isEmpty()) {
                // reopen billing accounts RTs
                ratedTransactionService.reopenRTs(billingRun.getId(), billingAccountsIds);
                // cancel invoice lines by billing accounts and billing run
                invoiceLineService.cancelInvoiceLines(billingRun.getId(), billingAccountsIds);
            }


            /* 2- applyThreshold by invoice */
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

            /* 3- Create rejected billing accounts for invoice and each entity (BA, CA, C) */
            // Create rejected billing accounts
            billingAccountsIds.forEach(baIdToReject -> {
                BillingAccount ba = getEntityManager().getReference(BillingAccount.class, baIdToReject);
                rejectedBillingAccountService.create(ba, getEntityManager().getReference(BillingRun.class, billingRun.getId()), billingAccountReason);
            });
            jobExecutionResult.registerSucces(billingAccountsIds.size());
        } catch (Exception e) {
            log.error("Failed to apply threshold for the billingRun id : {}", billingRun.getId(), e);
            jobExecutionResult.registerError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

}