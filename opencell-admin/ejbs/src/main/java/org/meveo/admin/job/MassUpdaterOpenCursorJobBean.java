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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;
import org.jgroups.JChannel;
import org.meveo.admin.async.SynchronizedMultiItemIterator;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.base.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctc.wstx.shaded.msv_core.datatype.xsd.LongType;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Job definition to do the mass update with open cursor pagination functionality.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Stateless
public class MassUpdaterOpenCursorJobBean extends MassUpdaterJobBean {

    private static final Logger log = LoggerFactory.getLogger(MassUpdaterOpenCursorJobBean.class);

    @Resource(lookup = "java:jboss/jgroups/channel/default")
    private JChannel channel;

    private StatelessSession statelessSession;
    private ScrollableResults scrollableResults;
    private Set<Long> uniqueIds = new HashSet<>();


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, null, this::processUpdateQueries, null, null, this::finalize, null);
    }

    /**
     * Initializes intervals for updating based on the specified chunk size.
     *
     * @return An iterator of Long[] representing intervals.
     */
    protected Optional<Iterator<List<Long>>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        //namedQuery parameter provided only by other jobs or services through code and not through the GUI.
        String namedQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_NAMED_QUERY);
        String updateQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_UPDATE_QUERY);
        String selectQuery = (String) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_QUERY);
        Long selectFetchSize = (Long) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_FETCH_SIZE);
        Long selectMaxResults = (Long) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_SELECT_MAX_RESULTS);
        Long updateChunk = (Long) jobExecutionResult.getJobParam(MassUpdaterJob.PARAM_UPDATE_CHUNK_SIZE);

        //Check the mandatory settings for mass update processing
        if (StringUtils.isEmpty(selectQuery) || (StringUtils.isBlank(updateQuery) && StringUtils.isBlank(namedQuery))) {
            log.error("params should not be null - selectQuery: {} and (updateQuery: {} or namedQuery: {})", selectQuery, updateQuery, namedQuery);
            return Optional.empty();
        }
        Long size = ((long) Runtime.getRuntime().availableProcessors()) * Long.valueOf(NativePersistenceService.SHORT_MAX_VALUE);
        if (selectFetchSize == null) {
            selectFetchSize = size;
        }
        updateChunk = (updateChunk != null) ? Math.min(updateChunk, NativePersistenceService.SHORT_MAX_VALUE) : Long.valueOf(NativePersistenceService.SHORT_MAX_VALUE);

        if (isPessimisticUpdateLock(jobExecutionResult) && !isUsingView(jobExecutionResult)) {
            JobInstance jobInstance = jobExecutionResult.getJobInstance();
            int nrOfNodes = jobInstance.getRunOnNodes() != null ? jobInstance.getRunOnNodes().split(",").length : channel.getView().getMembers().size();
            uniqueIds = Collections.newSetFromMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return this.size() > (2 * (nrOfNodes < 1 ? 1 : nrOfNodes) * size.intValue());
                }
            });
        }


        scrollableResults = getScrollableResult(jobExecutionResult, selectQuery, selectFetchSize, selectMaxResults);
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
    protected void processUpdateQueries(List<Long> interval, JobExecutionResultImpl jobExecutionResult) {
        if (isPessimisticUpdateLock(jobExecutionResult) && !isUsingView(jobExecutionResult)) {
            interval = interval.stream().distinct().collect(Collectors.toList());
            interval.removeAll(uniqueIds);
        }
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
            if (isPessimisticUpdateLock(jobExecutionResult) && !isUsingView(jobExecutionResult)) {
                uniqueIds.addAll(interval);
            }
        }
    }

    /**
     * Finalize function
     *
     * @param jobExecutionResult Job execution result
     */
    protected void finalize(JobExecutionResultImpl jobExecutionResult) {
        if (scrollableResults != null) {
            scrollableResults.close();
        }
        if (statelessSession != null) {
            statelessSession.close();
        }
        dropView();
    }

    /**
     * Get the scrollable results
     *
     * @param jobExecutionResult the job execution result
     * @param selectQuery        the select query
     * @param selectFetchSize    the select fetch size
     * @param selectMaxResults   the select max results
     * @return the scrollable results
     */
    public ScrollableResults getScrollableResult(JobExecutionResultImpl jobExecutionResult, String selectQuery, Long selectFetchSize, Long selectMaxResults) {
        statelessSession = emWrapper.getEntityManager().unwrap(Session.class).getSessionFactory().openStatelessSession();

        if (isUsingView(jobExecutionResult)) {
            createView(isNativeQuery(jobExecutionResult) ? selectQuery : PersistenceService.getNativeQueryFromJPA(emWrapper.getEntityManager().createQuery(selectQuery), null));
            NativeQuery query = statelessSession.createNativeQuery((isPessimisticUpdateLock(jobExecutionResult) ? "select distinct * from " : "select * from ") + VIEW_NAME);
            if (selectMaxResults != null) {
                query.setMaxResults(selectMaxResults.intValue());
            }
            scrollableResults = query.setFetchSize(selectFetchSize.intValue())
                    .setReadOnly(true)
                    .setCacheable(false)
                    .addScalar("id", StandardBasicTypes.LONG)
                    .scroll(ScrollMode.FORWARD_ONLY);
        } else {
            if (isNativeQuery(jobExecutionResult)) {
                NativeQuery query = statelessSession.createNativeQuery(selectQuery);
                if (selectMaxResults != null) {
                    query.setMaxResults(selectMaxResults.intValue());
                }
                scrollableResults = query.setFetchSize(selectFetchSize.intValue())
                        .setReadOnly(true)
                        .setCacheable(false)
                        .addScalar("id", StandardBasicTypes.LONG)
                        .scroll(ScrollMode.FORWARD_ONLY);
            } else {
                Query query = statelessSession.createQuery(selectQuery);
                if (selectMaxResults != null) {
                    query.setMaxResults(selectMaxResults.intValue());
                }
                scrollableResults = query.setFetchSize(selectFetchSize.intValue())
                        .setReadOnly(true)
                        .setCacheable(false)
                        .scroll(ScrollMode.FORWARD_ONLY);
            }
        }
        return scrollableResults;
    }
}