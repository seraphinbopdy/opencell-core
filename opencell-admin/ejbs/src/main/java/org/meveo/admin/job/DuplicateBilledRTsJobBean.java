package org.meveo.admin.job;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.LongType;
import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.RatedTransactionService;
import org.meveo.service.job.Job;
import org.meveo.service.job.TablesPartitioningService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Iterator;
import java.util.Optional;

/**
 * A job implementation to duplicate billed rated transactions
 *
 * @author Abdellatif BARI
 * @since 17.0.0
 */
@Stateless
public class DuplicateBilledRTsJobBean extends IteratorBasedJobBean<Long> {

    private static final long serialVersionUID = 1L;

    @Inject
    @MeveoJpa
    private EntityManagerWrapper emWrapper;

    @Inject
    private RatedTransactionService ratedTransactionService;

    @Inject
    TablesPartitioningService tablesPartitioningService;

    private StatelessSession statelessSession;
    private ScrollableResults scrollableResults;

    @Override
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, this::duplicateBilledRatedTransaction, null, this::terminate, null);
    }

    private Optional<Iterator<Long>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        JobInstance jobInstance = jobExecutionResult.getJobInstance();
        statelessSession = emWrapper.getEntityManager().unwrap(Session.class).getSessionFactory().openStatelessSession();

        Long nrOfRecords = getElementsToProcessSize();
        if (nrOfRecords == 0) {
            return Optional.empty();
        }

        Long batchSize = (Long) getParamOrCFValue(jobInstance, Job.CF_BATCH_SIZE, 10000L);
        Long nbThreads = (Long) this.getParamOrCFValue(jobInstance, Job.CF_NB_RUNS, -1L);
        if (nbThreads == -1) {
            nbThreads = (long) Runtime.getRuntime().availableProcessors();
        }
        int fetchSize = Long.valueOf(Math.min(batchSize * nbThreads, nrOfRecords)).intValue();

        NativeQuery query = statelessSession.createNativeQuery("(select distinct to_duplicate FROM main_rerate_tree mrt join rerate_billed_il i " +
                "on mrt.id=i.id CROSS JOIN unnest(string_to_array(rt_id, ',')) AS to_duplicate) " +
                "UNION (select distinct to_duplicate FROM main_rerate_tree mrt join rerate_billed_il i " +
                "on mrt.id=i.id CROSS JOIN unnest(string_to_array(drt_id, ',')) AS to_duplicate) " +
                "UNION (select distinct to_duplicate FROM triggered_rerate_tree trt join rerate_billed_il i " +
                "on trt.id=i.id CROSS JOIN unnest(string_to_array(trt_id, ',')) AS to_duplicate) " +
                "UNION (select distinct to_duplicate FROM triggered_rerate_tree trt join rerate_billed_il i " +
                "on trt.id=i.id CROSS JOIN unnest(string_to_array(tdrt_id, ',')) AS to_duplicate) " +
                "ORDER BY to_duplicate ");

        scrollableResults = query.setFetchSize(fetchSize)
                .setReadOnly(true)
                .setCacheable(false)
                .addScalar("to_duplicate", new LongType())
                .scroll(ScrollMode.FORWARD_ONLY);

        return Optional.of(new SynchronizedIterator<Long>(scrollableResults, nrOfRecords.intValue()));
    }

    private void duplicateBilledRatedTransaction(Long ratedTransactionId, JobExecutionResultImpl jobExecutionResult) {
        RatedTransaction ratedTransaction = ratedTransactionService.findById(ratedTransactionId);
        RatedTransaction duplicate = new RatedTransaction(ratedTransaction);
        duplicate.setUnitAmountTax(duplicate.getUnitAmountTax() != null ? duplicate.getUnitAmountTax().negate() : null);
        duplicate.setUnitAmountWithoutTax(duplicate.getUnitAmountWithoutTax() != null ? duplicate.getUnitAmountWithoutTax().negate() : null);
        duplicate.setUnitAmountWithTax(duplicate.getUnitAmountWithTax() != null ? duplicate.getUnitAmountWithTax().negate() : null);
        duplicate.setAmountTax(duplicate.getAmountTax() != null ? duplicate.getAmountTax().negate() : null);
        duplicate.setAmountWithoutTax(duplicate.getAmountWithoutTax() != null ? duplicate.getAmountWithoutTax().negate() : null);
        duplicate.setAmountWithTax(duplicate.getAmountWithTax() != null ? duplicate.getAmountWithTax().negate() : null);
        duplicate.setRawAmountWithTax(duplicate.getRawAmountWithTax() != null ? duplicate.getRawAmountWithTax().negate() : null);
        duplicate.setRawAmountWithoutTax(duplicate.getRawAmountWithoutTax() != null ? duplicate.getRawAmountWithoutTax().negate() : null);
        duplicate.setTransactionalAmountTax(duplicate.getTransactionalAmountTax() != null ? duplicate.getTransactionalAmountTax().negate() : null);
        duplicate.setTransactionalAmountWithoutTax(duplicate.getTransactionalAmountWithoutTax() != null ? duplicate.getTransactionalAmountWithoutTax().negate() : null);
        duplicate.setTransactionalAmountWithTax(duplicate.getTransactionalAmountWithTax() != null ? duplicate.getTransactionalAmountWithTax().negate() : null);
        duplicate.setOriginRatedTransaction(ratedTransaction);
        ratedTransactionService.create(duplicate);
    }

    private Long getElementsToProcessSize() {
        Number count = (Number) ratedTransactionService.getEntityManager().createNativeQuery("select sum(count_rt) from (" +
                        "(select count (distinct to_duplicate) as count_rt FROM main_rerate_tree mrt join rerate_billed_il i " +
                        "on mrt.id=i.id CROSS JOIN unnest(string_to_array(rt_id, ',')) AS to_duplicate GROUP BY to_duplicate) " +
                        "UNION (select count (distinct to_duplicate) as count_rt FROM main_rerate_tree mrt join rerate_billed_il i " +
                        "on mrt.id=i.id CROSS JOIN unnest(string_to_array(drt_id, ',')) AS to_duplicate GROUP BY to_duplicate) " +
                        "UNION (select count (distinct to_duplicate) as count_rt FROM triggered_rerate_tree trt join rerate_billed_il i " +
                        "on trt.id=i.id CROSS JOIN unnest(string_to_array(trt_id, ',')) AS to_duplicate GROUP BY to_duplicate) " +
                        "UNION (select count (distinct to_duplicate) as count_rt FROM triggered_rerate_tree trt join rerate_billed_il i " +
                        "on trt.id=i.id CROSS JOIN unnest(string_to_array(tdrt_id, ',')) AS to_duplicate GROUP BY to_duplicate) " +
                        ")")
                .getSingleResult();

        return count != null ? count.longValue() : 0;
    }

    private String getRTDateCondition(String alias, String lastRTPartition) {
        return StringUtils.isBlank(lastRTPartition) ? "" : " AND " + alias + ".usage_date>'" + lastRTPartition + "'";
    }

    /**
     * Terminate function
     */
    private void terminate(JobExecutionResultImpl jobExecutionResult) {
        JobInstance jobInstance = jobExecutionResult.getJobInstance();
        String alias = "rt";
        String operationDateConfig = (String) this.getParamOrCFValue(jobInstance,
                RatingCancellationJob.getOperationDateCFName(alias), RatingCancellationJob.NO_DATE_LIMITE);
        boolean useLimitDate = !operationDateConfig.equals(RatingCancellationJob.NO_DATE_LIMITE)
                && CollectionUtils.isNotEmpty(tablesPartitioningService.listPartitionsStartDate(alias));
        String lastRTPartition = useLimitDate ? (RatingCancellationJob.USE_LAST_PARTITION.equals(operationDateConfig) ?
                tablesPartitioningService.getLastPartitionStartingDateAsString(alias) : operationDateConfig) : null;

        String updateQuery = "UPDATE billing_rated_transaction new_RT " +
                "SET discounted_ratedtransaction_id = new_discounted_RT.id " +
                "FROM billing_rated_transaction " + alias +
                " JOIN billing_rated_transaction drt ON drt.id = " + alias + ".discounted_ratedtransaction_id " +
                "JOIN billing_rated_transaction new_discounted_RT ON drt.id = new_discounted_RT.origin_ratedtransaction_id " +
                "WHERE new_RT.origin_ratedtransaction_id = " + alias + ".id " +
                "AND new_RT.status = 'OPEN' " +
                "AND new_discounted_RT.status = 'OPEN' " + getRTDateCondition(alias, lastRTPartition);

        emWrapper.getEntityManager().createNativeQuery(updateQuery).executeUpdate();

        if (scrollableResults != null) {
            scrollableResults.close();
        }
        if (statelessSession != null) {
            statelessSession.close();
        }
    }
}
