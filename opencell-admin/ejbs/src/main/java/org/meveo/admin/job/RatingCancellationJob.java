package org.meveo.admin.job;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.utils.CustomFieldTemplateUtils;
import org.meveo.model.billing.BatchEntity;
import org.meveo.model.billing.ReratingTargetEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.job.Job;
import org.meveo.service.job.TablesPartitioningService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class RatingCancellationJob extends Job {

    private static final String JOB_INSTANCE_RATING_CANCELLATION_JOB = "JobInstance_RatingCancellationJob";

	public static final String CF_INVOICE_LINES_NR_RTS_PER_TX = "JobInstance_RatingCancellationJob_MaxRTsPerTransaction";

	public static final String CF_USE_EXISTING_VIEWS = "JobInstance_RatingCancellationJob_UseExistingViews";
	
	public static final String CF_OPERATIONS_STARTING_DATE="CF_OPERATIONS_STARTING_DATE";
	
	public static final String USE_LAST_PARTITION="LAST PARTITION DATE";
	
	public static final String NO_DATE_LIMITE="NO DATE LIMITE";
	
	@Inject
	private TablesPartitioningService tablesPartitioningService; 

    /**
     * To limit the scope of wallet operations to rerate.
     */
    public static final String CF_RERATING_TARGET = "ReRatingJobBean_reratingTarget";

    /**
     * Custom field contains a list of batch entities
     */
    public static final String CF_TARGET_BATCHES = "ReRatingJobBean_targetBatches";

    @Inject
    private RatingCancellationJobBean jobBean;

    @Override
    protected JobExecutionResultImpl execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        jobBean.execute(result, jobInstance);
        return result;
    }

    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.RATING;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<>();

        result.put(CF_NB_RUNS, CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:0", "-1", JOB_INSTANCE_RATING_CANCELLATION_JOB));
        result.put(Job.CF_WAITING_MILLIS, CustomFieldTemplateUtils.buildCF(Job.CF_WAITING_MILLIS, resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:1", "0", JOB_INSTANCE_RATING_CANCELLATION_JOB));
        result.put(CF_INVOICE_LINES_NR_RTS_PER_TX, CustomFieldTemplateUtils.buildCF(CF_INVOICE_LINES_NR_RTS_PER_TX, resourceMessages.getString("jobExecution.massUpdate.Size"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:2", "100000", true, JOB_INSTANCE_RATING_CANCELLATION_JOB));
        result.put(CF_USE_EXISTING_VIEWS, CustomFieldTemplateUtils.buildCF(CF_USE_EXISTING_VIEWS, resourceMessages.getString("jobExecution.useExistingViews"), CustomFieldTypeEnum.BOOLEAN,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:3", "true", JOB_INSTANCE_RATING_CANCELLATION_JOB));
		buildOperationDateCF(result,"wo",4);
		buildOperationDateCF(result,"rt",5);
		buildOperationDateCF(result,"edr",6);

        CustomFieldTemplate reratingTargetCFTemplate = CustomFieldTemplateUtils.buildCF(CF_RERATING_TARGET,
                resourceMessages.getString("jobExecution.reratingTarget"), CustomFieldTypeEnum.LIST,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:4",
                ReratingTargetEnum.ALL.name(), false, CustomFieldStorageTypeEnum.SINGLE, null,
                JOB_INSTANCE_RATING_CANCELLATION_JOB, null);
        Map<String, String> listValues = new HashMap();
        for (ReratingTargetEnum reratingTarget : ReratingTargetEnum.values()) {
            listValues.put(reratingTarget.name(), reratingTarget.getLabel());
        }
        reratingTargetCFTemplate.setListValues(listValues);
        result.put(CF_RERATING_TARGET, reratingTargetCFTemplate);


        CustomFieldTemplate targetBatchesCFTemplate = CustomFieldTemplateUtils.buildCF(CF_TARGET_BATCHES,
                resourceMessages.getString("jobExecution.targetBatches"), CustomFieldTypeEnum.ENTITY,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:5",
                null, false, CustomFieldStorageTypeEnum.LIST, null,
                JOB_INSTANCE_RATING_CANCELLATION_JOB, null);
        targetBatchesCFTemplate.setEntityClazz(BatchEntity.class.getName());
        result.put(CF_TARGET_BATCHES, targetBatchesCFTemplate);

        return result;
    }

	private void buildOperationDateCF(Map<String, CustomFieldTemplate> result, String alias, int i) {
		String cfName = getOperationDateCFName(alias);
		CustomFieldTemplate cft = CustomFieldTemplateUtils.buildCF(cfName,
                resourceMessages.getString("jobExecution.operation.starting.date",alias), CustomFieldTypeEnum.LIST,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:"+i, NO_DATE_LIMITE, false,
                JOB_INSTANCE_RATING_CANCELLATION_JOB);
		List<String> values=tablesPartitioningService.listPartitionsStartDate(alias);
		Map<String, String> options = new LinkedHashMap<>(Map.of(
                NO_DATE_LIMITE, NO_DATE_LIMITE,
                USE_LAST_PARTITION, USE_LAST_PARTITION));
		if(CollectionUtils.isNotEmpty(values)) {
			values.stream().forEach(item->options.put(item, item));
		}
		cft.setListValues(options);
		cft.setTags(CustomFieldTemplateService.UPDATE_EXISTING);
        result.put(cfName, cft);
	}

	public static String getOperationDateCFName(String alias) {
		String cfName=CF_OPERATIONS_STARTING_DATE+"_"+alias.toUpperCase();
		return cfName;
	}
}