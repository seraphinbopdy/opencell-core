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

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.base.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Job definition to do the mass update with offline pagination functionality.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class MassUpdaterOfflineJobBean extends MassUpdaterJobBean {

    private static final Logger log = LoggerFactory.getLogger(MassUpdaterOfflineJobBean.class);

    private List<Long> result;
    private StatelessSession statelessSession;


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        //namedQuery parameter provided only by other jobs or services through code and not through the GUI.
        String namedQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_NAMED_QUERY);
        String updateQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_UPDATE_QUERY);
        String selectQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_QUERY);
        Long selectFetchSize = (Long) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_FETCH_SIZE);
        Long selectMaxResults = (Long) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_MAX_RESULTS);

        //Check the mandatory settings for mass update processing
        if (StringUtils.isEmpty(selectQuery) || (StringUtils.isBlank(updateQuery) && StringUtils.isBlank(namedQuery))) {
            log.error("params should not be null - selectQuery: {} and (updateQuery: {} or namedQuery: {})", selectQuery, updateQuery, namedQuery);
            return;
        }
        Long size = ((long) Runtime.getRuntime().availableProcessors()) * Long.valueOf(NativePersistenceService.SHORT_MAX_VALUE);
        if (selectFetchSize == null) {
            selectFetchSize = size;
        }

        createView(isNativeQuery(jobExecutionResult) ? selectQuery : PersistenceService.getNativeQueryFromJPA(emWrapper.getEntityManager().createQuery(selectQuery), null));
        statelessSession = emWrapper.getEntityManager().unwrap(Session.class).getSessionFactory().openStatelessSession();
        int i = 1;
        boolean hasMore = true;
        try {
            do {
                int pageSize = selectFetchSize.intValue();
                if (selectMaxResults != null && ((selectFetchSize.intValue() * i) > selectMaxResults.intValue())) {
                    pageSize = selectMaxResults.intValue() - (selectFetchSize.intValue() * (i - 1));
                }
                result = getResult(jobExecutionResult, i, pageSize);
                if (!result.isEmpty()) {
                    super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, null, this::processUpdateQueries, null, null, null, null);
                }
                if (result.isEmpty() || result.size() < selectFetchSize.intValue() || (selectMaxResults != null && (selectFetchSize.intValue() * i >= selectMaxResults.intValue()))) {
                    hasMore = false;
                }
                i++;
            } while (hasMore);
            dropView();
        } finally {
            finalize(jobExecutionResult);
        }
    }

    /**
     * Initializes intervals for updating based on the specified chunk size.
     *
     * @return An iterator of Long[] representing intervals.
     */
    protected Optional<Iterator<List<Long>>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        Long updateChunk = (Long) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_UPDATE_CHUNK_SIZE);
        updateChunk = (updateChunk != null) ? Math.min(updateChunk, NativePersistenceService.SHORT_MAX_VALUE) : Long.valueOf(NativePersistenceService.SHORT_MAX_VALUE);

        return Optional.of(new SynchronizedIterator<>(Lists.partition(result, Math.min(NativePersistenceService.SHORT_MAX_VALUE, updateChunk.intValue())), -1));
    }

    /**
     * Process an update operation for a specific interval.
     *
     * @param interval           The interval to update.
     * @param jobExecutionResult The job execution result.
     */
    protected void processUpdateQueries(List<Long> interval, JobExecutionResultImpl jobExecutionResult) {
        if (!interval.isEmpty()) {
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
    }

    /**
     * Finalize function
     *
     * @param jobExecutionResult Job execution result
     */
    protected void finalize(JobExecutionResultImpl jobExecutionResult) {
        if (statelessSession != null) {
            statelessSession.close();
        }
    }

    /**
     * Get the results
     *
     * @param jobExecutionResult the job execution result
     * @param page               the page
     * @param pageSize           the page size
     * @return the scrollable results
     */
    public List<Long> getResult(JobExecutionResultImpl jobExecutionResult, int page, int pageSize) {
        NativeQuery query = statelessSession.createNativeQuery((isPessimisticUpdateLock(jobExecutionResult) ? "select distinct id from " : "select id from ") + VIEW_NAME + " order by id");
        return query.setFirstResult((page - 1) * pageSize)
                .addScalar("id", StandardBasicTypes.LONG)
                .setMaxResults(pageSize)
                .getResultList();
    }
}