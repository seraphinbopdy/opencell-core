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

import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.BatchEntityService;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * A job implementation to update huge entity
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class UpdateHugeEntityJobBean extends BaseJobBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private BatchEntityService batchEntityService;

    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        initJob(jobExecutionResult, jobInstance);
        batchEntityService.updateHugeEntity(jobExecutionResult);
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
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_DEFAULT_FILTER, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_DEFAULT_FILTER));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_EMAIL_TEMPLATE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_EMAIL_TEMPLATE));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_SELECT_FETCH_SIZE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_SELECT_FETCH_SIZE));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_SELECT_MAX_RESULTS, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_SELECT_MAX_RESULTS));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_UPDATE_CHUNK_SIZE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_UPDATE_CHUNK_SIZE));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_IS_PESSIMISTIC_UPDATE_LOCK, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_IS_PESSIMISTIC_UPDATE_LOCK));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_IS_USING_VIEW, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_IS_USING_VIEW));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_IS_OPEN_CURSOR, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_IS_OPEN_CURSOR));
        jobExecutionResult.addJobParam(UpdateHugeEntityJob.CF_IS_CASE_SENSITIVE, getParamOrCFValue(jobInstance, UpdateHugeEntityJob.CF_IS_CASE_SENSITIVE));
    }
}