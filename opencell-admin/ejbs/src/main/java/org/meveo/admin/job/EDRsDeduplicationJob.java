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
import org.meveo.service.job.ScopedJob;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * Job definition to cancel duplicated EDRs and keep only the newest version.
 *
 * @author Abdellatif BARI
 * @since 16.1.0
 */
@Stateless
public class EDRsDeduplicationJob extends ScopedJob {

    /**
     * Job instance name with which all custom fields will be applied
     */
    public static final String APPLIES_TO = "JobInstance_EDRsDeduplicationJob";

    /**
     * Default value of fetch size of select query
     */
    public static final Long DEFAULT_SELECT_FETCH_SIZE = 1000L;

    /**
     * Default value of update chunk size
     */
    public static final Long DEFAULT_UPDATE_CHUNK_SIZE = 100L;

    /**
     * Custom field that contains the select fetch size value
     */
    public static final String SELECT_FETCH_SIZE = "selectFetchSize";

    /**
     * Custom field that contains the update chunk value
     */
    public static final String UPDATE_CHUNK_SIZE = "updateChunkSize";

    /**
     * Custom field that contains the number of days to process
     */
    public static final String DAYS_TO_PROCESS = "daysToProcess";

    /**
     * Job bean
     */
    @Inject
    private EDRsDeduplicationJobBean edrsDeduplicationJobBean;

    @Override
    protected JobExecutionResultImpl execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        edrsDeduplicationJobBean.execute(result, jobInstance);
        return result;
    }

    /**
     * Get job category
     *
     * @return {@link MeveoJobCategoryEnum#UTILS}
     */
    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.UTILS;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<>();

        result.put(CF_NB_RUNS,
                CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"),
                        CustomFieldTypeEnum.LONG, "tab:Configuration:0;fieldGroup:Execution configuration:0;field:0", "-1", APPLIES_TO));

        result.put(CF_WAITING_MILLIS,
                CustomFieldTemplateUtils.buildCF(CF_WAITING_MILLIS,
                        resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:1", "0", APPLIES_TO));
        result.put(DAYS_TO_PROCESS,
                CustomFieldTemplateUtils.buildCF(DAYS_TO_PROCESS,
                        resourceMessages.getString("jobExecution.edrsDeduplication.daysToProcess"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:2", String.valueOf(2), APPLIES_TO));

        result.put(SELECT_FETCH_SIZE,
                CustomFieldTemplateUtils.buildCF(SELECT_FETCH_SIZE,
                        resourceMessages.getString("jobExecution.edrsDeduplication.selectFetchSize"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:3", String.valueOf(DEFAULT_SELECT_FETCH_SIZE), APPLIES_TO));

        result.put(UPDATE_CHUNK_SIZE,
                CustomFieldTemplateUtils.buildCF(UPDATE_CHUNK_SIZE,
                        resourceMessages.getString("jobExecution.edrsDeduplication.updateChunkSize"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:4", String.valueOf(DEFAULT_UPDATE_CHUNK_SIZE), APPLIES_TO));

        result.put(CF_JOB_ITEMS_LIMIT, CustomFieldTemplateUtils.buildCF(CF_JOB_ITEMS_LIMIT, resourceMessages.getString("jobExecution.jobItemsLimit"),
                CustomFieldTypeEnum.LONG, "tab:Configuration:0;fieldGroup:Execution configuration:0;field:5", APPLIES_TO));

        result.put(CF_JOB_DURATION_LIMIT, CustomFieldTemplateUtils.buildCF(CF_JOB_DURATION_LIMIT, resourceMessages.getString("jobExecution.jobDurationLimit"),
                CustomFieldTypeEnum.LONG, "tab:Configuration:0;fieldGroup:Execution configuration:0;field:6", APPLIES_TO));

        result.put(CF_JOB_TIME_LIMIT, CustomFieldTemplateUtils.buildCF(CF_JOB_TIME_LIMIT, resourceMessages.getString("jobExecution.jobTimeLimit"),
                CustomFieldTypeEnum.STRING, "tab:Configuration:0;fieldGroup:Execution configuration:0;field:7", APPLIES_TO, 5L));


        return result;
    }
}