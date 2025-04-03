package org.meveo.admin.job;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.utils.CustomFieldTemplateUtils;
import org.meveo.model.crm.CustomFieldTemplate;
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
public class ReRatingV2Job extends Job {

    private static final String JOB_INSTANCE_RE_RATING_V2_JOB = "JobInstance_ReRatingV2Job";

    public static final String CF_NR_ITEMS_PER_TX = "JobInstance_ReRatingV2Job_MaxRTsPerTransaction";

	public static final String CF_OPERATIONS_STARTING_DATE="CF_OPERATIONS_STARTING_DATE";
	
	public static final String USE_LAST_PARTITION="LAST PARTITION DATE";
	
	public static final String NO_DATE_LIMITE="NO DATE LIMITE";
	
    @Inject
	TablesPartitioningService tablesPartitioningService;
	
    @Inject
    private ReRatingV2JobBean jobBean;

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
                "tab:Configuration:0;fieldGroup:Configuration:0;field:0", "-1", JOB_INSTANCE_RE_RATING_V2_JOB));
        result.put(Job.CF_WAITING_MILLIS, CustomFieldTemplateUtils.buildCF(Job.CF_WAITING_MILLIS, resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:1", "0", JOB_INSTANCE_RE_RATING_V2_JOB));
        result.put(CF_NR_ITEMS_PER_TX, CustomFieldTemplateUtils.buildCF(CF_NR_ITEMS_PER_TX, resourceMessages.getString("jobExecution.numberOfItems"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:2", "500", true, JOB_INSTANCE_RE_RATING_V2_JOB));

        CustomFieldTemplate cft = CustomFieldTemplateUtils.buildCF(CF_OPERATIONS_STARTING_DATE,
                resourceMessages.getString("jobExecution.operation.starting.date","edr"), CustomFieldTypeEnum.LIST,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:3", NO_DATE_LIMITE, false,
                JOB_INSTANCE_RE_RATING_V2_JOB);
		List<String> values=tablesPartitioningService.listPartitionsStartDate("edr");
		Map<String, String> options = new LinkedHashMap<>(Map.of(
                NO_DATE_LIMITE, NO_DATE_LIMITE,
                USE_LAST_PARTITION, USE_LAST_PARTITION));
		if(CollectionUtils.isNotEmpty(values)) {
			values.stream().forEach(item->options.put(item, item));
		}
		cft.setListValues(options);
		cft.setTags(CustomFieldTemplateService.UPDATE_EXISTING);
        result.put(CF_OPERATIONS_STARTING_DATE, cft);
		
        
        return result;
    }
}