package org.meveo.admin.job.importexport;

import java.util.HashMap;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.service.job.Job;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class ImportCustomerBankDetailsJob extends Job {    
    @Inject
    private ImportCustomerBankDetailsJobBean importCustomerBankDetailsJobBean;
    
    private static final String APPLIES_TO_NAME = "JobInstance_ImportCustomerBankDetailsJob";

    @Override
    protected JobExecutionResultImpl execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        importCustomerBankDetailsJobBean.execute(result, jobInstance);
        return result;
    }

    @Override
    public JobCategoryEnum getJobCategory() {
        return MeveoJobCategoryEnum.IMPORT_HIERARCHY;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
    	 Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();    
        CustomFieldTemplate nbRuns = new CustomFieldTemplate();
        nbRuns.setCode("ImportCustomerBankDetailsJob_nbRuns");
        nbRuns.setAppliesTo(APPLIES_TO_NAME);
        nbRuns.setActive(true);
        nbRuns.setDescription(resourceMessages.getString("jobExecution.nbRuns"));
        nbRuns.setFieldType(CustomFieldTypeEnum.LONG);
        nbRuns.setValueRequired(false);
        nbRuns.setDefaultValue("1");
        result.put("nbRuns", nbRuns);

        CustomFieldTemplate waitingMillis = new CustomFieldTemplate();
        waitingMillis.setCode("ImportCustomerBankDetailsJob_waitingMillis");
        waitingMillis.setAppliesTo(APPLIES_TO_NAME);
        waitingMillis.setActive(true);
        waitingMillis.setDescription(resourceMessages.getString("jobExecution.waitingMillis"));
        waitingMillis.setFieldType(CustomFieldTypeEnum.LONG);
        waitingMillis.setValueRequired(false);
        waitingMillis.setDefaultValue("0");
        result.put("waitingMillis", waitingMillis);
        return result;
    }
    
}