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

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.utils.CustomFieldTemplateUtils;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.model.IEntity;
import org.meveo.model.billing.BatchEntity;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.job.Job;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;

/**
 * Job definition to update a huge entity.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class UpdateHugeEntityJob extends Job {

    /**
     * Default value of fetch size of select query
     */
    public static final Long DEFAULT_SELECT_FETCH_SIZE = 1000000L; //1M

    /**
     * Job instance name with which all custom fields will be applied
     */
    public static final String APPLIES_TO = "JobInstance_UpdateHugeEntityJob";

    /**
     * Custom field that contains the target job value
     */
    public static final String CF_TARGET_JOB = "targetJob";

    /**
     * Custom field that contains the huge entity on which the job will update
     */
    public static final String CF_ENTITY_ClASS_NAME = "entityClassName";

    /**
     * Custom field that contains the fields to update (list of fields separated by commas and their values, example: status='OPEN', amount =10)
     */
    public static final String CF_FIELDS_TO_UPDATE = "fieldsToUpdate";

    /**
     * Custom field that contains the default filter for the update query which will be executed by the job to update huge entites.
     */
    public static final String CF_DEFAULT_FILTER = "defaultFilter";

    /**
     * Custom field that contains notification message which will send when job is done
     */
    public static final String CF_EMAIL_TEMPLATE = "emailTemplate";

    /**
     * Custom field that contains the select fetch size value
     */
    public static final String CF_SELECT_FETCH_SIZE = "selectFetchSize";

    /**
     * Custom field that contains the select max results value
     */
    public static final String CF_SELECT_MAX_RESULTS = "selectMaxResults";

    /**
     * Custom field that contains update query chunk size value
     */
    public static final String CF_UPDATE_CHUNK_SIZE = "updateChunkSize";

    /**
     * Custom field containing the flag whether all update queries will be run on distinct IDs or whether it doesn't matter.
     */
    public static final String CF_IS_PESSIMISTIC_UPDATE_LOCK = "isPessimisticUpdateLock";

    /**
     * Custom field containing the flag that indicates if the job will be use the view or not.
     */
    public static final String CF_IS_USING_VIEW = "isUsingView";

    /**
     * Custom field containing the flag that indicates if the job will use open cursor or not.
     */
    public static final String CF_IS_OPEN_CURSOR = "isOpenCursor";

    /**
     * Custom field containing the flag that indicates if the select query should use strict checking to compare two strings or not.
     */
    public static final String CF_IS_CASE_SENSITIVE = "isCaseSensitive";

    /**
     * Custom field containing action and check to be performed before updating the entity
     */
    public static final String CF_PRE_UPDATE_EL = "preUpdateEl";

    /**
     * Custom field containing the list of batch entities that will be processed.
     */
    public static final String CF_BATCHES_TO_PROCESS = "batchesToProcess";

    /**
     * Custom field containing the pre update script which will be run before update the huge entity.
     */
    public static final String CF_PRE_UPDATE_SCRIPT = "preUpdateScript";


    /**
     * Job bean
     */
    @Inject
    private UpdateHugeEntityJobBean UpdateHugeEntityJobBean;

    @Override
    protected JobExecutionResultImpl execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        UpdateHugeEntityJobBean.execute(result, jobInstance);
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

    /**
     * Get all classes of the package "org.meveo.model"
     *
     * @return all classes of the package "org.meveo.model"
     */
    private Map<String, String> getClassNames() {
        Set<Class<? extends IEntity>> classes = null;
        try {
            classes = ReflectionUtils.getClasses("org.meveo.model", IEntity.class);
        } catch (Exception e) {
            log.error("Failed to get a list of classes for a model package", e);
            return null;
        }

        Map<String, String> classNames = new HashMap<>();
        for (Class<? extends IEntity> clazz : classes) {
            if (Proxy.isProxyClass(clazz) || clazz.getName().contains("$$")) {
                continue;
            }
            if (clazz.isAnnotationPresent(Entity.class)) {
                classNames.put(clazz.getName(), clazz.getSimpleName());
            }
        }
        return classNames;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<>();

        result.put(CF_TARGET_JOB, CustomFieldTemplateUtils.buildCF(CF_TARGET_JOB, resourceMessages.getString("jobExecution.updateHugeEntity.targetJob"),
                CustomFieldTypeEnum.STRING, "tab:Configuration:0;fieldGroup:Job configuration:0;field:0", null, true, APPLIES_TO));

        CustomFieldTemplate entityClassNameCF = CustomFieldTemplateUtils.buildCF(CF_ENTITY_ClASS_NAME, resourceMessages.getString("jobExecution.updateHugeEntity.entityClassName"),
                CustomFieldTypeEnum.LIST, "tab:Configuration:0;fieldGroup:Job configuration:0;field:1", null, true, CustomFieldStorageTypeEnum.SINGLE, null, APPLIES_TO, null);
        entityClassNameCF.setListValues(getClassNames());
        entityClassNameCF.setAllowEdit(false);
        result.put(CF_ENTITY_ClASS_NAME, entityClassNameCF);

        CustomFieldTemplate fieldsToUpdate = CustomFieldTemplateUtils.buildCF(CF_FIELDS_TO_UPDATE, resourceMessages.getString("jobExecution.updateHugeEntity.fieldsToUpdate"),
                CustomFieldTypeEnum.TEXT_AREA, "tab:Configuration:0;fieldGroup:Job configuration:0;field:2", APPLIES_TO);
        fieldsToUpdate.setAllowEdit(false);
        result.put(CF_FIELDS_TO_UPDATE, fieldsToUpdate);

        CustomFieldTemplate defaultFilter = CustomFieldTemplateUtils.buildCF(CF_DEFAULT_FILTER, resourceMessages.getString("jobExecution.updateHugeEntity.defaultFilter"),
                CustomFieldTypeEnum.TEXT_AREA, "tab:Configuration:0;fieldGroup:Job configuration:0;field:3", APPLIES_TO);
        defaultFilter.setAllowEdit(false);
        result.put(CF_DEFAULT_FILTER, defaultFilter);

        CustomFieldTemplate emailTemplateCF = CustomFieldTemplateUtils.buildCF(CF_EMAIL_TEMPLATE, resourceMessages.getString("jobExecution.updateHugeEntity.emailTemplate"), CustomFieldTypeEnum.ENTITY,
                "tab:Configuration:0;fieldGroup:Job configuration:0;field:4", null, false, null, EmailTemplate.class.getName(), APPLIES_TO, null);
        emailTemplateCF.setDataFilterEL("{\"media\":\"EMAIL\"}");
        result.put(CF_EMAIL_TEMPLATE, emailTemplateCF);

        result.put(CF_NB_RUNS,
                CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"),
                        CustomFieldTypeEnum.LONG, "tab:Configuration:0;fieldGroup:Execution configuration:1;field:0", "-1", APPLIES_TO));

        result.put(CF_WAITING_MILLIS,
                CustomFieldTemplateUtils.buildCF(CF_WAITING_MILLIS,
                        resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Execution configuration:1;field:1", "0", APPLIES_TO));

        result.put(CF_SELECT_FETCH_SIZE,
                CustomFieldTemplateUtils.buildCF(CF_SELECT_FETCH_SIZE,
                        resourceMessages.getString("jobExecution.updateHugeEntity.selectFetchSize"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:2", String.valueOf(DEFAULT_SELECT_FETCH_SIZE), APPLIES_TO));

        result.put(CF_SELECT_MAX_RESULTS,
                CustomFieldTemplateUtils.buildCF(CF_SELECT_MAX_RESULTS,
                        resourceMessages.getString("jobExecution.updateHugeEntity.selectMaxResults"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:3", APPLIES_TO));

        result.put(CF_UPDATE_CHUNK_SIZE,
                CustomFieldTemplateUtils.buildCF(CF_UPDATE_CHUNK_SIZE,
                        resourceMessages.getString("jobExecution.updateHugeEntity.updateChunkSize"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:4", String.valueOf(NativePersistenceService.SHORT_MAX_VALUE), APPLIES_TO));

        result.put(CF_IS_PESSIMISTIC_UPDATE_LOCK,
                CustomFieldTemplateUtils.buildCF(CF_IS_PESSIMISTIC_UPDATE_LOCK,
                        resourceMessages.getString("jobExecution.updateHugeEntity.isPessimisticUpdateLock"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:5", "false", APPLIES_TO));

        result.put(CF_IS_USING_VIEW,
                CustomFieldTemplateUtils.buildCF(CF_IS_USING_VIEW,
                        resourceMessages.getString("jobExecution.updateHugeEntity.isUsingView"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:6", "true", APPLIES_TO));

        result.put(CF_IS_OPEN_CURSOR,
                CustomFieldTemplateUtils.buildCF(CF_IS_OPEN_CURSOR,
                        resourceMessages.getString("jobExecution.updateHugeEntity.isOpenCursor"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:7", "true", APPLIES_TO));

        result.put(CF_IS_CASE_SENSITIVE,
                CustomFieldTemplateUtils.buildCF(CF_IS_CASE_SENSITIVE,
                        resourceMessages.getString("jobExecution.updateHugeEntity.isCaseSensitive"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:8", "false", APPLIES_TO));

        result.put(CF_PRE_UPDATE_EL,
                CustomFieldTemplateUtils.buildCF(CF_PRE_UPDATE_EL,
                        resourceMessages.getString("jobExecution.updateHugeEntity.preUpdateEl"), CustomFieldTypeEnum.STRING,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:9", APPLIES_TO, 1000L));

        result.put(CF_BATCHES_TO_PROCESS,
                CustomFieldTemplateUtils.buildCF(CF_BATCHES_TO_PROCESS,
                        resourceMessages.getString("jobExecution.updateHugeEntity.batchesToProcess"), CustomFieldTypeEnum.ENTITY,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:10",
                        null, false, CustomFieldStorageTypeEnum.LIST, BatchEntity.class.getName(),
                        APPLIES_TO, null));

        result.put(CF_PRE_UPDATE_SCRIPT,
                CustomFieldTemplateUtils.buildCF(CF_PRE_UPDATE_SCRIPT,
                        resourceMessages.getString("jobExecution.updateHugeEntity.preUpdateScript"), CustomFieldTypeEnum.ENTITY,
                        "tab:Configuration:0;fieldGroup:Execution configuration:0;field:11",
                        null, false, null, ScriptInstance.class.getName(),
                        APPLIES_TO, null));

        return result;
    }
}