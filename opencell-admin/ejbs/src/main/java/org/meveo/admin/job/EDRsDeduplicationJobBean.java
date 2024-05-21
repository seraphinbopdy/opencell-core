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

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.meveo.admin.async.SynchronizedMultiItemIterator;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.rating.EDR;
import org.meveo.model.rating.EDRStatusEnum;
import org.meveo.service.billing.impl.EdrService;
import org.meveo.service.billing.impl.ReratingService;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * A job implementation to cancel duplicated EDRs and keep only the newest version.
 *
 * @author Abdellatif BARI
 * @since 16.1.0
 */
@Stateless
public class EDRsDeduplicationJobBean extends IteratorBasedScopedJobBean<List<Long>> {

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
    protected Optional<Iterator<List<Long>>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        return getIterator(jobExecutionResult);
    }

    /**
     * Apply deduplication.
     *
     * @param edrIds             the edr ids to be removed.
     * @param jobExecutionResult The job execution result.
     */
    protected void applyDeduplication(List<Long> edrIds, JobExecutionResultImpl jobExecutionResult) {

        for (Long edrId : edrIds) {
            EDR edrToCancel = edrService.findById(edrId);
            boolean derivedWOwasCanceled = reratingService.validateAndCancelDerivedWosEdrsAndRts(edrToCancel);
            edrToCancel.setStatus(EDRStatusEnum.CANCELLED);
            if (derivedWOwasCanceled) {
                edrToCancel.setRejectReason("Received new version EDR[id=" + edrToCancel.getId() + "]");
                edrToCancel.setEventVersion(edrToCancel.getEventVersion() + 1);
            } else {
                edrToCancel.setRejectReason("EDR[id=" + edrToCancel.getId() + ", eventKey=" + edrToCancel.getEventKey() + "] has already been invoiced");
            }
            edrService.update(edrToCancel);
        }
    }

    /**
     * Get the scrollable results
     *
     * @param jobInstance the job job instance
     * @param maxResults  the max results
     * @return the scrollable results
     */
    public ScrollableResults getScrollableResult(JobInstance jobInstance, int maxResults) {
        Long selectFetchSize = (Long) getParamOrCFValue(jobInstance, EDRsDeduplicationJob.SELECT_FETCH_SIZE);
        Long nbThreads = (Long) this.getParamOrCFValue(jobInstance, EDRsDeduplicationJob.CF_NB_RUNS, -1L);
        if (nbThreads == -1) {
            nbThreads = (long) Runtime.getRuntime().availableProcessors();
        }
        if (selectFetchSize == null) {
            selectFetchSize = EDRsDeduplicationJob.DEFAULT_SELECT_FETCH_SIZE * nbThreads.intValue();
        }
        statelessSession = emWrapper.getEntityManager().unwrap(Session.class).getSessionFactory().openStatelessSession();
        Query query = statelessSession.createNamedQuery("EDR.getDuplicatedEDRs");
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        scrollableResults = query.setFetchSize(selectFetchSize.intValue())
                .setReadOnly(true)
                .setCacheable(false)
                .scroll(ScrollMode.FORWARD_ONLY);
        return scrollableResults;
    }

    private Optional<Iterator<List<Long>>> getSynchronizedIterator(JobExecutionResultImpl jobExecutionResult, int jobItemsLimit) {
        JobInstance jobInstance = jobExecutionResult.getJobInstance();
        Long updateChunkSize = (Long) getParamOrCFValue(jobInstance, EDRsDeduplicationJob.UPDATE_CHUNK_SIZE, 10000L);
        scrollableResults = getScrollableResult(jobInstance, jobItemsLimit);
        return Optional.of(new SynchronizedMultiItemIterator<>(scrollableResults, -1) {
            long totalItemCount = 0L;

            @Override
            public void initializeDecisionMaking(Long item) {
                totalItemCount = 1;
                jobExecutionResult.setNbItemsToProcess(jobExecutionResult.getNbItemsToProcess() + 1);
            }

            @Override
            public boolean isIncludeItem(Long item) {
                if (totalItemCount > updateChunkSize) {
                    return false;
                }
                totalItemCount++;
                jobExecutionResult.setNbItemsToProcess(jobExecutionResult.getNbItemsToProcess() + 1);
                return true;
            }
        });
    }

    @Override
    Optional<Iterator<List<Long>>> getSynchronizedIteratorWithLimit(JobExecutionResultImpl jobExecutionResult, int jobItemsLimit) {
        return getSynchronizedIterator(jobExecutionResult, jobItemsLimit);
    }

    @Override
    Optional<Iterator<List<Long>>> getSynchronizedIterator(JobExecutionResultImpl jobExecutionResult) {
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
    }
}
