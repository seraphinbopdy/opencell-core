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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.MassUpdaterJobBean;
import org.meveo.admin.job.UpdateHugeEntityJob;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.apiv2.common.HugeEntity;
import org.meveo.commons.utils.MethodCallingUtils;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.CustomGenericEntityCode;
import org.meveo.model.admin.User;
import org.meveo.model.billing.BatchEntity;
import org.meveo.model.billing.BatchEntityStatusEnum;
import org.meveo.model.communication.email.EmailTemplate;
import org.meveo.model.crm.EntityReferenceWrapper;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.JobLauncherEnum;
import org.meveo.service.admin.impl.CustomGenericEntityCodeService;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.communication.impl.EmailSender;
import org.meveo.service.communication.impl.EmailTemplateService;
import org.meveo.service.communication.impl.InternationalSettingsService;
import org.meveo.service.crm.impl.ProviderService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.meveo.service.base.ValueExpressionWrapper.evaluateExpression;

/**
 * A class for Batch entity persistence services.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class BatchEntityService extends PersistenceService<BatchEntity> {

    private static final String DEFAULT_EMAIL_ADDRESS = "no-reply@opencellsoft.com";

    @Inject
    @Named
    private NativePersistenceService nativePersistenceService;

    @Inject
    private UserService userService;

    @Inject
    EmailTemplateService emailTemplateService;

    @Inject
    private InternationalSettingsService internationalSettingsService;

    @Inject
    private ProviderService providerService;

    @Inject
    private EmailSender emailSender;

    @Inject
    private CustomGenericEntityCodeService customGenericEntityCodeService;

    @Inject
    private ServiceSingleton serviceSingleton;

    @Inject
    private MassUpdaterJobBean massUpdaterJobBean;

    @Inject
    private MethodCallingUtils methodCallingUtils;

    /**
     * Create the new batch entity
     *
     * @param filters      the filters
     * @param targetJob    the target job
     * @param targetEntity the target entity
     */
    @Deprecated()
    public void create(Map<String, Object> filters, String targetJob, String targetEntity) {
        BatchEntity batchEntity = new BatchEntity();
        batchEntity.setCode(getBatchEntityCode(null));
        batchEntity.setDescription(targetJob + "_" + targetEntity);
        batchEntity.setTargetJob(targetJob);
        batchEntity.setTargetEntity(targetEntity);
        batchEntity.setFilters(filters);
        batchEntity.setNotify(true);
        create(batchEntity);
    }

    /**
     * Create the new batch entity
     *
     * @param hugeEntity   the huge entity
     * @param filters      the filters
     * @param targetEntity the target entity
     */
    public void create(HugeEntity hugeEntity, Map<String, Object> filters, String targetEntity) {
        BatchEntity batchEntity = new BatchEntity();
        batchEntity.setCode(getBatchEntityCode(null));
        batchEntity.setDescription(!StringUtils.isBlank(hugeEntity.getDescription()) ? hugeEntity.getDescription() : hugeEntity.getTargetJob() + "_" + targetEntity);
        batchEntity.setTargetJob(hugeEntity.getTargetJob());
        batchEntity.setTargetEntity(targetEntity);
        batchEntity.setFilters(filters);
        batchEntity.setNotify(hugeEntity.getNotify() != null ? hugeEntity.getNotify() : true);
        create(batchEntity);
    }

    /**
     * Call BatchEntity.cancelOpenedBatchEntity Named query to cancel opened RatedTransaction.
     *
     * @param id rated batch entity to cancel
     */
    public void cancel(Long id) {
        getEntityManager().createNamedQuery("BatchEntity.cancelOpenedBatchEntity").setParameter("id", id).executeUpdate();
    }

    /**
     * Update a huge entity.
     *
     * @param jobExecutionResult Job execution result
     */
    public void updateHugeEntity(JobExecutionResultImpl jobExecutionResult) {
        String entityClassName = getHugeEntityClassName(jobExecutionResult);
        String targetJob = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_TARGET_JOB);
        if (StringUtils.isBlank(targetJob)) {
            throw new BusinessException("the target job is missing!");
        }
        List<BatchEntity> batchEntities = getEntityManager().createNamedQuery("BatchEntity.getOpenedBatchEntity")
                .setParameter("targetJob", targetJob).getResultList();
        JobInstance jobInstance = jobExecutionResult.getJobInstance();
        for (BatchEntity batchEntity : batchEntities) {
            processBatchEntity(jobExecutionResult, jobInstance, batchEntity, entityClassName);
        }
    }

    /**
     * Process a batch entity
     *
     * @param jobExecutionResult job execution result
     * @param jobInstance        job instance
     * @param batchEntity        batch entity
     * @param entityClassName    entity class name
     */
    private void processBatchEntity(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, BatchEntity batchEntity, String entityClassName) {
        try {
            updateHugeEntity(jobExecutionResult, jobInstance, batchEntity, entityClassName);
        } catch (Exception e) {
            log.error("Failed to process the entity batch id : {}", batchEntity.getId(), e);
            jobExecutionResult.registerError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            if (isRunningAsJobManager(jobExecutionResult)) {
                batchEntity.setJobInstance(jobInstance);
                batchEntity.setStatus(BatchEntityStatusEnum.FAILURE);
                update(batchEntity);
            }
        }
    }

    /**
     * Check is running as job manager and not worker one
     *
     * @param jobExecutionResult the job execution result
     * @return true if is running as job manager
     */
    private boolean isRunningAsJobManager(JobExecutionResultImpl jobExecutionResult) {
        return jobExecutionResult.getJobLauncherEnum() != JobLauncherEnum.WORKER;
    }

    /**
     * Update batch entity
     *
     * @param jobExecutionResult the job execution result
     * @param jobInstance        the job instance
     * @param batchEntity        the batch entity
     */
    private void update(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, BatchEntity batchEntity) {
        if (isRunningAsJobManager(jobExecutionResult)) {
            batchEntity.setJobInstance(jobInstance);
            if (hasError(jobExecutionResult)) {
                batchEntity.setStatus(BatchEntityStatusEnum.FAILURE);
            } else {
                batchEntity.setStatus(BatchEntityStatusEnum.SUCCESS);
            }
            update(batchEntity);
        }
    }

    /**
     * Check if the batch entity has an error.
     *
     * @return true if the batch entity has an error.
     */
    private boolean hasError(JobExecutionResultImpl jobExecutionResult) {
        for (JobExecutionResultImpl workerJobExecutionResult : jobExecutionResult.getWorkerJobExecutionResults()) {
            if (workerJobExecutionResult.getNbItemsProcessedWithError() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the class name of huge entity
     *
     * @param jobExecutionResult the job execution result
     * @return the class name of huge entity
     */
    public String getHugeEntityClassName(JobExecutionResultImpl jobExecutionResult) {
        String targetEntity = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_ENTITY_ClASS_NAME);
        return (getHugeEntityClass(targetEntity)).getSimpleName();
    }

    /**
     * Get the class of huge entity
     *
     * @param targetEntity the target entity
     * @return the class name of huge entity
     */
    public Class getHugeEntityClass(String targetEntity) {
        if (StringUtils.isBlank(targetEntity)) {
            throw new BusinessException("the entity class name is missing!");
        }
        Class entityClass = null;
        try {
            entityClass = Class.forName(targetEntity);
        } catch (ClassNotFoundException e) {
            throw new BusinessException("Unknown classname " + targetEntity + ". Please provide a valid entity classname");
        }
        return entityClass;
    }

    /**
     * Update a multiple Wallet operations to rerate for one batch entity
     *
     * @param jobExecutionResult job execution result
     * @param jobInstance        job instance
     * @param batchEntity        batch entity
     * @param entityClassName    entity class name
     */
    private void updateHugeEntity(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, BatchEntity batchEntity, String entityClassName) {
        executeMassUpdaterJob(jobExecutionResult, jobInstance, batchEntity, entityClassName);
        if (isRunningAsJobManager(jobExecutionResult)) {
            //Execute the email sending in an isolated transaction ==> if there is an exception, we don't position the batch in FAILURE status.
            methodCallingUtils.callMethodInNewTx(() -> sendEmail(batchEntity, jobExecutionResult));
            update(jobExecutionResult, jobInstance, batchEntity);
        }
    }

    /**
     * Add the default filter to batch filters
     *
     * @param jobExecutionResult the job execution result
     * @param filters            batch filters
     * @return all filters (new + default)
     */
    private Map<String, Object> addFilters(JobExecutionResultImpl jobExecutionResult, Map<String, Object> filters) {
        Map<String, Object> mergedFilters = new HashMap<>(filters);
        String defaultFilter = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_DEFAULT_FILTER);
        if (!StringUtils.isBlank(defaultFilter)) {
            try {
                Map<String, Object> result = new ObjectMapper().readValue(defaultFilter, HashMap.class);
                if (result != null && result.get("filters") != null) {
                    Map<String, Object> subFilters = (Map<String, Object>) result.get("filters");
                    for (Map.Entry<String, Object> mapItem : subFilters.entrySet()) {
                        if (mapItem.getKey().matches("\\$filter[0-9]+$")) {
                            mergedFilters.put(mapItem.getKey() + "0000000", mapItem.getValue());
                        } else {
                            mergedFilters.put(mapItem.getKey(), mapItem.getValue());
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                throw new BusinessException("The default filter is invalid!");
            }
        }
        return mergedFilters;
    }

    /**
     * Execute the mass updater job
     *
     * @param jobExecutionResult the job execution result
     * @param jobInstance        the job instance
     * @param batchEntity        the batch entity
     * @param entityClassName    entity class name
     */
    private void executeMassUpdaterJob(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, BatchEntity batchEntity, String entityClassName) {
        Long selectLimit = (Long) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_SELECT_LIMIT);
        Long updateChunk = (Long) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_UPDATE_CHUNK);
        Boolean isPessimisticUpdateLock = (Boolean) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_IS_PESSIMISTIC_UPDATE_LOCK);

        Map<String, Object> filters = addFilters(jobExecutionResult, batchEntity.getFilters());

        String selectQuery = getSelectQuery(entityClassName, filters);
        String updateQuery = getUpdateQuery(jobExecutionResult, batchEntity, entityClassName);
        massUpdaterJobBean.execute(jobExecutionResult, jobInstance, null, updateQuery, updateChunk, selectQuery, selectLimit, false, isPessimisticUpdateLock);
    }

    /**
     * Build the update query with the provided fields
     *
     * @param jobExecutionResult the job execution result
     * @param batchEntity        the batch entity
     * @param entityClassName    the entity class name
     */
    public String getUpdateQuery(JobExecutionResultImpl jobExecutionResult, BatchEntity batchEntity, String entityClassName) {
        StringBuilder updateQuery = new StringBuilder("UPDATE ").append(entityClassName).append(" SET ")
                .append("updated=").append(QueryBuilder.paramToString(new Date()))
                .append(", reratingBatch.id=").append(batchEntity.getId());

        String fieldsToUpdate = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_FIELDS_TO_UPDATE);
        if (!StringUtils.isBlank(fieldsToUpdate)) {
            updateQuery = updateQuery.append(", ").append(fieldsToUpdate);
        }
        return updateQuery.toString();
    }

    /**
     * Gets the select query
     *
     * @param entityClassName the entity class name
     * @param filters         the filters
     * @return the select query
     */
    private String getSelectQuery(String entityClassName, Map<String, Object> filters) {
        PaginationConfiguration searchConfig = new PaginationConfiguration(filters);
        searchConfig.setFetchFields(Arrays.asList("id"));
        return nativePersistenceService.getQuery(entityClassName, searchConfig, null).getQueryAsString();
    }

    /**
     * Send Email to the creator
     *
     * @param batchEntity        Batch entity
     * @param jobExecutionResult Job execution result
     */
    private void sendEmail(BatchEntity batchEntity, JobExecutionResultImpl jobExecutionResult) {
        EntityReferenceWrapper emailTemplateWrapper = (EntityReferenceWrapper) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_EMAIL_TEMPLATE);
        if (batchEntity.isNotify() && emailTemplateWrapper != null && !StringUtils.isBlank(emailTemplateWrapper.getCode())) {
            try {
                String emailTemplateCode = emailTemplateWrapper.getCode();
                EmailTemplate emailTemplate = ofNullable(emailTemplateService.findByCode(emailTemplateCode))
                        .orElseThrow(() -> new EntityDoesNotExistsException(EmailTemplate.class, emailTemplateCode));

                String from = DEFAULT_EMAIL_ADDRESS;
                Provider provider = providerService.getProvider();
                if (provider != null && StringUtils.isNotBlank(provider.getEmail())) {
                    from = provider.getEmail();
                }
                String username = batchEntity.getAuditable().getCreator();
                User user = userService.findByUsername(username, false);
                if (user == null) {
                    log.warn("No user with username {} was found", username);
                    throw new BusinessException("No user with username " + username + " was found");
                }
                String to = user.getEmail();
                if (StringUtils.isBlank(to)) {
                    log.warn("Cannot send batch entity notification message to the creator {}, because he doesn't have an email", username);
                    throw new BusinessException("Cannot send batch entity notification message to the creator " + username + ", because he doesn't have an email");
                }
                String localeAttribute = userService.getUserAttributeValue(username, "locale");
                Locale locale = !StringUtils.isBlank(localeAttribute) ? new Locale(localeAttribute) : new Locale("en");
                String languageCode = locale.getISO3Language().toUpperCase();

                String emailSubject = internationalSettingsService.resolveSubject(emailTemplate, languageCode);
                String emailContent = internationalSettingsService.resolveEmailContent(emailTemplate, languageCode);
                String htmlContent = internationalSettingsService.resolveHtmlContent(emailTemplate, languageCode);


                Map<Object, Object> params = new HashMap<>();
                params.put("batchEntityId", batchEntity.getId());
                params.put("batchEntityStatus", batchEntity.getStatus());
                params.put("batchEntityDescription", StringUtils.isNotBlank(batchEntity.getDescription()) ? batchEntity.getDescription() : "");
                params.put("jobExecutionId", jobExecutionResult.getId() != null ? jobExecutionResult.getId() : "");
                params.put("jobInstanceCode", batchEntity.getJobInstance() != null ? batchEntity.getJobInstance().getCode() : "");

                String subject = StringUtils.isNotBlank(emailTemplate.getSubject()) ? evaluateExpression(emailSubject, params, String.class) : "";
                String content = StringUtils.isNotBlank(emailTemplate.getTextContent()) ? evaluateExpression(emailContent, params, String.class) : "";
                String contentHtml = StringUtils.isNotBlank(emailTemplate.getHtmlContent()) ? evaluateExpression(htmlContent, params, String.class) : "";

                emailSender.send(from, asList(from), asList(to), subject, content, contentHtml);
                //in the case of an exception, don't reject the batch entity and don't record an error or a warning but rather add a message in the reports.
            } catch (Exception e) {
                jobExecutionResult.addReport("Warning : can not send email (" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()) + ")");
            }
        }

    }

    /**
     * Gets the batch entity code
     *
     * @param defaultCode the default batch entity code
     * @return the batch entity code
     */
    public String getBatchEntityCode(String defaultCode) {
        String code = defaultCode;
        if (StringUtils.isBlank(code)) {
            CustomGenericEntityCode customGenericEntityCode = ofNullable(customGenericEntityCodeService.findByClass(BatchEntity.class.getName()))
                    .orElseThrow(() -> new BusinessException("Generic code does not exist for the BatchEntity class."));
            code = serviceSingleton.getGenericCode(customGenericEntityCode);
        }
        return code;
    }
}