package org.meveo.admin.job;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.meveo.admin.async.SynchronizedIterator;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.ServiceInstanceService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * Job definition to calculate MRR
 * 
 * @since 17.0
 */
@Stateless
public class MRRCalculationJobBean extends IteratorBasedJobBean<ServiceInstance> {
    
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MRRCalculationJobBean.class);

    @Inject
    private ServiceInstanceService serviceInstanceService;

    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private BillingAccountService billingAccountService;
    
    @Inject
    private OfferTemplateService offerTemplateService;
    
    /**
     * Execute the job
     * 
     * @param jobExecutionResult Job execution result
     * @param jobInstance Job instance
     */
    public void execute(JobExecutionResultImpl jobExecutionResult, JobInstance jobInstance) {
        execute(jobExecutionResult, jobInstance, this::initJobAndGetDataToProcess, null, this::calculateMRR, null, null, null, this::finalizeCalculation);
    }

    /**
     * Update all ascendent dependencies (Subscription, BillingAccount and OfferTemplate) of a ServiceInstance
     * 
     * 
     * @param jobExecutionResult Job execution result
     */
    private void finalizeCalculation(JobExecutionResultImpl jobExecutionResult) {
        
        subscriptionService.massCalculateMRR();
        billingAccountService.massCalculateMRR();
        offerTemplateService.massCalculateARR();
        
    }

    /**
     * Initialize job and get data to process. This Job should process all ACTIVE Recurrent ServiceInstance
     * 
     * @param jobExecutionResult - Execution result
     * @return - ServiceInstances to process
     */
    private Optional<Iterator<ServiceInstance>> initJobAndGetDataToProcess(JobExecutionResultImpl jobExecutionResult) {
        List<ServiceInstance> serviceInstances = serviceInstanceService.listActiveRecurrentServiceInstances();
        return Optional.of(new SynchronizedIterator<>(serviceInstances));
    }

    /**
     * Calculate MRR for a ServiceInstance
     * 
     * @param serviceInstance - ServiceInstance to calculate MRR for
     * @param jobExecutionResult - Execution result
     */
    private void calculateMRR(ServiceInstance serviceInstance, JobExecutionResultImpl jobExecutionResult) {
        // Calculate MRR for a ServiceInstance
        log.info("Calculating MRR for serviceInstance {}", serviceInstance.getCode());
        try {
            serviceInstanceService.calculateMRR(serviceInstance);
        } catch (Exception e) {
            log.error("Failed to calculate MRR for serviceInstance #{}", serviceInstance.getId(), e);
            jobExecutionResult.unRegisterSucces();
            jobExecutionResult.registerError(serviceInstance.getId(), e.getMessage());
        }


    }
    
}
