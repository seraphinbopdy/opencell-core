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
 * Job definition to do the mass update.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class MassUpdaterJob extends Job {

    /**
     * Default value of fetch size of select query
     */
    public static final Long DEFAULT_SELECT_FETCH_SIZE = 1000000L; //1M

    /**
     * Job instance name with which all custom fields will be applied
     */
    public static final String APPLIES_TO = "JobInstance_MassUpdaterJob";

    /**
     * Custom field that contains the update query value
     */
    public static final String PARAM_UPDATE_QUERY = "updateQuery";

    /**
     * Custom field that contains the update chunk value
     */
    public static final String PARAM_UPDATE_CHUNK_SIZE = "updateChunkSize";

    /**
     * Custom field that contains the select query value
     */
    public static final String PARAM_SELECT_QUERY = "selectQuery";

    /**
     * Custom field that contains the select fetch size value
     */
    public static final String PARAM_SELECT_FETCH_SIZE = "selectFetchSize";

    /**
     * Custom field that contains the select max results value
     */
    public static final String PARAM_SELECT_MAX_RESULTS = "selectMaxResults";

    /**
     * Custom field that contains the flag that indicates if query select and update one are native or not.
     */
    public static final String PARAM_IS_NATIVE_QUERY = "isNativeQuery";

    /**
     * Custom field containing the flag whether all update queries will be run on distinct IDs or whether it doesn't matter.
     */
    public static final String PARAM_IS_PESSIMISTIC_UPDATE_LOCK = "isPessimisticUpdateLock";

    /**
     * Custom field containing the flag that indicates if the job will use the view or not.
     */
    public static final String PARAM_IS_USING_VIEW = "isUsingView";

    /**
     * Custom field containing the flag that indicates if the job will use open cursor or not.
     */
    public static final String PARAM_IS_OPEN_CURSOR = "isOpenCursor";

    /**
     * Custom field that contains the named query name
     */
    public static final String PARAM_NAMED_QUERY = "namedQuery";

    /**
     * Job bean
     */
    @Inject
    private MassUpdaterOpenCursorJobBean massUpdaterOpenCursorJobBean;

    /**
     * Job bean
     */
    @Inject
    private MassUpdaterOfflineJobBean massUpdaterOfflineJobBean;


    @Override
    protected JobExecutionResultImpl execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) throws BusinessException {
        String updateQuery = (String) getParamOrCFValue(jobInstance, PARAM_UPDATE_QUERY);
        Long updateChunkSize = (Long) getParamOrCFValue(jobInstance, PARAM_UPDATE_CHUNK_SIZE);
        String selectQuery = (String) getParamOrCFValue(jobInstance, PARAM_SELECT_QUERY);
        Long selectFetchSize = (Long) getParamOrCFValue(jobInstance, PARAM_SELECT_FETCH_SIZE);
        Long selectMaxResults = (Long) getParamOrCFValue(jobInstance, PARAM_SELECT_MAX_RESULTS);
        Boolean isNativeQuery = (Boolean) getParamOrCFValue(jobInstance, PARAM_IS_NATIVE_QUERY, false);
        Boolean isPessimisticUpdateLock = (Boolean) getParamOrCFValue(jobInstance, PARAM_IS_PESSIMISTIC_UPDATE_LOCK, false);
        Boolean isUsingView = (Boolean) getParamOrCFValue(jobInstance, PARAM_IS_USING_VIEW, false);
        Boolean isOpenCursor = (Boolean) getParamOrCFValue(jobInstance, PARAM_IS_OPEN_CURSOR, false);

        execute(jobExecutionResult, jobInstance, null, updateQuery, updateChunkSize, selectQuery, selectFetchSize, selectMaxResults, isNativeQuery,
                isPessimisticUpdateLock, isUsingView, isOpenCursor);

        return jobExecutionResult;
    }

    /**
     * Execute job
     *
     * @param jobExecutionResult      the job execution result
     * @param jobInstance             the job instance
     * @param namedQuery              the named query
     * @param updateQuery             the update query
     * @param updateChunkSize         the chunk size of update query
     * @param selectQuery             the select query
     * @param selectFetchSize         the fetch size of select query
     * @param selectMaxResults        the max results of select query
     * @param isNativeQuery           indicates if the query is native or not
     * @param isPessimisticUpdateLock indicates if all update queries will be run on distinct IDs or whether it doesn't matter.
     * @param isUsingView             indicates if the job will be use the view or not.
     * @param isOpenCursor            indicates if the job will use open cursor or not.
     */
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, String namedQuery, String updateQuery, Long updateChunkSize, String selectQuery,
                        Long selectFetchSize, Long selectMaxResults, Boolean isNativeQuery, Boolean isPessimisticUpdateLock, Boolean isUsingView, Boolean isOpenCursor) {
        if (isOpenCursor) {
            massUpdaterOpenCursorJobBean.execute(jobExecutionResult, jobInstance, namedQuery, updateQuery, updateChunkSize, selectQuery, selectFetchSize,
                    selectMaxResults, isNativeQuery, isPessimisticUpdateLock, isUsingView);
        } else {
            massUpdaterOfflineJobBean.execute(jobExecutionResult, jobInstance, namedQuery, updateQuery, updateChunkSize, selectQuery, selectFetchSize,
                    selectMaxResults, isNativeQuery, isPessimisticUpdateLock);
        }
    }

    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.UTILS;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<>();

        result.put(CF_NB_RUNS,
                CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"),
                        CustomFieldTypeEnum.LONG, "tab:Configuration:0;fieldGroup:Configuration:0;field:0", "-1", APPLIES_TO));
        result.put(Job.CF_WAITING_MILLIS,
                CustomFieldTemplateUtils.buildCF(Job.CF_WAITING_MILLIS,
                        resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:1", "0", APPLIES_TO));
        result.put(PARAM_SELECT_FETCH_SIZE,
                CustomFieldTemplateUtils.buildCF(PARAM_SELECT_FETCH_SIZE,
                        resourceMessages.getString("jobExecution.massUpdate.selectFetchSize"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:2", String.valueOf(DEFAULT_SELECT_FETCH_SIZE), APPLIES_TO));
        result.put(PARAM_SELECT_MAX_RESULTS,
                CustomFieldTemplateUtils.buildCF(PARAM_SELECT_MAX_RESULTS,
                        resourceMessages.getString("jobExecution.massUpdate.selectMaxResults"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:3", APPLIES_TO));
        result.put(PARAM_UPDATE_CHUNK_SIZE,
                CustomFieldTemplateUtils.buildCF(PARAM_UPDATE_CHUNK_SIZE,
                        resourceMessages.getString("jobExecution.massUpdate.updateChunkSize"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:4", "100000", APPLIES_TO));
        result.put(PARAM_SELECT_QUERY,
                CustomFieldTemplateUtils.buildCF(PARAM_SELECT_QUERY,
                        resourceMessages.getString("jobExecution.massUpdate.selectQuery"), CustomFieldTypeEnum.TEXT_AREA,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:5", "", APPLIES_TO));
        result.put(PARAM_UPDATE_QUERY,
                CustomFieldTemplateUtils.buildCF(PARAM_UPDATE_QUERY,
                        resourceMessages.getString("jobExecution.massUpdate.updateQuery"), CustomFieldTypeEnum.TEXT_AREA,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:6", "", APPLIES_TO));
        result.put(PARAM_IS_NATIVE_QUERY,
                CustomFieldTemplateUtils.buildCF(PARAM_IS_NATIVE_QUERY,
                        resourceMessages.getString("jobExecution.massUpdate.isNativeQuery"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:7", "true", APPLIES_TO));
        result.put(PARAM_IS_PESSIMISTIC_UPDATE_LOCK,
                CustomFieldTemplateUtils.buildCF(PARAM_IS_PESSIMISTIC_UPDATE_LOCK,
                        resourceMessages.getString("jobExecution.massUpdate.isPessimisticUpdateLock"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:8", "false", APPLIES_TO));
        result.put(PARAM_IS_USING_VIEW,
                CustomFieldTemplateUtils.buildCF(PARAM_IS_USING_VIEW,
                        resourceMessages.getString("jobExecution.massUpdate.isUsingView"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:9", "false", APPLIES_TO));
        result.put(PARAM_IS_OPEN_CURSOR,
                CustomFieldTemplateUtils.buildCF(PARAM_IS_OPEN_CURSOR,
                        resourceMessages.getString("jobExecution.massUpdate.isOpenCursor"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:10", "true", APPLIES_TO));
        return result;
    }
}