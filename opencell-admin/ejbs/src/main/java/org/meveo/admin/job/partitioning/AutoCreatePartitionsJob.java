package org.meveo.admin.job.partitioning;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.UpdateStepExecutor;
import org.meveo.admin.job.utils.CustomFieldTemplateUtils;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.model.rating.EDR;
import org.meveo.model.securityDeposit.FinanceSettings;
import org.meveo.service.job.Job;
import org.meveo.service.job.TablesPartitioningService;
import org.meveo.service.securityDeposit.impl.FinanceSettingsService;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
/**
 * Job to create the next needed partition, and  find and move operations (EDR, WalletOperation,
 * RatedTransacrion) from 'default partition' to the new partition (only
 * data that match target partition)
 */
@Stateless
public class AutoCreatePartitionsJob extends Job {
    private static final String JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB = "JobInstance_AutoCreatePartitionsJob";
    private static final String CF_MASS_UPDATE_CHUNK = "CF_MASS_UPDATE_CHUNK";
    private static final String OPERATION_TO_UPDATE = "CF_AutoCreatePartitionsJob_OPERATION_TO_UPDATE";
    
    private static final String UPDATE_QUERY = "UPDATE %s SET partition_id = %s WHERE status<>'OPEN' and %s >= '%s' AND %s < '%s'";
    private static final String LIMIT_QUERY = "SELECT min(id), max(id) FROM %s WHERE status<>'OPEN' and %s >= '%s' AND %s < '%s'";
    private static final String ALL_ENTITIES = "ALL";
    private static final String CREATE_PARTITION_QUERY_PATTERN = "select count(*) from create_new_%s_partition('%s', '%s', '%s')";
    @Inject
    private UpdateStepExecutor updateStepExecutor;
    @Inject
    private TablesPartitioningService tablesPartitioningService;
    @Inject
    FinanceSettingsService financeSettingsService;
    private static final Map<String, OperationDetails> OPERATION_DETAILS_MAP = new HashMap<>();
    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    protected JobExecutionResultImpl execute(JobExecutionResultImpl updateResult, JobInstance jobInstance) throws BusinessException {
        OPERATION_DETAILS_MAP.put(RatedTransaction.class.getSimpleName(),
                new OperationDetails(RatedTransaction.class.getSimpleName(), "billing_rated_transaction_other_default", "usage_date", tablesPartitioningService.RT_PARTITION_SOURCE,"rt"));
        OPERATION_DETAILS_MAP.put(WalletOperation.class.getSimpleName(),
                new OperationDetails(WalletOperation.class.getSimpleName(), "billing_wallet_operation_other_default", "operation_date", tablesPartitioningService.WO_PARTITION_SOURCE,"wo"));
        OPERATION_DETAILS_MAP.put(EDR.class.getSimpleName(),
                new OperationDetails(EDR.class.getSimpleName(), "rating_edr_other_default", "event_date", tablesPartitioningService.EDR_PARTITION_SOURCE,"edr"));
        String jobConfig = (String) getParamOrCFValue(jobInstance, OPERATION_TO_UPDATE, ALL_ENTITIES);
        List<String> operationsToUpdate = ALL_ENTITIES.equals(jobConfig) ? new ArrayList<>(OPERATION_DETAILS_MAP.keySet()) : Arrays.asList(jobConfig);
        updateResult.setNbItemsToProcess(operationsToUpdate.size());
        for (String operationToUpdate : operationsToUpdate) {
            createNewPartition(updateResult, jobInstance, operationToUpdate);
        }
        return updateResult;
    }
    private void createNewPartition(JobExecutionResultImpl updateResult, JobInstance jobInstance, String operationToUpdate) {
    	OperationDetails details = OPERATION_DETAILS_MAP.get(operationToUpdate);
        migrateOperationsToPartition(updateResult, jobInstance, operationToUpdate, 1);
        tablesPartitioningService.createNewPartition(CREATE_PARTITION_QUERY_PATTERN, details.getPartitionSource(), details.getAlias(), updateResult);
        migrateOperationsToPartition(updateResult, jobInstance, operationToUpdate, 0);
    }
    private void migrateOperationsToPartition(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance, String operationToUpdate, int partitionId) {
        OperationDetails details = OPERATION_DETAILS_MAP.get(operationToUpdate);
        Date[] referenceDates = tablesPartitioningService.getLastPartitionsDate(details.getAlias());
        String startDate = tablesPartitioningService.getDateAsString(referenceDates[0]);
        String endDate = tablesPartitioningService.getDateAsString(referenceDates[1]);
        String updateQuery = buildUpdateQuery(details, startDate, endDate, partitionId);
        String limitQuery = buildLimitQuery(details, startDate, endDate);
        jobExecutionResult.addJobParam(UpdateStepExecutor.PARAM_UPDATE_QUERY, updateQuery);
        jobExecutionResult.addJobParam(UpdateStepExecutor.PARAM_READ_INTERVAL_QUERY, limitQuery);
        jobExecutionResult.addJobParam(UpdateStepExecutor.PARAM_NATIVE_QUERY, (true));
        updateStepExecutor.execute(jobExecutionResult, jobInstance);
    }
    private int getPartitionRange(String operationToUpdate, FinanceSettings settings) {
        if (operationToUpdate.equals(WalletOperation.class.getSimpleName())) {
            return settings.getWoPartitionPeriod();
        } else if (operationToUpdate.equals(RatedTransaction.class.getSimpleName())) {
            return settings.getRtPartitionPeriod();
        } else {
            return settings.getEdrPartitionPeriod();
        }
    }
    static String buildUpdateQuery(OperationDetails details, String startDate, String endDate, int partitionId) {
        return String.format(UPDATE_QUERY, details.getTableName(), partitionId, details.getColumnName(), startDate, details.getColumnName(), endDate);
    }
    static String buildLimitQuery(OperationDetails details, String startDate, String endDate) {
        return String.format(LIMIT_QUERY, details.getTableName(), details.getColumnName(), startDate, details.getColumnName(), endDate);
    }
    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.UTILS;
    }
    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<>();
        result.put(CF_NB_RUNS,
                CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"),
                        CustomFieldTypeEnum.LONG, "tab:Configuration:0;fieldGroup:Configuration:0;field:0", "-1",
                        JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB));
        result.put(Job.CF_WAITING_MILLIS,
                CustomFieldTemplateUtils.buildCF(Job.CF_WAITING_MILLIS,
                        resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:1", "0",
                        JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB));
        result.put(CF_MASS_UPDATE_CHUNK,
                CustomFieldTemplateUtils.buildCF(CF_MASS_UPDATE_CHUNK,
                        resourceMessages.getString("jobExecution.massUpdate.Size"), CustomFieldTypeEnum.LONG,
                        "tab:Configuration:0;fieldGroup:Configuration:0;field:3", "100000",
                        JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB));
        CustomFieldTemplate cft = CustomFieldTemplateUtils.buildCF(OPERATION_TO_UPDATE,
                resourceMessages.getString("jobExecution.operation.to.update"), CustomFieldTypeEnum.LIST,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:4", "0", false,
                JOB_INSTANCE_MIGRATE_OPERATIONS_PARTITION_JOB);
        cft.setListValues(Map.of(ALL_ENTITIES, ALL_ENTITIES,
                RatedTransaction.class.getSimpleName(), RatedTransaction.class.getSimpleName(),
                WalletOperation.class.getSimpleName(), WalletOperation.class.getSimpleName(),
                EDR.class.getSimpleName(), EDR.class.getSimpleName()));
        result.put(OPERATION_TO_UPDATE, cft);
        return result;
    }
	
}