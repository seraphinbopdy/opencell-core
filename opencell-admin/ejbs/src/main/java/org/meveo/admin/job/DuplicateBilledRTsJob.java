package org.meveo.admin.job;

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
import org.meveo.service.settings.impl.AdvancedSettingsService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A job implementation to duplicate billed rated transactions
 *
 * @author Abdellatif BARI
 * @since 17.0.0
 */
@Stateless
public class DuplicateBilledRTsJob extends Job {

    @Inject
    private DuplicateBilledRTsJobBean duplicateBilledRTsJobBean;

    @Inject
    private AdvancedSettingsService advancedSettingsService;

    @Inject
    private TablesPartitioningService tablesPartitioningService;

    private static final String JOB_INSTANCE_DUPLICATION_BILLED_RTS_JOB = "JobInstance_DuplicateBilledRTsJob";
    private static final String CF_OPERATIONS_STARTING_DATE = "operationsStartingDate";
    private static final String USE_LAST_PARTITION = "LAST PARTITION DATE";
    private static final String NO_DATE_LIMITE = "NO DATE LIMITE";

    @Override
    protected JobExecutionResultImpl execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        Map<String, Object> advancedSettingsValues = advancedSettingsService.getAdvancedSettingsMapByGroup("rating", Object.class);
        Boolean allowBilledItemsRerating = (Boolean) advancedSettingsValues.get("rating.allowBilledItemsRerating");
        if (Boolean.TRUE.equals(allowBilledItemsRerating)) {
            duplicateBilledRTsJobBean.execute(result, jobInstance);
        }
        return result;
    }

    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.INVOICING;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<>();

        result.put(CF_NB_RUNS, CustomFieldTemplateUtils.buildCF(CF_NB_RUNS, resourceMessages.getString("jobExecution.nbRuns"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:0", "-1", JOB_INSTANCE_DUPLICATION_BILLED_RTS_JOB));

        result.put(Job.CF_WAITING_MILLIS, CustomFieldTemplateUtils.buildCF(Job.CF_WAITING_MILLIS, resourceMessages.getString("jobExecution.waitingMillis"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:1", "0", JOB_INSTANCE_DUPLICATION_BILLED_RTS_JOB));

        result.put(CF_BATCH_SIZE, CustomFieldTemplateUtils.buildCF(CF_BATCH_SIZE, resourceMessages.getString("jobExecution.batchSize"), CustomFieldTypeEnum.LONG,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:2", "10000", true, JOB_INSTANCE_DUPLICATION_BILLED_RTS_JOB));

        CustomFieldTemplate cft = CustomFieldTemplateUtils.buildCF(CF_OPERATIONS_STARTING_DATE,
                resourceMessages.getString("jobExecution.operation.starting.date", "rt"), CustomFieldTypeEnum.LIST,
                "tab:Configuration:0;fieldGroup:Configuration:0;field:3", NO_DATE_LIMITE, false,
                JOB_INSTANCE_DUPLICATION_BILLED_RTS_JOB);
        List<String> values = tablesPartitioningService.listPartitionsStartDate("rt");
        Map<String, String> options = new LinkedHashMap<>(Map.of(
                NO_DATE_LIMITE, NO_DATE_LIMITE,
                USE_LAST_PARTITION, USE_LAST_PARTITION));
        if (CollectionUtils.isNotEmpty(values)) {
            values.stream().forEach(item -> options.put(item, item));
        }
        cft.setListValues(options);
        cft.setTags(CustomFieldTemplateService.UPDATE_EXISTING);
        result.put(CF_OPERATIONS_STARTING_DATE, cft);

        return result;
    }

}