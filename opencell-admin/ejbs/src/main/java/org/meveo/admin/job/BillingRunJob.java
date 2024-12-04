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

import java.util.HashMap;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.BillingProcessTypesEnum;
import org.meveo.model.billing.BillingRunAutomaticActionEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.service.job.Job;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;


/**
 * The Class BillingRunJob create a BillingRun for the given BillingCycle, lastTransactionDate,invoiceDate.
 * @author Abdellatif BARI
 * @lastModifiedVersion 7.0
 */
@Stateless
public class BillingRunJob extends Job {

    /** The billing run job bean. */
    @Inject
    private BillingRunJobBean billingRunJobBean;

    @Override
    protected JobExecutionResultImpl execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        billingRunJobBean.execute(result,  jobInstance);
        return result;
    }

   
    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.INVOICING;
    }

   
    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();

        CustomFieldTemplate lastTransactionDate = new CustomFieldTemplate();
        lastTransactionDate.setCode("BillingRunJob_lastTransactionDate");
        lastTransactionDate.setAppliesTo("JobInstance_BillingRunJob");
        lastTransactionDate.setActive(true);
        lastTransactionDate.setDescription(resourceMessages.getString("jobExecution.lastTransationDate"));
        lastTransactionDate.setFieldType(CustomFieldTypeEnum.DATE);
        lastTransactionDate.setValueRequired(false);
        lastTransactionDate.setGuiPosition("tab:Configuration:0;field:0");
        result.put("BillingRunJob_lastTransactionDate", lastTransactionDate);

        CustomFieldTemplate invoiceDate = new CustomFieldTemplate();
        invoiceDate.setCode("BillingRunJob_invoiceDate");
        invoiceDate.setAppliesTo("JobInstance_BillingRunJob");
        invoiceDate.setActive(true);
        invoiceDate.setDescription(resourceMessages.getString("jobExecution.InvoiceDate"));
        invoiceDate.setFieldType(CustomFieldTypeEnum.DATE);
        invoiceDate.setValueRequired(false);
        invoiceDate.setGuiPosition("tab:Configuration:0;field:1");
        result.put("BillingRunJob_invoiceDate", invoiceDate);

        CustomFieldTemplate billingCycle = new CustomFieldTemplate();
        billingCycle.setCode("BillingRunJob_billingCycle");
        billingCycle.setAppliesTo("JobInstance_BillingRunJob");
        billingCycle.setActive(true);
        billingCycle.setDescription(resourceMessages.getString("jobExecution.billingCycles"));
        billingCycle.setFieldType(CustomFieldTypeEnum.ENTITY);
        billingCycle.setStorageType(CustomFieldStorageTypeEnum.LIST);
        billingCycle.setEntityClazz("org.meveo.model.billing.BillingCycle");
        billingCycle.setValueRequired(true);
        billingCycle.setGuiPosition("tab:Configuration:0;field:2");
        result.put("BillingRunJob_billingCycle", billingCycle);

        CustomFieldTemplate billingCycleType = new CustomFieldTemplate();
        billingCycleType.setCode("BillingRunJob_billingRun_Process");
        billingCycleType.setAppliesTo("JobInstance_BillingRunJob");
        billingCycleType.setActive(true);
        billingCycleType.setDescription(resourceMessages.getString("jobExecution.billingRunProcess"));
        billingCycleType.setFieldType(CustomFieldTypeEnum.LIST);
        billingCycleType.setStorageType(CustomFieldStorageTypeEnum.SINGLE);
        Map<String, String> listValues = new HashMap();
        for(BillingProcessTypesEnum type : BillingProcessTypesEnum.values()){
            listValues.put(""+type.getId(), resourceMessages.getString(type.getLabel()));
        }
        billingCycleType.setListValues(listValues);
        billingCycleType.setValueRequired(false);
        billingCycleType.setGuiPosition("tab:Configuration:0;field:3");
        result.put("BillingRunJob_billingRun_Process", billingCycleType);
        
        CustomFieldTemplate rejectAutoAction = new CustomFieldTemplate();
        rejectAutoAction.setCode("BillingRunJob_rejectAutoAction");
        rejectAutoAction.setAppliesTo("JobInstance_BillingRunJob");
        rejectAutoAction.setActive(true);
        rejectAutoAction.setDescription(resourceMessages.getString("billingRun.rejectAutoAction"));
        rejectAutoAction.setFieldType(CustomFieldTypeEnum.LIST);
        billingCycleType.setStorageType(CustomFieldStorageTypeEnum.SINGLE);
        Map<String, String> billingRunAutomaticActionValues = new HashMap<>();
        for(BillingRunAutomaticActionEnum type : BillingRunAutomaticActionEnum.values()){
            billingRunAutomaticActionValues.put(""+type.getId(), resourceMessages.getString(type.getLabel()));
        }
        billingCycleType.setListValues(billingRunAutomaticActionValues);
        rejectAutoAction.setValueRequired(false);
        rejectAutoAction.setGuiPosition("tab:Configuration:0;field:4");
        result.put("BillingRunJob_rejectAutoAction", rejectAutoAction);

        CustomFieldTemplate suspectAutoAction = new CustomFieldTemplate();
        suspectAutoAction.setCode("BillingRunJob_suspectAutoAction");
        suspectAutoAction.setAppliesTo("JobInstance_BillingRunJob");
        suspectAutoAction.setActive(true);
        suspectAutoAction.setDescription(resourceMessages.getString("billingRun.suspectAutoAction"));
        suspectAutoAction.setFieldType(CustomFieldTypeEnum.LIST);
        billingCycleType.setStorageType(CustomFieldStorageTypeEnum.SINGLE);
        Map<String, String> suspectAutoActionValues = new HashMap<>();
        for(BillingRunAutomaticActionEnum type : BillingRunAutomaticActionEnum.values()){
            suspectAutoActionValues.put(""+type.getId(), resourceMessages.getString(type.getLabel()));
        }
        billingCycleType.setListValues(suspectAutoActionValues);
        suspectAutoAction.setValueRequired(false);
        suspectAutoAction.setGuiPosition("tab:Configuration:0;field:5");
        result.put("BillingRunJob_suspectAutoAction", suspectAutoAction);

        CustomFieldTemplate generateAO = new CustomFieldTemplate();
        generateAO.setCode("BillingRunJob_generateAO");
        generateAO.setAppliesTo("JobInstance_BillingRunJob");
        generateAO.setActive(true);
        generateAO.setDescription(resourceMessages.getString("invoiceType.invoiceAccountable"));
        generateAO.setFieldType(CustomFieldTypeEnum.BOOLEAN);
        generateAO.setValueRequired(false);
        suspectAutoAction.setGuiPosition("tab:Configuration:0;field:6");
        result.put("BillingRunJob_generateAO", generateAO);

        CustomFieldTemplate computeDatesAtValidation = new CustomFieldTemplate();
        computeDatesAtValidation.setCode("BillingRunJob_computeDatesAtValidation");
        computeDatesAtValidation.setAppliesTo("JobInstance_BillingRunJob");
        computeDatesAtValidation.setActive(true);
        computeDatesAtValidation.setDescription(resourceMessages.getString("billingRun.computeDatesAtValidation"));
        computeDatesAtValidation.setFieldType(CustomFieldTypeEnum.BOOLEAN);
        computeDatesAtValidation.setValueRequired(false);
        computeDatesAtValidation.setGuiPosition("tab:Configuration:0;field:7");
        result.put("BillingRunJob_computeDatesAtValidation", computeDatesAtValidation);

        return result;
    }
}
