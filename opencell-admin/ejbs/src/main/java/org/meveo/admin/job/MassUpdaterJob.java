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

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.utils.CustomFieldTemplateUtils;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.service.job.Job;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;


/**
 * Job definition to do the mass update.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class MassUpdaterJob extends Job {

    /**
     * Default value of limit of select query
     */
    public static final Long DEFAULT_SELECT_LIMIT = 10000000L; //10M

    /**
     * Job instance name with which all custom fields will be applied
     */
    public static final String APPLIES_TO = "JobInstance_MassUpdaterJob";

    /**
     * Custom field that contains the update query value
     */
    public static final String PARAM_UPDATE_QUERY = "MassUpdaterJob_updateQuery";

    /**
     * Custom field that contains the update chunk value
     */
    public static final String PARAM_UPDATE_CHUNK = "MassUpdaterJob_updateChunk";

    /**
     * Custom field that contains the select query value
     */
    public static final String PARAM_SELECT_QUERY = "MassUpdaterJob_selectQuery";

    /**
     * Custom field that contains the select table alias value
     */
    public static final String PARAM_SELECT_TABLE_ALIAS = "MassUpdaterJob_selectTableAlias";

    /**
     * Custom field that contains the select limit value
     */
    public static final String PARAM_SELECT_LIMIT = "MassUpdaterJob_selectLimit";

    /**
     * Custom field that contains the flag that indicates if query select and update one are native or not.
     */
    public static final String PARAM_IS_NATIVE_QUERY = "MassUpdaterJob_isNativeQuery";

    /**
     * Custom field that contains the named query name
     */
    public static final String PARAM_NAMED_QUERY = "MassUpdaterJob_namedQuery";

    /**
     * Job bean
     */
    @Inject
    private MassUpdaterJobBean massUpdaterJobBean;

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    protected JobExecutionResultImpl execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) throws BusinessException {

        String updateQuery = (String) getParamOrCFValue(jobInstance, PARAM_UPDATE_QUERY);
        Long updateChunk = (Long) getParamOrCFValue(jobInstance, PARAM_UPDATE_CHUNK);
        String selectQuery = (String) getParamOrCFValue(jobInstance, PARAM_SELECT_QUERY);
        String selectTableAlias = (String) getParamOrCFValue(jobInstance, PARAM_SELECT_TABLE_ALIAS);
        Long selectLimit = (Long) getParamOrCFValue(jobInstance, PARAM_SELECT_LIMIT);
        Boolean isNativeQuery = (Boolean) getParamOrCFValue(jobInstance, PARAM_IS_NATIVE_QUERY, false);
        massUpdaterJobBean.execute(jobExecutionResult, jobInstance, null, updateQuery, updateChunk, selectQuery, selectTableAlias, selectLimit, isNativeQuery);
        return jobExecutionResult;
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
        result.put(PARAM_UPDATE_QUERY,
                CustomFieldTemplateUtils.buildCF(PARAM_UPDATE_QUERY,
                        resourceMessages.getString("jobExecution.massUpdate.updateQuery"), CustomFieldTypeEnum.TEXT_AREA,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:2", "", APPLIES_TO));
        result.put(PARAM_UPDATE_CHUNK,
                CustomFieldTemplateUtils.buildCF(PARAM_UPDATE_CHUNK,
                        resourceMessages.getString("jobExecution.massUpdate.updateChunk"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:3", "100000", APPLIES_TO));
        result.put(PARAM_SELECT_QUERY,
                CustomFieldTemplateUtils.buildCF(PARAM_SELECT_QUERY,
                        resourceMessages.getString("jobExecution.massUpdate.selectQuery"), CustomFieldTypeEnum.TEXT_AREA,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:4", "", APPLIES_TO));
        result.put(PARAM_SELECT_TABLE_ALIAS,
                CustomFieldTemplateUtils.buildCF(PARAM_SELECT_TABLE_ALIAS,
                        resourceMessages.getString("jobExecution.massUpdate.selectTableAlias"), CustomFieldTypeEnum.STRING,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:5", "a", false,
                        null, null, APPLIES_TO, 5L));
        result.put(PARAM_SELECT_LIMIT,
                CustomFieldTemplateUtils.buildCF(PARAM_SELECT_LIMIT,
                        resourceMessages.getString("jobExecution.massUpdate.selectLimit"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:6", String.valueOf(DEFAULT_SELECT_LIMIT), APPLIES_TO));
        result.put(PARAM_IS_NATIVE_QUERY,
                CustomFieldTemplateUtils.buildCF(PARAM_IS_NATIVE_QUERY,
                        resourceMessages.getString("jobExecution.massUpdate.isNativeQuery"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:7", "true", APPLIES_TO));
        return result;
    }
}