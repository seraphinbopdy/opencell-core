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
import org.meveo.admin.job.utils.CustomFieldTemplateUtils;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.service.job.Job;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * Job definition to apply the threshold rules for the invoice, billing account, customer account and customer.
 *
 * @author Abdellatif BARI
 * @since 16.0.0
 */
@Stateless
public class InvoicingThresholdJob extends Job {

    /**
     * Job instance name with which all custom fields will be applied
     */

    public static final String APPLIES_TO = "JobInstance_InvoicingThresholdJob";

    /**
     * Job bean
     */
    @Inject
    private InvoicingThresholdJobBean invoicingThresholdJobBean;

    @Override
    protected JobExecutionResultImpl execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        invoicingThresholdJobBean.execute(result, jobInstance);
        return result;
    }

    /**
     * Get job category
     *
     * @return {@link MeveoJobCategoryEnum#INVOICING}
     */
    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.INVOICING;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<>();

        result.put(CF_NB_RUNS,
                CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"),
                        CustomFieldTypeEnum.LONG, "tab:Configuration:0;fieldGroup:Job configuration:0;field:0", "-1", APPLIES_TO));

        result.put(CF_WAITING_MILLIS,
                CustomFieldTemplateUtils.buildCF(CF_WAITING_MILLIS,
                        resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Job configuration:0;field:1", "0", APPLIES_TO));

        return result;
    }
}