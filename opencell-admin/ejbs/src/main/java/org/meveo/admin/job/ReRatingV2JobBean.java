package org.meveo.admin.job;

import static org.apache.commons.collections4.ListUtils.partition;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.meveo.admin.async.SynchronizedMultiItemIterator;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.ReratingService;
import org.meveo.service.job.Job;
import org.meveo.service.job.TablesPartitioningService;
import org.meveo.service.settings.impl.AdvancedSettingsService;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@Stateless
public class ReRatingV2JobBean extends IteratorBasedJobBean<List<Object[]>> {

	private static final long serialVersionUID = 8799763764569695857L;


	@Inject
	@MeveoJpa
	private EntityManagerWrapper emWrapper;
	
	@Inject
	TablesPartitioningService tablesPartitioningService;

	@Inject
	private AdvancedSettingsService advancedSettingsService;
	
	private EntityManager entityManager;

	private StatelessSession statelessSession;
	
	private ScrollableResults scrollableResults;

	private Long nrOfInitialWOs = null;
	
	private boolean useSamePricePlan;
	
    @Inject
    private ReratingService reratingService;

    private String lastEDRPartition;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, this::initJobOnWorkerNode, this::applyReRating, null, null, this::closeResultset, null);
	}

    /**
     * Initialize job settings and retrieve data to process
     *
     * @param jobExecutionResult Job execution result
     * @return An iterator over a list of Wallet operation Ids to convert to Rated transactions
     */
	private Optional<Iterator<List<Object[]>>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {

		useSamePricePlan = "justPrice".equalsIgnoreCase(jobExecutionResult.getJobInstance().getParametres());
		
		JobInstance jobInstance = jobExecutionResult.getJobInstance();

		Long nbThreads = (Long) this.getParamOrCFValue(jobInstance, Job.CF_NB_RUNS, -1L);
		if (nbThreads == -1) {
			nbThreads = (long) Runtime.getRuntime().availableProcessors();
		}

		lastEDRPartition = getOperationDate(jobInstance);
		
		final long configuredNrPerTx = (Long) this.getParamOrCFValue(jobInstance, ReRatingV2Job.CF_NR_ITEMS_PER_TX, 10000L);
		
		entityManager = emWrapper.getEntityManager();
		statelessSession = entityManager.unwrap(Session.class).getSessionFactory().openStatelessSession();

		Map<String, Object> advancedSettingsValues = advancedSettingsService.getAdvancedSettingsMapByGroup("rating", Object.class);
		Boolean allowBilledItemsRerating = (Boolean) advancedSettingsValues.get("rating.allowBilledItemsRerating");
		getProcessingSummary(allowBilledItemsRerating);
		if (nrOfInitialWOs.intValue() == 0) {
			dropView();
			return Optional.empty();
		}
		jobExecutionResult.addReport(" Start rerate step for " + nrOfInitialWOs + " WOs");
		
		final long nrPerTx = (nrOfInitialWOs / nbThreads) < configuredNrPerTx ? nrOfInitialWOs / nbThreads : configuredNrPerTx;
		int fetchSize = ((Long) nrPerTx).intValue() * nbThreads.intValue();
		String sql = "SELECT CAST(unnest(string_to_array(wo_id, ',')) AS bigint) as id FROM " + RatingCancellationJobBean.MAIN_VIEW_NAME;
		if (!Boolean.TRUE.equals(allowBilledItemsRerating)) {
			sql += " WHERE billed_il is null";
		}
		sql += " order by ba_id";
		NativeQuery nativeQuery = statelessSession.createNativeQuery(sql);
		scrollableResults = nativeQuery.setReadOnly(true).setCacheable(false).setFetchSize(fetchSize).scroll(ScrollMode.FORWARD_ONLY);

        return Optional.of(new SynchronizedMultiItemIterator<Object[]>(scrollableResults, nrOfInitialWOs.intValue(), true, null) {

					long count = 0L;

					@Override
					public void initializeDecisionMaking(Object[] item) {
						count = 0L;
					}

					@Override
					public boolean isIncludeItem(Object[] item) {
						if (count ++ > nrPerTx) {
							return false;
						}
						return true;
					}
				});
	}

    /**
     * Initialize job settings on Worker node
     * 
     * @param jobExecutionResult Job execution result
     */
    private void initJobOnWorkerNode(JobExecutionResultImpl jobExecutionResult) {

        useSamePricePlan = "justPrice".equalsIgnoreCase(jobExecutionResult.getJobInstance().getParametres());
    }

	private void applyReRating(List<Object[]> reratingTree, JobExecutionResultImpl jobExecutionResult) {
		if (reratingTree != null) {
			rerateByGroup(reratingTree.stream().map(x->((Number)x[0]).longValue()).collect(Collectors.toList()), jobExecutionResult);
		}
	}

	private void rerateByGroup(List<Long> reratingTree, JobExecutionResultImpl jobExecutionResult) {
    	final int maxValue = ParamBean.getInstance().getPropertyAsInteger("database.number.of.inlist.limit", reratingService.SHORT_MAX_VALUE);
    	List<List<Long>> subList = partition(reratingTree, maxValue);
    	
		String edrDateCondition = lastEDRPartition != null ? " AND edr.eventDate>'" + lastEDRPartition+"'" : "";
		subList.forEach(ids -> reratingService.applyMassRerate(ids, useSamePricePlan, jobExecutionResult, edrDateCondition));
	}

	private String getOperationDate(JobInstance jobInstance) {
		String operationDateConfig = (String) this.getParamOrCFValue(jobInstance,
				ReRatingV2Job.CF_OPERATIONS_STARTING_DATE, ReRatingV2Job.NO_DATE_LIMITE);
		boolean useLimitDate = !operationDateConfig.equals(ReRatingV2Job.NO_DATE_LIMITE)
				&& CollectionUtils.isNotEmpty(tablesPartitioningService.listPartitionsStartDate("edr"));
		return useLimitDate ? 
				(ReRatingV2Job.USE_LAST_PARTITION.equals(operationDateConfig) ? tablesPartitioningService.getLastPartitionStartingDateAsString("edr") : operationDateConfig)
				: null;
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
		dropView();
	}

	private void dropView() {
		Session hibernateSession = entityManager.unwrap(Session.class);

		hibernateSession.doWork(new org.hibernate.jdbc.Work() {
			@Override
			public void execute(Connection connection) throws SQLException {

				try (Statement statement = connection.createStatement()) {
					log.info("Dropping materialized view {}", RatingCancellationJobBean.MAIN_VIEW_NAME);
					statement.execute("drop materialized view if exists " + RatingCancellationJobBean.MAIN_VIEW_NAME +" cascade");
				} catch (Exception e) {
					log.error("Failed to drop/create the materialized view " + RatingCancellationJobBean.MAIN_VIEW_NAME, e.getMessage());
					throw new BusinessException(e);
				}
			}
		});
	}

	private void getProcessingSummary(Boolean allowBilledItemsRerating) {
		String sql = "select sum(count_wo), count(rr.id) from " + RatingCancellationJobBean.MAIN_VIEW_NAME + " rr LEFT JOIN " + RatingCancellationJobBean.BILLED_VIEW_NAME + " bil ON rr.id = bil.id ";
		if (!Boolean.TRUE.equals(allowBilledItemsRerating)) {
			sql += " WHERE bil.id IS NULL";
		}
		Object[] count = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();
		nrOfInitialWOs = count[0] != null ? ((Number) count[0]).longValue() : 0;
	}

	@Override
    protected boolean isProcessItemInNewTx() {
        return false;
    }

}