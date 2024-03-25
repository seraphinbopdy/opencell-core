package org.meveo.admin.job;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.utils.CustomFieldTemplateUtils;
import org.meveo.jpa.EntityManagerProvider;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.model.rating.EDR;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.job.Job;
import org.meveo.service.job.TablesPartitioningService;

/**
 * Job definition to find and move operations (EDR, WalletOperation, RatedTransacrion) from 'default partition' to the current partition (only data that match target partition)
 * 
 * 
 */
@Stateless
public class MigrateOperationsPartionJob extends Job {

	private static final String JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB = "JobInstance_MigrateOperationsPartionJob";

	public static final String CF_MASS_UPDATE_CHUNK = "CF_MASS_UPDATE_CHUNK";
	
	private static final String OPERATION_TO_UPDATE = "CF_MigrateOperationsPartionJob_OPERATION_TO_UPDATE";

	@Inject
	private UpdateStepExecutor updateStepExecutor;
	
	@Inject
	private TablesPartitioningService tablesPartitioningService; 
	
	Map<String, String> MIGRATE_OPERATIONS_QUERIES = Map.ofEntries(
            Map.entry(RatedTransaction.class.getSimpleName(), "UPDATE billing_rated_transaction_other_default SET usage_date = usage_date WHERE usage_date >= :startDate AND usage_date < :endDate"),
            Map.entry(WalletOperation.class.getSimpleName(), "UPDATE billing_wallet_operation_other_default SET operation_date = operation_date WHERE operation_date >= :startDate AND operation_date < :endDate"),
            Map.entry(EDR.class.getSimpleName(), "UPDATE rating_edr_other_default SET event_date = event_date WHERE event_date >= :startDate AND event_date < :endDate")
        );

	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)
	protected JobExecutionResultImpl execute(JobExecutionResultImpl updateResult, JobInstance jobInstance) throws BusinessException {

		initUpdateStepParams(updateResult, jobInstance);
		updateStepExecutor.execute(updateResult, jobInstance);
		return updateResult;
	}

	private void initUpdateStepParams(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
		String operationToUpdate = (String) getParamOrCFValue(jobInstance, OPERATION_TO_UPDATE);
		String query = MIGRATE_OPERATIONS_QUERIES.get(operationToUpdate);
		Date startDate = tablesPartitioningService.getLastPartitionDate(tablesPartitioningService.WO_PARTITION_SOURCE);
		Date endDate = DateUtils.addMonthsToDate(startDate, 1);
		jobExecutionResult.addJobParam(UpdateStepExecutor.PARAM_QUERY_PARAMS,(Map.ofEntries(
				Map.entry("startDate", startDate), Map.entry("endDate", endDate))));
		jobExecutionResult.addJobParam(UpdateStepExecutor.PARAM_UPDATE_QUERY,
				(query + (EntityManagerProvider.isDBOracle() ? "Oracle" : "")));
		jobExecutionResult.addJobParam(UpdateStepExecutor.PARAM_READ_INTERVAL_QUERY,
				("select min(id), max(id) from RatedTransaction where status ='OPEN' and discountedRatedTransaction is null"));
		jobExecutionResult.addJobParam(UpdateStepExecutor.PARAM_NATIVE_QUERY, (false));
	}

	@Override
	public JobCategoryEnum getJobCategory() {
		return MeveoJobCategoryEnum.UTILS;
	}

	@Override
	public Map<String, CustomFieldTemplate> getCustomFields() {
		Map<String, CustomFieldTemplate> result = new HashMap<>();

		result.put(CF_NB_RUNS,
				CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"), CustomFieldTypeEnum.LONG,
						"tab:Configuration:0;fieldGroup:Configuration:0;field:0", "-1", JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB));
		result.put(Job.CF_WAITING_MILLIS, CustomFieldTemplateUtils.buildCF(Job.CF_WAITING_MILLIS, resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
				"tab:Configuration:0;fieldGroup:Configuration:0;field:1", "0", JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB));
		result.put(CF_MASS_UPDATE_CHUNK, CustomFieldTemplateUtils.buildCF(CF_MASS_UPDATE_CHUNK, resourceMessages.getString("jobExecution.massUpdate.Size"), CustomFieldTypeEnum.LONG,
				"tab:Configuration:0;fieldGroup:Configuration:0;field:3", "100000", JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB));
        CustomFieldTemplate cft = CustomFieldTemplateUtils.buildCF(OPERATION_TO_UPDATE, resourceMessages.getString("jobExecution.operation.to.update"), CustomFieldTypeEnum.LIST,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:4", "0", false, JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB);
        cft.setListValues(Map.of(RatedTransaction.class.getSimpleName(), RatedTransaction.class.getSimpleName(), WalletOperation.class.getSimpleName(), WalletOperation.class.getSimpleName(), EDR.class.getSimpleName(), EDR.class.getSimpleName()));
        result.put(OPERATION_TO_UPDATE, cft);

		return result;
	}
}