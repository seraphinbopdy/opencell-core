package org.meveo.admin.job.partitioning;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.job.BaseJobBean;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.service.job.TablesPartitioningService;

@Stateless
public class AutoCreateRatedTransactionPartitionBean extends BaseJobBean {

    @Inject
    private TablesPartitioningService partitionService;

    private final static String RT_QUERY_PATTERN = "select count(*) from create_new_rt_partition('%s', '%s', '%s')";

    

    @Interceptors(JobLoggingInterceptor.class)
    public void createNewRTPartition(JobExecutionResultImpl result) {
        partitionService.createNewPartition(RT_QUERY_PATTERN, partitionService.RT_PARTITION_SOURCE, "RT", result);
    }
}

