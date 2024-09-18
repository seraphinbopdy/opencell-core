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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.meveo.admin.async.SynchronizedMultiItemIterator;
import org.meveo.admin.exception.BusinessException;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.base.NativePersistenceService;
import org.meveo.service.billing.impl.EdrService;
import org.meveo.service.billing.impl.ReratingService;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * A job implementation to cancel duplicated EDRs and keep only the newest version.
 *
 * @author Abdellatif BARI
 * @since 16.1.0
 */
@Stateless
public class EDRsDeduplicationJobBean extends IteratorBasedScopedJobBean<List<Object[]>> {

    private static final long serialVersionUID = 1L;

    @Inject
    @MeveoJpa
    protected EntityManagerWrapper emWrapper;
    private StatelessSession statelessSession;
    private ScrollableResults scrollableResults;

    @Inject
    private EdrService edrService;

    @Inject
    private ReratingService reratingService;

    private EntityManager entityManager;

    private Long toProcess = 0L;


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, null, this::applyDeduplication, null, null, this::finalize, null);
    }

    /**
     * Initialize job settings and retrieve data to process
     *
     * @param jobExecutionResult Job execution result
     * @return An iterator over a list of EDR
     */
    protected Optional<Iterator<List<Object[]>>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        return getIterator(jobExecutionResult);
    }

    /**
     * Apply deduplication.
     *
     * @param items              the edr ids to be removed.
     * @param jobExecutionResult The job execution result.
     */
    protected void applyDeduplication(List<Object[]> items, JobExecutionResultImpl jobExecutionResult) {
        Set<Long> edrsEV = new HashSet<>();
        Set<Long> edrs = new HashSet<>();

        for (Object[] item : items) {
            List<Long> idList = Arrays.stream(((String) item[1]).split(",")).map(Long::valueOf).collect(Collectors.toList());
            idList.stream().max(Long::compareTo).ifPresent(idList::remove);

            for (Long edrId : idList) {
                boolean derivedWOwasCanceled = reratingService.validateAndCancelDerivedWosEdrsAndRts(edrService.findById(edrId));
                if (derivedWOwasCanceled) {
                    edrsEV.add(edrId);
                } else {
                    edrs.add(edrId);
                }
            }
            if (idList.size() > 1) {
                jobExecutionResult.addNbItemsCorrectlyProcessed(idList.size() - 1);
            }
        }

        if (!edrsEV.isEmpty()) {
            edrService.getEntityManager().createNamedQuery("EDR.cancelEDRsWithRejectReasonAndEventVersion").setParameter("updatedDate", new Date()).setParameter("ids", edrsEV).executeUpdate();
        }
        if (!edrs.isEmpty()) {
            edrService.getEntityManager().createNamedQuery("EDR.cancelEDRsWithRejectReason").setParameter("updatedDate", new Date()).setParameter("ids", edrs).executeUpdate();
        }
    }

    /**
     * Get the scrollable results
     *
     * @param jobInstance the job instance
     * @param maxResults  the max results
     * @return the scrollable results
     */
    public ScrollableResults getScrollableResult(JobInstance jobInstance, int maxResults) {
        Long selectFetchSize = (Long) getParamOrCFValue(jobInstance, EDRsDeduplicationJob.SELECT_FETCH_SIZE);
        Long daysToProcess = (Long) getParamOrCFValue(jobInstance, EDRsDeduplicationJob.DAYS_TO_PROCESS);

        Long nbThreads = (Long) this.getParamOrCFValue(jobInstance, EDRsDeduplicationJob.CF_NB_RUNS, -1L);
        if (nbThreads == -1) {
            nbThreads = (long) Runtime.getRuntime().availableProcessors();
        }
        if (selectFetchSize == null) {
            selectFetchSize = EDRsDeduplicationJob.DEFAULT_SELECT_FETCH_SIZE * nbThreads.intValue();
        }

        statelessSession = entityManager.unwrap(Session.class).getSessionFactory().openStatelessSession();

        createView(daysToProcess);

        getProcessingSummary();

        org.hibernate.query.Query nativeQuery = statelessSession.createNativeQuery("select TO_PROCESS, IDS from DUPLICATED_EDRS_SUMMARY");
        scrollableResults = nativeQuery.setReadOnly(true).setCacheable(false).setFetchSize(selectFetchSize.intValue()).scroll(ScrollMode.FORWARD_ONLY);

        return scrollableResults;
    }

    private void createView(Long daysToProcess) {
        entityManager = emWrapper.getEntityManager();
        Session hibernateSession = entityManager.unwrap(Session.class);
        hibernateSession.doWork(new org.hibernate.jdbc.Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (Statement statement = connection.createStatement()) {
                    log.info("Creating materialized view DUPLICATED_EDRS_SUMMARY...");
                    String dateCondition = daysToProcess > 0 ? " AND CREATED>CURRENT_DATE-" + daysToProcess : "";
                    String viewQuery = "CREATE MATERIALIZED VIEW DUPLICATED_EDRS_SUMMARY AS SELECT COUNT(ID)-1 AS TO_PROCESS,"
                            + "	STRING_AGG(CAST(ID AS text),',') AS IDS,"
                            + "	EVENT_KEY AS EVENT_KEY"
                            + " FROM RATING_EDR "
                            + " WHERE STATUS <> 'CANCELLED'"
                            + dateCondition
                            + "	AND (EVENT_KEY IS NOT NULL)"
                            + " GROUP BY EVENT_KEY"
                            + " HAVING COUNT(ID) > 1";
                    log.info(viewQuery);
                    statement.execute(viewQuery);
                } catch (Exception e) {
                    log.error("Failed to create the materialized view DUPLICATED_EDRS_SUMMARY", e.getMessage());
                    throw new BusinessException(e);
                }
            }
        });
    }

    private Optional<Iterator<List<Object[]>>> getSynchronizedIterator(JobExecutionResultImpl jobExecutionResult, int jobItemsLimit) {
        JobInstance jobInstance = jobExecutionResult.getJobInstance();
        Long updateChunkSize = (Long) getParamOrCFValue(jobInstance, EDRsDeduplicationJob.UPDATE_CHUNK_SIZE, EDRsDeduplicationJob.DEFAULT_UPDATE_CHUNK_SIZE);
        Long finalUpdateChunkSize = Math.min(updateChunkSize, NativePersistenceService.SHORT_MAX_VALUE);
        entityManager = emWrapper.getEntityManager();
        scrollableResults = getScrollableResult(jobInstance, jobItemsLimit);
        return Optional.of(new SynchronizedMultiItemIterator<>(scrollableResults, toProcess.intValue(), true, null) {
            long totalItemCount = 0L;

            @Override
            public void initializeDecisionMaking(Object[] item) {
                totalItemCount = ((Number) item[0]).longValue();
            }

            @Override
            public boolean isIncludeItem(Object[] item) {
                totalItemCount += ((Number) item[0]).longValue();
                return totalItemCount <= finalUpdateChunkSize;
            }
        });
    }

    @Override
    Optional<Iterator<List<Object[]>>> getSynchronizedIteratorWithLimit(JobExecutionResultImpl jobExecutionResult, int jobItemsLimit) {
        return getSynchronizedIterator(jobExecutionResult, jobItemsLimit);
    }

    @Override
    Optional<Iterator<List<Object[]>>> getSynchronizedIterator(JobExecutionResultImpl jobExecutionResult) {
        return getSynchronizedIterator(jobExecutionResult, 0);
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

    private void dropView() {
        Session hibernateSession = entityManager.unwrap(Session.class);

        hibernateSession.doWork(new org.hibernate.jdbc.Work() {
            @Override
            public void execute(Connection connection) throws SQLException {

                try (Statement statement = connection.createStatement()) {
                    log.info("Dropping materialized view 'DUPLICATED_EDRS_SUMMARY'");
                    statement.execute("drop materialized view if exists DUPLICATED_EDRS_SUMMARY");
                } catch (Exception e) {
                    log.error("Failed to drop/create the materialized view DUPLICATED_EDRS_SUMMARY", e.getMessage());
                    throw new BusinessException(e);
                }
            }
        });
    }

    private void getProcessingSummary() {
        Number count = (Number) entityManager.createNativeQuery("select sum(TO_PROCESS) from DUPLICATED_EDRS_SUMMARY").getSingleResult();
        toProcess = count != null ? ((Number) count).longValue() : 0;
    }
}
