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
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.job.Job;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Entity;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Job definition to update a huge entity.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class UpdateHugeEntityJob extends Job {

    /**
     * Default value of limit of select query
     */
    public static final Long DEFAULT_SELECT_LIMIT = 1000000L; //1M

    /**
     * Job instance name with which all custom fields will be applied
     */
    public static final String APPLIES_TO = "JobInstance_UpdateHugeEntityJob";

    /**
     * Custom field that contains the target job value
     */
    public static final String CF_TARGET_JOB = "UpdateHugeEntityJob_targetJob";

    /**
     * Custom field that contains the huge entity on which the job will update
     */
    public static final String CF_ENTITY_ClASS_NAME = "UpdateHugeEntityJob_entityClassName";

    /**
     * Custom field that contains the fields to update (list of fields separated by commas and their values, example: status='OPEN', amount =10)
     */
    public static final String CF_FIELDS_TO_UPDATE = "UpdateHugeEntityJob_fieldsToUpdate";

    /**
     * Custom field that contains the default filter for the update query which will be executed by the job to update huge entites.
     */
    public static final String CF_DEFAULT_FILTER = "UpdateHugeEntityJob_defaultFilter";

    /**
     * Custom field that contains the select limit value
     */
    public static final String CF_SELECT_LIMIT = "UpdateHugeEntityJob_selectLimit";

    /**
     * Custom field that contains update query chunk value
     */
    public static final String CF_UPDATE_CHUNK = "UpdateHugeEntityJob_updateChunk";

    /**
     * Custom field containing the flag whether all update queries will be run on distinct IDs or whether it doesn't matter.
     */
    public static final String CF_IS_PESSIMISTIC_UPDATE_LOCK = "UpdateHugeEntityJob_isPessimisticUpdateLock";

    /**
     * Custom field that contains notification message which will send when job is done
     */
    public static final String CF_EMAIL_TEMPLATE = "UpdateHugeEntityJob_emailTemplate";


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
        List<Class> classes = null;
        try {
            classes = ReflectionUtils.getClasses("org.meveo.model");
        } catch (Exception e) {
            log.error("Failed to get a list of classes for a model package", e);
            return null;
        }

        Map<String, String> classNames = new HashMap<>();
        for (Class clazz : classes) {
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

        result.put(CF_NB_RUNS,
                CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"),
                        CustomFieldTypeEnum.LONG, "tab:Configuration:0;fieldGroup:Configuration:0;field:0", "-1", APPLIES_TO));

        result.put(Job.CF_WAITING_MILLIS,
                CustomFieldTemplateUtils.buildCF(Job.CF_WAITING_MILLIS,
                        resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:1", "0", APPLIES_TO));

        result.put(CF_TARGET_JOB, CustomFieldTemplateUtils.buildCF(CF_TARGET_JOB, resourceMessages.getString("jobExecution.updateHugeEntity.targetJob"),
                CustomFieldTypeEnum.STRING, "tab:Configuration:0;fieldGroup:Configuration:0;field:2", null, true, APPLIES_TO));

        CustomFieldTemplate entityClassNameCF = CustomFieldTemplateUtils.buildCF(CF_ENTITY_ClASS_NAME, resourceMessages.getString("jobExecution.updateHugeEntity.entityClassName"),
                CustomFieldTypeEnum.LIST, "tab:Configuration:0;fieldGroup:Configuration:0;field:3", null, true, CustomFieldStorageTypeEnum.SINGLE, null, APPLIES_TO, null);
        entityClassNameCF.setListValues(getClassNames());
        entityClassNameCF.setAllowEdit(false);
        result.put(CF_ENTITY_ClASS_NAME, entityClassNameCF);

        CustomFieldTemplate fieldsToUpdate = CustomFieldTemplateUtils.buildCF(CF_FIELDS_TO_UPDATE, resourceMessages.getString("jobExecution.updateHugeEntity.fieldsToUpdate"),
                CustomFieldTypeEnum.TEXT_AREA, "tab:Configuration:0;fieldGroup:Configuration:0;field:4", APPLIES_TO);
        fieldsToUpdate.setAllowEdit(false);
        result.put(CF_FIELDS_TO_UPDATE, fieldsToUpdate);

        CustomFieldTemplate defaultFilter = CustomFieldTemplateUtils.buildCF(CF_DEFAULT_FILTER, resourceMessages.getString("jobExecution.updateHugeEntity.defaultFilter"),
                CustomFieldTypeEnum.TEXT_AREA, "tab:Configuration:0;fieldGroup:Configuration:0;field:5", APPLIES_TO);
        defaultFilter.setAllowEdit(false);
        result.put(CF_DEFAULT_FILTER, defaultFilter);

        result.put(CF_SELECT_LIMIT,
                CustomFieldTemplateUtils.buildCF(CF_SELECT_LIMIT,
                        resourceMessages.getString("jobExecution.updateHugeEntity.selectLimit"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:6", String.valueOf(DEFAULT_SELECT_LIMIT), APPLIES_TO));

        result.put(CF_UPDATE_CHUNK,
                CustomFieldTemplateUtils.buildCF(CF_UPDATE_CHUNK,
                        resourceMessages.getString("jobExecution.updateHugeEntity.updateChunk"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:7", String.valueOf(NativePersistenceService.SHORT_MAX_VALUE), APPLIES_TO));

        result.put(CF_IS_PESSIMISTIC_UPDATE_LOCK,
                CustomFieldTemplateUtils.buildCF(CF_IS_PESSIMISTIC_UPDATE_LOCK,
                        resourceMessages.getString("jobExecution.updateHugeEntity.isPessimisticUpdateLock"), CustomFieldTypeEnum.BOOLEAN,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:8", "false", APPLIES_TO));

        CustomFieldTemplate emailTemplateCF = CustomFieldTemplateUtils.buildCF(CF_EMAIL_TEMPLATE, resourceMessages.getString("jobExecution.updateHugeEntity.emailTemplate"), CustomFieldTypeEnum.ENTITY,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:9", null, false, null, EmailTemplate.class.getName(), APPLIES_TO, null);
        emailTemplateCF.setDataFilterEL("{\"media\":\"EMAIL\"}");
        result.put(CF_EMAIL_TEMPLATE, emailTemplateCF);
        return result;
    }
}