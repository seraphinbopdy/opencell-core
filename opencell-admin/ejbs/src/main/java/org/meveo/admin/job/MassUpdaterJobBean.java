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

import org.hibernate.Session;
import org.meveo.admin.exception.BusinessException;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Job definition to do the mass update.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
public abstract class MassUpdaterJobBean extends IteratorBasedJobBean<List<Long>> {

    protected static final String VIEW_NAME = "mass_update";

    @Inject
    @MeveoJpa
    protected EntityManagerWrapper emWrapper;

    /**
     * Initializes intervals for updating.
     *
     * @return An iterator of Long[] representing intervals.
     */
    protected abstract Optional<Iterator<List<Long>>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult);

    /**
     * Process an update operation for a specific interval.
     *
     * @param interval           The interval to update.
     * @param jobExecutionResult The job execution result.
     */
    protected abstract void processUpdateQueries(List<Long> interval, JobExecutionResultImpl jobExecutionResult);

    /**
     * Terminate function
     *
     * @param jobExecutionResult Job execution result
     */
    protected abstract void terminate(JobExecutionResultImpl jobExecutionResult);

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
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, String namedQuery, String updateQuery, Long updateChunkSize,
                        String selectQuery, Long selectFetchSize, Long selectMaxResults, Boolean isNativeQuery, Boolean isPessimisticUpdateLock) {

        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_NAMED_QUERY, namedQuery);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_UPDATE_QUERY, updateQuery);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_UPDATE_CHUNK_SIZE, updateChunkSize);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_SELECT_QUERY, selectQuery);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_SELECT_FETCH_SIZE, selectFetchSize);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_SELECT_MAX_RESULTS, selectMaxResults);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_IS_NATIVE_QUERY, isNativeQuery);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_IS_PESSIMISTIC_UPDATE_LOCK, isPessimisticUpdateLock);
        execute(jobExecutionResult, jobInstance);
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
     * @param usingView               indicates if the job will be use the view or not.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, String namedQuery, String updateQuery, Long updateChunkSize,
                        String selectQuery, Long selectFetchSize, Long selectMaxResults, Boolean isNativeQuery, Boolean isPessimisticUpdateLock, Boolean usingView) {

        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_IS_USING_VIEW, usingView);
        execute(jobExecutionResult, jobInstance, namedQuery, updateQuery, updateChunkSize, selectQuery, selectFetchSize, selectMaxResults, isNativeQuery, isPessimisticUpdateLock);
    }

    /**
     * Get the update query
     *
     * @param jobExecutionResult the job execution result
     * @return the update query
     */
    protected String getUpdateQuery(JobExecutionResultImpl jobExecutionResult) {
        String updateString = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_UPDATE_QUERY);
        String clauseWhere = " WHERE id in :ids";
        return !updateString.toUpperCase().contains("WHERE") ? updateString + clauseWhere :
                updateString.replace(updateString.substring(updateString.toUpperCase().indexOf("WHERE")), clauseWhere);
    }

    /**
     * Indicates if the query is native or not.
     *
     * @param jobExecutionResult the job execution result
     * @return true if the query is native.
     */
    protected boolean isNativeQuery(JobExecutionResultImpl jobExecutionResult) {
        return jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_IS_NATIVE_QUERY) != null ?
                (boolean) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_IS_NATIVE_QUERY) : false;
    }

    /**
     * Indicates if all update queries will be run on distinct IDs or whether it doesn't matter.
     *
     * @param jobExecutionResult the job execution result
     * @return true if all update queries will be run on distinct IDs.
     */
    protected boolean isPessimisticUpdateLock(JobExecutionResultImpl jobExecutionResult) {
        return jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_IS_PESSIMISTIC_UPDATE_LOCK) != null ?
                (boolean) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_IS_PESSIMISTIC_UPDATE_LOCK) : false;
    }

    /**
     * Indicates if the job will be use the view or not.
     *
     * @param jobExecutionResult the job execution result
     * @return true if the job will be use the view.
     */
    protected boolean isUsingView(JobExecutionResultImpl jobExecutionResult) {
        return jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_IS_USING_VIEW) != null ?
                (boolean) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_IS_USING_VIEW) : false;
    }

    /**
     * Create new view with the provided sql
     *
     * @param sql the sql query
     */
    protected void createView(String sql) {
        EntityManager em = emWrapper.getEntityManager();
        Session hibernateSession = em.unwrap(Session.class);

        hibernateSession.doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                log.info("Dropping and creating materialized view {} with sql query {}: ", VIEW_NAME, sql);
                statement.execute("drop materialized view if exists " + VIEW_NAME);
                statement.execute("create materialized view " + VIEW_NAME + " (id) as (" + sql + ")");
                statement.execute("create index idx_" + VIEW_NAME + " on " + VIEW_NAME + " (id) ");
            } catch (Exception e) {
                log.error("Failed to drop/create the materialized view " + VIEW_NAME, e.getMessage());
                throw new BusinessException(e);
            }
        });
    }

    /**
     * Drop the view
     */
    protected void dropView() {
        EntityManager em = emWrapper.getEntityManager();
        Session hibernateSession = em.unwrap(Session.class);

        hibernateSession.doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                log.info("Dropping materialized view {}", VIEW_NAME);
                statement.execute("drop materialized view if exists " + VIEW_NAME);

            } catch (Exception e) {
                log.error("Failed to drop the materialized view " + VIEW_NAME, e.getMessage());
                throw new BusinessException(e);
            }
        });
    }
}