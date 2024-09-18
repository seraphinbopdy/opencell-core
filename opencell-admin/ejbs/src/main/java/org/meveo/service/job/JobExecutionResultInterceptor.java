package org.meveo.service.job;

import org.meveo.model.jobs.JobExecutionResultImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Interceptor to update the Jobs informations in realtime
 *
 * @author Mohamed Ali Hammal
 * @since 11.X
 */
public class JobExecutionResultInterceptor {

    /**
     * class logger
     */
    private static final Logger log = LoggerFactory.getLogger(JobExecutionResultInterceptor.class);

//    @Inject
//    @RegistryScope(scope = MetricRegistry.APPLICATION_SCOPE)
//    MetricRegistry registry;
    // Akk migrate me

    /**
     * Update metrics for Prometheus a method on an entity
     *
     * @param context the method invocation context
     * @return the method result if update is OK
     * @throws Exception if the update failed
     */
    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        Object[] entity = context.getParameters();
        JobExecutionResultImpl result = (JobExecutionResultImpl) entity[0];

        // Update the counters
        long numberOfOKs = result.getNbItemsCorrectlyProcessed();
        long numberOfKOs = result.getNbItemsProcessedWithError();
        long numberOfRemainingValues = (result.getNbItemsToProcess() - numberOfOKs - numberOfKOs);
        long numberOfWarnings = result.getNbItemsProcessedWithWarning();

        counterInc(result, "number_of_OKs", numberOfOKs);
        counterInc(result, "number_of_KOs", numberOfKOs);
        counterInc(result, "number_of_Remaining_Items", numberOfRemainingValues);
        counterInc(result, "number_of_Warnings", numberOfWarnings);

        try {
            return context.proceed();
        } catch (Exception e) {
            log.warn(" update of metrics failed because of : {}", e);
            return null;
        }
    }

    /**
     * Increment counter metric for JobExecutionResultImpl
     *
     * @param value
     * @param name the name of metric
     */
    private void counterInc(JobExecutionResultImpl jobExecutionResultImpl, String name, Long value) {
//        JobInstance jobInstance = jobExecutionResultImpl.getJobInstance();
//        Metadata metadata = new MetadataBuilder().withName(name + "_" + jobInstance.getJobTemplate() + "_" + jobInstance.getCode()).build();
//        Tag tgName = new Tag("name", jobInstance.getCode());
//        Counter counter = registry.counter(metadata, tgName);
//        if (value != null) {
//            counter.inc(value - counter.getCount());
//        } else {
//            counter.inc();
//        }
     // Akk migrate me
    }
}
