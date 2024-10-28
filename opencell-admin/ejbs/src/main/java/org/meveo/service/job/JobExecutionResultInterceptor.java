package org.meveo.service.job;

import java.util.concurrent.atomic.AtomicInteger;

import org.meveo.commons.utils.EjbUtils;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.inject.Inject;
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

    @Inject
    private MeterRegistry meterRegistry;

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

        updateMetrics(result, "number_of_OKs", numberOfOKs);
        updateMetrics(result, "number_of_KOs", numberOfKOs);
        updateMetrics(result, "number_of_Remaining_Items", numberOfRemainingValues);
        updateMetrics(result, "number_of_Warnings", numberOfWarnings);

        try {
            return context.proceed();
        } catch (Exception e) {
            log.warn(" update of metrics failed because of : {}", e);
            return null;
        }
    }

    /**
     * Update gauge metrics for Job execution result statistics
     *
     * @param jobExecutionResultImpl Job execution result
     * @param name the name of metric
     * @param value Absolute value to set gauge to.
     */
    private void updateMetrics(JobExecutionResultImpl jobExecutionResultImpl, String name, Long value) {

        JobInstance jobInstance = jobExecutionResultImpl.getJobInstance();

        AtomicInteger gaugeValue = meterRegistry.gauge(name + "." + jobInstance.getJobTemplate() + "." + jobInstance.getCode(), Tags.of("name", jobInstance.getCode(), "node", EjbUtils.getCurrentClusterNode()),
            new AtomicInteger(0));

        gaugeValue.set(value.intValue());
    }
}