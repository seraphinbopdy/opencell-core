package org.meveo.service.job;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.meveo.commons.utils.EjbUtils;
import org.meveo.model.jobs.JobInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Interceptor to update the Jobs number of threads in realtime
 *
 * @author Mohamed Ali Hammal
 * @since 11.X
 */
public class JobExecutionInterceptor {

    /**
     * class logger
     */
    private static final Logger log = LoggerFactory.getLogger(JobExecutionInterceptor.class);

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
        Object[] params = context.getParameters();

        long numberOfThreads = 0;
        long isRunning = 0;
        long isStopped = 1;

        if (params.length == 3) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<Future> futures = (List<Future>) params[2];
            if (futures != null && !futures.isEmpty()) {
                numberOfThreads = futures.size();
                isRunning = 1;
                isStopped = 0;
            }
        }
        updateMetrics((JobInstance) params[0], "number_of_Threads", numberOfThreads);
        updateMetrics((JobInstance) params[0], "is_running", isRunning);
        updateMetrics((JobInstance) params[0], "is_stopped", isStopped);
        try {
            return context.proceed();
        } catch (Exception e) {
            log.warn(" update of metrics failed because of : {}", e);
            return null;
        }
    }

    /**
     * Update gauge metrics for Job execution statistics
     *
     * @param jobInstance Job instance
     * @param name the name of metric
     * @param value Absolute value to set gauge value to.
     */
    private void updateMetrics(JobInstance jobInstance, String name, Long value) {

        AtomicInteger gaugeValue = meterRegistry.gauge(name + "." + jobInstance.getJobTemplate() + "." + jobInstance.getCode(), Tags.of("name", jobInstance.getCode(), "node", EjbUtils.getCurrentClusterNode()),
            new AtomicInteger(0));

        gaugeValue.set(value.intValue());
    }
}
