package org.meveo.admin.job.invoicing;

import static java.lang.Long.valueOf;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.admin.job.IteratorBasedJobBean;
import org.meveo.admin.job.utils.BillinRunApplicationElFilterUtils;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.InvoiceLine;
import org.meveo.model.crm.EntityReferenceWrapper;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.BillingRunService;
import org.meveo.service.billing.impl.InvoiceLineService;
import org.meveo.service.job.Job;
import org.primefaces.model.SortOrder;

@Stateless
public class InvoiceTaxCalculationJobBean extends IteratorBasedJobBean<Long> {

    private static final long serialVersionUID = 1L;

	public static final String FIELD_PRIORITY_SORT = "billingCycle.priority, auditable.created";
    
    @Inject
    private InvoiceLineService invoiceLineService;

    @Inject
    private BillingRunService billingRunService;

    @Inject
    @MeveoJpa
    private EntityManagerWrapper emWrapper;

    private StatelessSession statelessSession;
    private ScrollableResults scrollableResults;

    private Long nrOfRecords = null;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED) // Transaction set to REQUIRED, so ScrollableResultset would apply paging. NEVER change this value!
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        super.execute(jobExecutionResult, jobInstance, this::getSynchronizedIterator, null, null, this::calculateTaxAndAmounts, this::hasMore, this::closeResultset, null);
    }

    private void calculateTaxAndAmounts(List<Long> invoiceLines, JobExecutionResultImpl jobExecutionResult) {
        invoiceLineService.calculateTax(invoiceLines, jobExecutionResult);
    }

    private boolean hasMore(JobInstance jobInstance) {
        return false;
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
    }
    

    @Override
    protected boolean isProcessItemInNewTx() {
        return true;
    }

    
    private Optional<Iterator<Long>> getSynchronizedIterator(JobExecutionResultImpl jobExecutionResult) {
        JobInstance jobInstance = jobExecutionResult.getJobInstance();
        
        List<BillingRun> billingRuns = getBillingRunsToProcess(jobInstance, jobExecutionResult);
        if (billingRuns == null || billingRuns.isEmpty()) {
            return Optional.empty();
        }
        
        Long batchSize = (Long) getParamOrCFValue(jobInstance, Job.CF_BATCH_SIZE, 10000L);
        Long nbThreads = (Long) this.getParamOrCFValue(jobInstance, Job.CF_NB_RUNS, -1L);
        if (nbThreads == -1) {
            nbThreads = (long) Runtime.getRuntime().availableProcessors();
        }
        int fetchSize = batchSize.intValue() * nbThreads.intValue();

        List<Long> BRids =billingRuns.stream().map(BillingRun::getId).collect(Collectors.toList());
		List<Object[]> convertSummary = (List<Object[]>) emWrapper.getEntityManager().createNamedQuery("InvoiceLines.getOpenByBillingRunSummary").setParameter("billingRuns", BRids).getResultList();

        nrOfRecords = convertSummary.stream().mapToLong(obj -> (Long) obj[0]).sum();

        if (nrOfRecords.intValue() == 0) {
            return Optional.empty();
        }

        statelessSession = emWrapper.getEntityManager().unwrap(Session.class).getSessionFactory().openStatelessSession();
        scrollableResults = statelessSession.createNamedQuery("InvoiceLine.listOpenByBillingRuns").setParameter("billingRuns", BRids).setReadOnly(true).setCacheable(false)
            .setFetchSize(fetchSize).scroll(ScrollMode.FORWARD_ONLY);


        return Optional.of(new SynchronizedIterator<Long>(scrollableResults, nrOfRecords.intValue()));
    }
    
    private List<BillingRun> getBillingRunsToProcess(JobInstance jobInstance, JobExecutionResultImpl jobExecutionResult) {

        List<EntityReferenceWrapper> billingRunWrappers = (List<EntityReferenceWrapper>) this.getParamOrCFValue(jobInstance, InvoiceTaxCalculationJob.CF_TAX_CALCULATION_BR);
        List<Long> billingRunIds = billingRunWrappers != null ? billingRunWrappers.stream().map(br -> valueOf(br.getCode().split("/")[0])).collect(toList()) : emptyList();
        Map<String, Object> filters = new HashMap<>();
        if (billingRunIds.isEmpty()) {
            filters.put("inList status", Arrays.asList(BillingRunStatusEnum.INVOICE_LINES_CREATED));
        } else {
            filters.put("inList id", billingRunIds);
        }
        PaginationConfiguration pagination = new PaginationConfiguration(null, null, filters,
                null, asList("billingCycle"), FIELD_PRIORITY_SORT, SortOrder.ASCENDING);

        List<BillingRun> billingRuns = BillinRunApplicationElFilterUtils.filterByApplicationEL(billingRunService.list(pagination), jobInstance);

        // Extra validation of BR status when billing run list is provided as parameters
        if (!billingRunIds.isEmpty() && !billingRuns.isEmpty()) {
            List<BillingRun> excludedBRs = billingRuns.stream().filter(br -> br.getStatus() != BillingRunStatusEnum.INVOICE_LINES_CREATED)
                .collect(toList());
            excludedBRs.forEach(br -> jobExecutionResult.registerWarning(format("BillingRun[id={%d}] has been ignored as it neither NEW nor OPEN status", br.getId())));
            billingRuns.removeAll(excludedBRs);
            if (billingRuns.isEmpty()) {
                jobExecutionResult.registerError("No valid billing run with status = NEW or OPEN found");
            }
        }
        return billingRuns;
    }

}