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

import static java.lang.Long.valueOf;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.job.utils.BillinRunApplicationElFilterUtils;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.model.billing.BillingCycle;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunReportTypeEnum;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.ThresholdOptionsEnum;
import org.meveo.model.crm.EntityReferenceWrapper;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.BillingRunReportService;
import org.meveo.service.billing.impl.BillingRunService;
import org.meveo.service.billing.impl.InvoicingThresholdService;
import org.primefaces.model.SortOrder;

/**
 * A job implementation to apply the threshold rules for the invoice, billing account, customer account and customer.
 *
 * @author Abdellatif BARI
 * @since 16.0.0
 */
@Stateless
public class InvoicingThresholdJobBean extends BaseJobBean {

    private static final long serialVersionUID = 1L;
    
    public static final String FIELD_PRIORITY_SORT = "billingCycle.priority, auditable.created";

    @Inject
    private InvoicingThresholdService invoicingThresholdService;
    
    @Inject
    private BillingRunReportService billingRunReportService;
    
    @Inject
    private BillingRunService billingRunService;

    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
    	List<BillingRun> billingRuns = getBillingRunsToProcess(jobInstance, jobExecutionResult);
        applyThreshold(jobExecutionResult, billingRuns);
        if (!billingRuns.isEmpty()) {
        	billingRunReportService.resetFinalStatus(billingRuns.stream().map(BillingRun::getId).collect(Collectors.toList()), List.of(BillingRunReportTypeEnum.OPEN_INVOICE_LINES, BillingRunReportTypeEnum.OPEN_RATED_TRANSACTIONS, BillingRunReportTypeEnum.BILLED_RATED_TRANSACTIONS));
        }
        
    }
    
    
    private List<BillingRun> getBillingRunsToProcess(JobInstance jobInstance, JobExecutionResultImpl jobExecutionResult) {

        List<EntityReferenceWrapper> billingRunWrappers = (List<EntityReferenceWrapper>) this.getParamOrCFValue(jobInstance, InvoicingThresholdJob.BILLING_RUNS);
        List<Long> billingRunIds = billingRunWrappers != null ? billingRunWrappers.stream().map(br -> valueOf(br.getCode().split("/")[0])).collect(toList()) : emptyList();
        List<BillingRunStatusEnum> billingRunStatus = asList(BillingRunStatusEnum.INVOICE_LINES_CREATED);
        Map<String, Object> filters = new HashMap<>();
        if (billingRunIds.isEmpty()) {
            filters.put("inList status", billingRunStatus);
        } else {
            filters.put("inList id", billingRunIds);
        }
        PaginationConfiguration pagination = new PaginationConfiguration(null, null, filters,
                null, asList("billingCycle"), FIELD_PRIORITY_SORT, SortOrder.ASCENDING);

        List<BillingRun> billingRuns = BillinRunApplicationElFilterUtils.filterByApplicationEL(billingRunService.list(pagination), jobInstance);

        // Extra validation of BR status when billing run list is provided as parameters
        if (!billingRunIds.isEmpty() && !billingRuns.isEmpty()) {
            List<BillingRun> excludedBRs = billingRuns.stream().filter(br -> br.getStatus() != BillingRunStatusEnum.INVOICE_LINES_CREATED).collect(toList());
            excludedBRs.forEach(br -> jobExecutionResult.registerWarning(format("BillingRun[id={%d}] has been ignored as it's not in INVOICE_LINES_CREATED status", br.getId())));
            billingRuns.removeAll(excludedBRs);
            if (billingRuns.isEmpty()) {
                jobExecutionResult.registerError("No valid billing run with status = INVOICE_LINES_CREATED");
            }
        }

        return billingRuns;
    }
    
    /**
     * Apply threshold
     *
     * @param jobExecutionResult the job execution result
     */
    public void applyThreshold(JobExecutionResultImpl jobExecutionResult, List<BillingRun> billingRuns) {
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
    public void applyThreshold(JobExecutionResultImpl jobExecutionResult, BillingRun billingRun) {
        try {
            
            Set<Long> billingAccountsIds = new HashSet<>();
            
            BillingCycle billingCycle = billingRun.getBillingCycle();
            BigDecimal invoicingThreshold = billingCycle != null ? billingCycle.getInvoicingThreshold() : null;
            ThresholdOptionsEnum checkThreshold = billingCycle != null ? billingCycle.getCheckThreshold() : null;
            

            // Check the threshold by invoice
            invoicingThresholdService.applyThresholdByInvoice(billingRun, billingAccountsIds, invoicingThreshold, checkThreshold);
            
            // Apply the threshold by billing account
            invoicingThresholdService.applyThresholdByEntity(billingRun, billingAccountsIds,"ByBA");
            // Check the threshold by customer account
            invoicingThresholdService.applyThresholdByEntity(billingRun, billingAccountsIds,"ByCA");
            // Check the threshold by customer
            invoicingThresholdService. applyThresholdByEntity(billingRun, billingAccountsIds,"ByC");
            
            jobExecutionResult.registerSucces(billingAccountsIds.size());
        } catch (Exception e) {
            log.error("Failed to apply threshold for the billingRun id : {}", billingRun.getId(), e);
            jobExecutionResult.registerError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }
}