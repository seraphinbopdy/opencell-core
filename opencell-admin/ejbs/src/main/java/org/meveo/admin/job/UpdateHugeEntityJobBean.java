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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.IEntity;
import org.meveo.model.billing.BatchEntity;
import org.meveo.model.crm.EntityReferenceWrapper;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.BatchEntityService;
import org.meveo.service.script.PreUpdateHugeEntityScript;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.script.ScriptInterface;
import org.meveo.service.settings.impl.AdvancedSettingsService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * A job implementation to update huge entity
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class UpdateHugeEntityJobBean extends IteratorBasedScopedJobBean<Map.Entry<BatchEntity, IEntity>> {

    private static final long serialVersionUID = 1L;

    @Inject
    private BatchEntityService batchEntityService;

    @Inject
    private AdvancedSettingsService advancedSettingsService;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    private Set<BatchEntity> processedBatchEntities;

    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        String paramOrCFValue = (String) getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_PRE_UPDATE_EL);
        initJob(jobExecutionResult, jobInstance);
        if (!StringUtils.isBlank(paramOrCFValue)) {
            super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, this::updateWithCheck, null, null, this::finalizeProcess);
        } else {
            batchEntityService.checkAndUpdateHugeEntity(jobExecutionResult);
    }
    }

    private void finalizeProcess(JobExecutionResultImpl jobExecutionResult) {

        log.info("Finalizing process for job {}", jobExecutionResult.getJobInstance().getId());
        processedBatchEntities.forEach(be -> batchEntityService.finalizeProcess(jobExecutionResult, be));
        processedBatchEntities.clear();

    }

    private void updateWithCheck(Map.Entry<BatchEntity, IEntity> entry, JobExecutionResultImpl jobExecutionResult) {
        try {
            IEntity iEntity = entry.getValue();
            if (!batchEntityService.checkAndUpdateHugeEntity(iEntity, jobExecutionResult)) {
                jobExecutionResult.unRegisterSucces();
                jobExecutionResult.registerError(iEntity.getId(), "Entity " + iEntity.getId() + " was not updated because the pre update EL returned false");
            }
        } catch (Exception e) {
            log.error(String.format("Failed to update entity %s", entry.getValue().getId()), e);
            jobExecutionResult.unRegisterSucces();
            jobExecutionResult.registerError(entry.getValue().getId(), e.getMessage());
        }
    }

    /**
     * Initialize the job parameters.
     *
     * @param jobExecutionResult the job execution result
     * @param jobInstance        the job instance
     */
    private void initJob(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_TARGET_JOB, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_TARGET_JOB));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_ENTITY_ClASS_NAME, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_ENTITY_ClASS_NAME));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_FIELDS_TO_UPDATE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_FIELDS_TO_UPDATE));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_DEFAULT_FILTER, getFilter(jobInstance));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_EMAIL_TEMPLATE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_EMAIL_TEMPLATE));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_SELECT_FETCH_SIZE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_SELECT_FETCH_SIZE));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_SELECT_MAX_RESULTS, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_SELECT_MAX_RESULTS));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_UPDATE_CHUNK_SIZE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_UPDATE_CHUNK_SIZE));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_IS_PESSIMISTIC_UPDATE_LOCK, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_IS_PESSIMISTIC_UPDATE_LOCK));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_IS_USING_VIEW, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_IS_USING_VIEW));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_IS_OPEN_CURSOR, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_IS_OPEN_CURSOR));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_IS_CASE_SENSITIVE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_IS_CASE_SENSITIVE));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_PRE_UPDATE_EL, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_PRE_UPDATE_EL));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_BATCHES_TO_PROCESS, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_BATCHES_TO_PROCESS));
    }

    private Optional<Iterator<Map.Entry<BatchEntity, IEntity>>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        // pretreatment

        // fetch entities to process based batch entities waiting for processing
        return getIterator(jobExecutionResult);
    }

    /**
     * Get data to process from the batch entities
     *
     * @param jobExecutionResult the job execution result
     * @param jobItemsLimit      the job items limit
     * @return An iterator over a list of entities to process
     */
    @Override
    Optional<Iterator<Map.Entry<BatchEntity, IEntity>>> getSynchronizedIteratorWithLimit(JobExecutionResultImpl jobExecutionResult, int jobItemsLimit) {

        List<BatchEntity> batchEntitiesToProcess = batchEntityService.getBatchEntitiesToProcess(jobExecutionResult);
        Class<? extends IEntity> hugeEntityClass = batchEntityService.getHugeEntityClass(jobExecutionResult);

        String defaultFilter = (String) jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_DEFAULT_FILTER);
        boolean isCaseSensitive = Boolean.TRUE.equals(jobExecutionResult.getJobParam(UpdateHugeEntityJob.CF_IS_CASE_SENSITIVE));


        // Get IEntity to process from each BatchEntity and merge them in a unique list to process
        List<Map.Entry<BatchEntity, IEntity>> entitiesToProcess = batchEntitiesToProcess.stream()
                .flatMap(be -> batchEntityService.getDataToProcessByBatchEntity(be, hugeEntityClass, defaultFilter, isCaseSensitive)
                        .stream()
                        .map(ie -> Map.entry(be, ie)))
                .collect(Collectors.toList());

        processedBatchEntities = entitiesToProcess.stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        return Optional.of(new SynchronizedIterator<>(entitiesToProcess));
    }

    @Override
    Optional<Iterator<Map.Entry<BatchEntity, IEntity>>> getSynchronizedIterator(JobExecutionResultImpl jobExecutionResult) {
        return getSynchronizedIteratorWithLimit(jobExecutionResult, 0);
    }

    private String getFilter(JobInstance jobInstance) {
        String filter = null;
        try {
            String preUpdateScriptCode = null;
            EntityReferenceWrapper entityReferenceWrapper = ((EntityReferenceWrapper) this.getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_PRE_UPDATE_SCRIPT));
            if (entityReferenceWrapper != null) {
                preUpdateScriptCode = entityReferenceWrapper.getCode();
            }
            if (StringUtils.isBlank(preUpdateScriptCode)) {
                Map<String, Object> advancedSettingsValues = advancedSettingsService.getAdvancedSettingsMapByGroup("rating", Object.class);
                Boolean allowBilledItemsRerating = (Boolean) advancedSettingsValues.get("rating.allowBilledItemsRerating");
                if (Boolean.TRUE.equals(allowBilledItemsRerating)) {
                    preUpdateScriptCode = PreUpdateHugeEntityScript.MARK_WO_TO_RERATE_PRE_SCRIPT;
                }
            }

            if (StringUtils.isNotBlank(preUpdateScriptCode)) {
                if (log.isDebugEnabled()) {
                    log.debug(" looking for ScriptInstance with code :  [{}] ", preUpdateScriptCode);
                }
                ScriptInterface si = scriptInstanceService.getScriptInstance(preUpdateScriptCode);
                if (si instanceof PreUpdateHugeEntityScript) {
                    Map<String, Object> methodContext = new HashMap<>();
                    filter = ((PreUpdateHugeEntityScript) si).getFilter(methodContext);
                }
            }
        } catch (Exception e) {
            log.error(" Error on PreUpdateHugeEntityScript execution : [{}]", e.getMessage());
        }
        return !StringUtils.isBlank(filter) ? filter : (String) getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_DEFAULT_FILTER);
    }

}