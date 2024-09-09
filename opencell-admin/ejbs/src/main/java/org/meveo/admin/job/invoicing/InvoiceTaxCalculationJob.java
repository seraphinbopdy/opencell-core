package org.meveo.admin.job.invoicing;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.utils.CustomFieldTemplateUtils;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.service.job.Job;
import org.meveo.service.job.ScopedJob;


@Stateless
public class InvoiceTaxCalculationJob extends ScopedJob {


    public static final String INVOICE_TAX_CALCULATION_JOB = "JobInstance_InvoiceTaxCalculationJob";
    public static final String CF_TAX_CALCULATION_BR = "InvoiceTaxCalculationJob_billingRun";
    
    
	@Inject
    private InvoiceTaxCalculationJobBean invoiceTaxCalculationJobBean;


    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    protected JobExecutionResultImpl execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        invoiceTaxCalculationJobBean.execute(result, jobInstance);
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.INVOICING;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<>();

        result.put(CF_NB_RUNS, CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:0", "-1", INVOICE_TAX_CALCULATION_JOB));
        result.put(Job.CF_WAITING_MILLIS, CustomFieldTemplateUtils.buildCF(Job.CF_WAITING_MILLIS, resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:1", "0", INVOICE_TAX_CALCULATION_JOB));
        result.put(CF_NB_PUBLISHERS, CustomFieldTemplateUtils.buildCF(CF_NB_PUBLISHERS, resourceMessages.getString("jobExecution.nbPublishers"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:2", INVOICE_TAX_CALCULATION_JOB));
        result.put(CF_BATCH_SIZE, CustomFieldTemplateUtils.buildCF(CF_BATCH_SIZE, resourceMessages.getString("jobExecution.batchSize"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:3", "10000", true, INVOICE_TAX_CALCULATION_JOB));
        
        result.put(CF_TAX_CALCULATION_BR, CustomFieldTemplateUtils.buildCF(CF_TAX_CALCULATION_BR, resourceMessages.getString("jobExecution.ilJob.billingRuns"), CustomFieldTypeEnum.ENTITY,
                "tab:Configuration:0;fieldGroup:Filtering:0;field:4", null, false, CustomFieldStorageTypeEnum.LIST, BillingRun.class.getName(), INVOICE_TAX_CALCULATION_JOB, null));

        return result;
    }
}