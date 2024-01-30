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

import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.meveo.admin.async.SynchronizedMultiItemIterator;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.base.NativePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Job definition to do the mass update.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class MassUpdaterJobBean extends IteratorBasedJobBean<List<Long>> {

    private static final Logger log = LoggerFactory.getLogger(MassUpdaterJobBean.class);


    @Inject
    @MeveoJpa
    private EntityManagerWrapper emWrapper;

    private StatelessSession statelessSession;
    private ScrollableResults scrollableResults;

    /**
     * Execute job
     *
     * @param jobExecutionResult the job execution result
     * @param jobInstance        the job instance
     * @param namedQuery         the named query
     * @param updateQuery        the update query
     * @param updateChunk        the chunk of update query
     * @param selectQuery        the select query
     * @param selectAliasTable   the select alias table
     * @param selectLimit        the limit of select query
     * @param isNativeQuery      indicates if the query is native or not
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, String namedQuery, String updateQuery, Long updateChunk,
                        String selectQuery, String selectAliasTable, Long selectLimit, boolean isNativeQuery) {

        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_NAMED_QUERY, namedQuery);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_UPDATE_QUERY, updateQuery);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_UPDATE_CHUNK, updateChunk);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_SELECT_QUERY, selectQuery);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_SELECT_TABLE_ALIAS, selectAliasTable);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_SELECT_LIMIT, selectLimit);
        jobExecutionResult.addJobParam(MassUpdaterJob.PARAM_IS_NATIVE_QUERY, isNativeQuery);
        execute(jobExecutionResult, jobInstance);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, null, this::processUpdateQueries, null, null, this::closeResultset, null);
    }

    /**
     * Initializes intervals for updating based on the specified chunk size.
     *
     * @return An iterator of Long[] representing intervals.
     */
    public Optional<Iterator<List<Long>>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        //namedQuery parameter provided only by other jobs or services through code and not through the GUI.
        String namedQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_NAMED_QUERY);
        String updateString = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_UPDATE_QUERY);
        String selectQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_QUERY);
        String tableAlias = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_TABLE_ALIAS);
        Long selectLimit = (Long) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_LIMIT);
        Long updateChunk = (Long) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_UPDATE_CHUNK);

        //Check the mandatory settings for mass update processing
        if (StringUtils.isEmpty(selectQuery) || (StringUtils.isBlank(namedQuery) && (StringUtils.isBlank(updateString) || StringUtils.isBlank(tableAlias)))) {
            log.error("params should not be null - selectQuery: {}, updateString: {}, tableAlias: {}, namedQuery: {}", selectQuery, updateString, tableAlias, namedQuery);
            return Optional.empty();
        }
        if (selectLimit == null) {
            selectLimit = ((long) Runtime.getRuntime().availableProcessors()) * Long.valueOf(NativePersistenceService.SHORT_MAX_VALUE);
        }
        if (updateChunk == null) {
            updateChunk = Long.valueOf(NativePersistenceService.SHORT_MAX_VALUE);
        }
        updateChunk = Math.min(updateChunk, selectLimit);

        scrollableResults = getScrollableResult(jobExecutionResult, selectQuery, selectLimit);
        Long finalUpdateChunk = updateChunk;
        return Optional.of(new SynchronizedMultiItemIterator<>(scrollableResults, -1) {
            long totalItemCount = 0L;

            @Override
            public void initializeDecisionMaking(Long item) {
                totalItemCount = 1;
                jobExecutionResult.setNbItemsToProcess(jobExecutionResult.getNbItemsToProcess() + 1);
            }

            @Override
            public boolean isIncludeItem(Long item) {
                if (totalItemCount > finalUpdateChunk) {
                    return false;
                }
                totalItemCount++;
                jobExecutionResult.setNbItemsToProcess(jobExecutionResult.getNbItemsToProcess() + 1);
                return true;
            }
        });
    }

    /**
     * Process an update operation for a specific interval.
     *
     * @param interval           The interval to update.
     * @param jobExecutionResult The job execution result.
     */
    private void processUpdateQueries(List<Long> interval, JobExecutionResultImpl jobExecutionResult) {
        String namedQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_NAMED_QUERY);
        if (!StringUtils.isBlank(namedQuery)) {
            emWrapper.getEntityManager().createNamedQuery(namedQuery).setParameter("ids", interval).executeUpdate();
        } else {
            String updateQuery = getUpdateQuery(jobExecutionResult);
            if (isNativeQuery(jobExecutionResult)) {
                emWrapper.getEntityManager().createNativeQuery(updateQuery).setParameter("ids", interval).executeUpdate();
            } else {
                emWrapper.getEntityManager().createQuery(updateQuery).setParameter("ids", interval).executeUpdate();
            }
        }
    }


    /**
     * Get the update query
     *
     * @param jobExecutionResult the job execution result
     * @return the update query
     */
    private String getUpdateQuery(JobExecutionResultImpl jobExecutionResult) {
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
    private boolean isNativeQuery(JobExecutionResultImpl jobExecutionResult) {
        return jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_IS_NATIVE_QUERY) != null ?
                (boolean) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_IS_NATIVE_QUERY) : true;
    }

    /**
     * Close data resultset
     *
     * @param jobExecutionResult Job execution result
     */
    private void closeResultset(JobExecutionResultImpl jobExecutionResult) {
        if (scrollableResults != null) {
            scrollableResults.close();
        }
        if (statelessSession != null) {
            statelessSession.close();
        }
    }

    /**
     * Get the scrollable results
     *
     * @param jobExecutionResult the job execution result
     * @param selectQuery        the select query
     * @param selectLimit        the select limit
     * @return the scrollable results
     */
    public ScrollableResults getScrollableResult(JobExecutionResultImpl jobExecutionResult, String selectQuery, Long selectLimit) {
        String namedQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_NAMED_QUERY);
        statelessSession = emWrapper.getEntityManager().unwrap(Session.class).getSessionFactory().openStatelessSession();
        if (!StringUtils.isBlank(namedQuery)) {
            scrollableResults = statelessSession.createNamedQuery(namedQuery)
                    .setFetchSize(selectLimit.intValue())
                    .setReadOnly(true)
                    .setCacheable(false)
                    .scroll(ScrollMode.FORWARD_ONLY);
        } else {
            if (isNativeQuery(jobExecutionResult)) {
                scrollableResults = statelessSession.createNativeQuery(selectQuery)
                        .setFetchSize(selectLimit.intValue())
                        .setReadOnly(true)
                        .setCacheable(false)
                        //.setLockMode("a", LockMode.NONE)
                        .setFlushMode(FlushMode.COMMIT)
                        .scroll(ScrollMode.FORWARD_ONLY);
            } else {
                scrollableResults = statelessSession.createQuery(selectQuery)
                        .setFetchSize(selectLimit.intValue())
                        .setReadOnly(true)
                        .setCacheable(false)
                        .scroll(ScrollMode.FORWARD_ONLY);
            }
        }
        return scrollableResults;
    }
}