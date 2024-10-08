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
import org.apache.commons.collections.CollectionUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.MassUpdaterJob;
import org.meveo.admin.job.UpdateHugeEntityJob;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.generics.GenericRequestMapper;
import org.meveo.api.generics.PersistenceServiceHelper;
import org.meveo.apiv2.common.HugeEntity;
import org.meveo.commons.utils.MethodCallingUtils;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.IEntity;
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
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.communication.impl.EmailSender;
import org.meveo.service.communication.impl.EmailTemplateService;
import org.meveo.service.communication.impl.InternationalSettingsService;
import org.meveo.service.crm.impl.ProviderService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.ListUtils.partition;
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
    private MassUpdaterJob massUpdaterJob;

    @Inject
    private MethodCallingUtils methodCallingUtils;

    @Inject
    private ResourceBundle resourceMessages;

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
    public void checkAndUpdateHugeEntity(JobExecutionResultImpl jobExecutionResult) {
        Class<? extends IEntity> hugeEntityClass = getHugeEntityClass(jobExecutionResult);
        Set<Long> batchEntityIds = getBatchEntities(jobExecutionResult);
        JobInstance jobInstance = jobExecutionResult.getJobInstance();
        for (Long batchEntityId : batchEntityIds) {
            methodCallingUtils.callMethodInNewTx(() -> processBatchEntity(jobExecutionResult, jobInstance, batchEntityId, hugeEntityClass));
        }
    }

    /**
     * Process a batch entity
     *
     * @param jobExecutionResult job execution result
     * @param jobInstance        job instance
     * @param batchEntityId      batch entity id
     * @param hugeEntityClass    huge entity class
     */
    private void processBatchEntity(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, Long batchEntityId, Class hugeEntityClass) {
        BatchEntity batchEntity = findById(batchEntityId);
        try {
            checkAndUpdateHugeEntity(jobExecutionResult, jobInstance, batchEntity, hugeEntityClass);
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
     * Get the class of huge entity
     *
     * @param jobExecutionResult the job execution result
     * @return the class name of huge entity
     */
    public Class<? extends IEntity> getHugeEntityClass(JobExecutionResultImpl jobExecutionResult) {
        String targetEntity = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_ENTITY_ClASS_NAME);
        return getHugeEntityClass(targetEntity);
    }

    /**
     * Get the class of huge entity
     *
     * @param targetEntity the target entity
     * @return the class name of huge entity
     */
    public Class<? extends IEntity> getHugeEntityClass(String targetEntity) {
        if (StringUtils.isBlank(targetEntity)) {
            throw new BusinessException("the entity class name is missing!");
        }
        Class<? extends IEntity> entityClass = null;
        try {
            entityClass = Class.forName(targetEntity).asSubclass(IEntity.class);
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
     * @param hugeEntityClass    huge entity class
     */
    private void checkAndUpdateHugeEntity(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, BatchEntity batchEntity, Class hugeEntityClass) {
        executeMassUpdaterJob(jobExecutionResult, jobInstance, batchEntity, hugeEntityClass);
        if (isRunningAsJobManager(jobExecutionResult)) {
            update(jobExecutionResult, jobInstance, batchEntity);
            if (jobExecutionResult.getNbItemsCorrectlyProcessed() > 0) {
                //Execute the email sending in an isolated transaction ==> if there is an exception, we don't position the batch in FAILURE status.
                methodCallingUtils.callMethodInNewTx(() -> sendEmail(batchEntity, jobExecutionResult));
            }
        }
    }

    /**
     * Add the default filter to batch filters
     *
     * @param defaultFilter the default filter
     * @param filters       batch filters
     * @return all filters (new + default)
     */
    public Map<String, Object> addFilters(String defaultFilter, Map<String, Object> filters) {
        Map<String, Object> mergedFilters = new HashMap<>(ofNullable(filters).orElse(Collections.emptyMap()));
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
     * @param hugeEntityClass    huge entity class
     */
    private void executeMassUpdaterJob(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, BatchEntity batchEntity, Class hugeEntityClass) {
        Long selectFetchSize = (Long) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_SELECT_FETCH_SIZE);
        Long selectMaxResults = (Long) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_SELECT_MAX_RESULTS);
        Long updateChunkSize = (Long) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_UPDATE_CHUNK_SIZE);
        String defaultFilter = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_DEFAULT_FILTER);
        Boolean isPessimisticUpdateLock = (Boolean) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_IS_PESSIMISTIC_UPDATE_LOCK);
        Boolean isUsingView = (Boolean) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_IS_USING_VIEW);
        Boolean isOpenCursor = isOpenCursor(jobExecutionResult);
        Boolean isCaseSensitive = isCaseSensitive(jobExecutionResult);

        String selectQuery = getSelectQuery(hugeEntityClass, batchEntity.getFilters(), defaultFilter, isCaseSensitive);
        String updateQuery = getUpdateQuery(jobExecutionResult, batchEntity, hugeEntityClass.getSimpleName());

        massUpdaterJob.execute(jobExecutionResult, jobInstance, null, updateQuery, updateChunkSize, selectQuery, selectFetchSize, selectMaxResults,
                false, isPessimisticUpdateLock, isUsingView, isOpenCursor);
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
                .append("updated=").append(QueryBuilder.paramToString(new Date()));

        if (batchEntity != null) {
            updateQuery.append(", reratingBatch.id=").append(batchEntity.getId());
        }

        String fieldsToUpdate = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_FIELDS_TO_UPDATE);
        if (!StringUtils.isBlank(fieldsToUpdate)) {
            updateQuery = updateQuery.append(", ").append(fieldsToUpdate);
        }
        return updateQuery.toString();
    }

    /**
     * Gets the select query
     *
     * @param hugeEntityClass the hug entity class
     * @param filters         the filters
     * @param defaultFilter   the default filter
     * @param isCaseSensitive Indicates if the select query should use strict checking to compare two strings or not.
     * @return the select query
     */
    public String getSelectQuery(Class hugeEntityClass, Map<String, Object> filters, String defaultFilter, boolean isCaseSensitive) {
        Map<String, Object> mergedFilters = addFilters(defaultFilter, filters);
        if (isCaseSensitive) {
            GenericRequestMapper mapper = new GenericRequestMapper(hugeEntityClass, PersistenceServiceHelper.getPersistenceService());
            mergedFilters = mapper.evaluateFilters(Objects.requireNonNull(mergedFilters), hugeEntityClass);
        }
        PaginationConfiguration searchConfig = new PaginationConfiguration(mergedFilters);
        searchConfig.setFetchFields(Arrays.asList("id"));
        return nativePersistenceService.getQuery(hugeEntityClass.getSimpleName(), searchConfig, null, Boolean.FALSE).getQueryAsString();
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
                params.put("jobExecutionNbItemsProcessed", jobExecutionResult.getNbItemsProcessed());

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

    /**
     * Update entities through their ids
     *
     * @param updateQuery the update query which will be executed
     * @param ids         the ids of entities to be updated
     * @return the number of updated Wallet operations
     */
    public int update(StringBuilder updateQuery, List<Long> ids) {
        return nativePersistenceService.update(updateQuery, ids);
    }

    /**
     * Indicates if the job will use the open cursor or not.
     *
     * @param jobExecutionResult the job execution result
     * @return true if the job will use the open cursor.
     */
    protected boolean isOpenCursor(JobExecutionResultImpl jobExecutionResult) {
        return jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_IS_OPEN_CURSOR) != null ?
                (boolean) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_IS_OPEN_CURSOR) : false;
    }

    /**
     * Indicates if the select query should use strict checking to compare two strings or not.
     *
     * @param jobExecutionResult the job execution result
     * @return true  if the select query should use strict checking to compare two strings or not.
     */
    protected boolean isCaseSensitive(JobExecutionResultImpl jobExecutionResult) {
        return jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_IS_CASE_SENSITIVE) != null ?
                (boolean) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_IS_CASE_SENSITIVE) : false;
    }

    public List<BatchEntity> getBatchEntitiesToProcess(JobExecutionResultImpl jobExecutionResult) {
        String targetJob = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_TARGET_JOB);
        if (StringUtils.isBlank(targetJob)) {
            throw new BusinessException("the target job is missing!");
        }
        Set<BatchEntity> batchEntities = getBatchEntities(jobExecutionResult, targetJob);
        if (batchEntities != null) {
            return new ArrayList<>(batchEntities);
        }
        List<BatchEntity> batchsToProcess = getEntityManager().createQuery("FROM BatchEntity be WHERE be.status=:status AND be.targetJob=:targetJob", BatchEntity.class)
                .setParameter("status", BatchEntityStatusEnum.OPEN)
                .setParameter("targetJob", jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_TARGET_JOB))
                .getResultList();
        return batchsToProcess;
    }

    public List<IEntity> getDataToProcessByBatchEntity(BatchEntity batchEntity, Class hugeEntityClass, String defaultFilter, boolean isCaseSensitive) {
        String selectQuery = getSelectQuery(hugeEntityClass, batchEntity.getFilters(), defaultFilter, isCaseSensitive);

        List<Long> ids = getEntityManager().createQuery(selectQuery, Long.class)
                .getResultList();

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        return getEntities(hugeEntityClass, ids);
    }

    public List<IEntity> getEntities(Class hugeEntityClass, List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return getEntityManager().createQuery("FROM " + hugeEntityClass.getSimpleName() + " WHERE id IN (:ids)", IEntity.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public boolean checkAndUpdateHugeEntity(IEntity entity, JobExecutionResultImpl jobExecutionResult) {

        String updateQuery = getUpdateQuery(jobExecutionResult, null, entity.getClass().getSimpleName());
        return checkAndUpdateEntity(entity, (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_PRE_UPDATE_EL), updateQuery);
    }

    public boolean checkAndUpdateEntity(IEntity entity, String preUpdateEL, String updateQuery) {

        var context = ValueExpressionWrapper.completeContext(preUpdateEL, new HashMap<>(), entity);
        boolean shouldUpdate = ValueExpressionWrapper.evaluateToBoolean(preUpdateEL, context);

        if (shouldUpdate) {
            update(new StringBuilder(updateQuery), List.of((Long) entity.getId()));
            return true;
        }

        return false;
    }

    /**
     * Finalize Batch processing - (main usage from UpdateHugeEntityJobBean)
     *
     * @param jobExecutionResult the job execution result
     * @param batchEntity        the batch entity
     */
    public void finalizeProcess(JobExecutionResultImpl jobExecutionResult, BatchEntity batchEntity) {
        log.info("finalizeProcess for batchEntity {} for the jobInstance {}", batchEntity.getId(), jobExecutionResult.getJobInstance().getId());
        update(jobExecutionResult, jobExecutionResult.getJobInstance(), batchEntity);
        sendEmail(batchEntity, jobExecutionResult);
    }

    /**
     * Get batch entity ids to process
     *
     * @param jobExecutionResult the job execution result
     * @param targetJob          the targetJob
     * @return the batch entity ids to process
     */
    public Set<BatchEntity> getBatchEntities(JobExecutionResultImpl jobExecutionResult, String targetJob) {
        List<EntityReferenceWrapper> batchEntityWrappers = (List<EntityReferenceWrapper>) jobExecutionResult.getJobParam(UpdateHugeEntityJob.BATCHES_TO_PROCESS);
        if (CollectionUtils.isEmpty(batchEntityWrappers)) {
            return null;
        }

        final Set<BatchEntity> batchEntities = new HashSet<>();
        List<String> selectedBatchEntityCodes = batchEntityWrappers.stream().map(EntityReferenceWrapper::getCode).collect(Collectors.toList());

        String jobInstanceCode = jobExecutionResult.getJobInstance().getCode();
        List<List<String>> listOfSubListCodes = partition(selectedBatchEntityCodes, SHORT_MAX_VALUE);
        listOfSubListCodes.forEach(sublist -> {
            if (sublist != null && !sublist.isEmpty()) {
                List<BatchEntity> batchsToProcess = getEntityManager().createQuery("FROM BatchEntity be WHERE be.code in (:codes)", BatchEntity.class)
                        .setParameter("codes", sublist)
                        .getResultList();

                for (BatchEntity batchEntity : batchsToProcess) {
                    if (targetJob.equals(batchEntity.getTargetJob()) && batchEntity.getStatus() == BatchEntityStatusEnum.OPEN) {
                        batchEntities.add(batchEntity);
                    } else {
                        if (!targetJob.equals(batchEntity.getTargetJob())) {
                            log.warn(resourceMessages.getString("batchEntityService.targetJob.warning", batchEntity.getId(), batchEntity.getTargetJob(), jobInstanceCode));
                        }
                        if (batchEntity.getStatus() != BatchEntityStatusEnum.OPEN) {
                            log.warn(resourceMessages.getString("batchEntityService.status.warning", batchEntity.getId(), batchEntity.getStatus()));
                        }
                    }
                }
            }
        });
        return batchEntities;
    }

    /**
     * Get batch entity ids to process
     *
     * @param jobExecutionResult the job execution result
     * @return batch entity ids to process
     */
    private Set<Long> getBatchEntities(JobExecutionResultImpl jobExecutionResult) {
        String targetJob = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_TARGET_JOB);
        if (StringUtils.isBlank(targetJob)) {
            throw new BusinessException("the target job is missing!");
        }
        Set<BatchEntity> batchEntities = getBatchEntities(jobExecutionResult, targetJob);
        if (batchEntities != null) {
            return batchEntities.stream().map(BatchEntity::getId).collect(Collectors.toSet());
        } else {
            return new HashSet<Long>(getEntityManager().createNamedQuery("BatchEntity.getOpenedBatchEntityIds")
                    .setParameter("targetJob", targetJob).getResultList());
        }
    }
}