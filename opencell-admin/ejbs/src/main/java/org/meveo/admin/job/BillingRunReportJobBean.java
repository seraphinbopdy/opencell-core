package org.meveo.admin.job;

import static java.lang.Long.valueOf;
import static java.util.Collections.emptyList;
import static org.meveo.model.billing.BillingRunReportTypeEnum.BILLED_RATED_TRANSACTIONS;
import static org.meveo.model.billing.BillingRunReportTypeEnum.OPEN_RATED_TRANSACTIONS;
import static org.meveo.model.billing.BillingRunStatusEnum.NEW;
import static org.meveo.model.billing.BillingRunStatusEnum.OPEN;

import java.util.List;
import java.util.Map;

import org.meveo.commons.utils.MethodCallingUtils;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunReport;
import org.meveo.model.billing.BillingRunReportTypeEnum;
import org.meveo.model.crm.EntityReferenceWrapper;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.BillingRunReportService;
import org.meveo.service.billing.impl.BillingRunService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class BillingRunReportJobBean extends BaseJobBean {

    @Inject
    private BillingRunReportService billingRunReportService;

    @Inject
    private BillingRunService billingRunService;

    private List<Long> billingRunIds;

    @Inject
    private MethodCallingUtils methodCallingUtils;

    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        methodCallingUtils.callMethodInNewTx(() -> executeInTx(jobExecutionResult, jobInstance));
    }

    private void executeInTx(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {

        List<EntityReferenceWrapper> billingRunWrappers =
                (List<EntityReferenceWrapper>) this.getParamOrCFValue(jobInstance, "billingRuns");
        billingRunIds = billingRunWrappers != null ? extractBRIds(billingRunWrappers) : emptyList();
        List<BillingRun> billingRuns;
        try {
            billingRuns = initJobAndGetDataToProcess();
            jobExecutionResult.setNbItemsToProcess(billingRuns.size());
            Map<String, Object> filters = jobInstance.getRunTimeValues() != null
                    ? (Map<String, Object>) jobInstance.getRunTimeValues().get("filters") : null;
            BillingRunReportTypeEnum reportType = filters == null ? OPEN_RATED_TRANSACTIONS : BILLED_RATED_TRANSACTIONS;
            jobExecutionResult.registerSucces(createBillingRunReport(billingRuns, jobExecutionResult, filters, reportType));
        } catch (Exception exception) {
            jobExecutionResult.registerError(exception.getMessage());
            log.error(exception.getMessage());
        }

    }

    private List<Long> extractBRIds(List<EntityReferenceWrapper> billingRunWrappers) {
    	if(billingRunWrappers == null || billingRunWrappers.isEmpty()) {
            return emptyList();
        }
        return billingRunWrappers.stream()
                    .map(br -> valueOf(br.getCode().split("/")[0]))
                    .toList();
    }

    private List<BillingRun> initJobAndGetDataToProcess() {
        if(billingRunIds != null && !billingRunIds.isEmpty()) {
            return billingRunIds.stream()
                    .map(id -> billingRunService.findById(id))
                    .toList();
        }
        return billingRunService.getBillingRuns(NEW, OPEN);
    }

    private int createBillingRunReport(List<BillingRun> billingRuns, JobExecutionResultImpl result,
                                       Map<String, Object> filters, BillingRunReportTypeEnum reportType) {
        int countOfReportCreated = 0;
        for (BillingRun billingRun : billingRuns) {
            if (filters != null && !filters.isEmpty()) {
                filters.put("billingRun", billingRun);
            }
            BillingRunReport billingRunReport =
                    billingRunReportService.createBillingRunReport(billingRun, reportType);
            billingRun = billingRunService.refreshOrRetrieve(billingRun);
            billingRun.setPreInvoicingReport(billingRunReport);
            billingRun.addJobExecutions(result);
            billingRunService.update(billingRun);
            countOfReportCreated++;
        }
        return countOfReportCreated;
    }
}
