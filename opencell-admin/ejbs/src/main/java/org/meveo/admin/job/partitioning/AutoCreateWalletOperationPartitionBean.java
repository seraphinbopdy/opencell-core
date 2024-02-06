package org.meveo.admin.job.partitioning;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.job.BaseJobBean;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.service.job.TablesPartitioningService;

@Stateless
public class AutoCreateWalletOperationPartitionBean extends BaseJobBean {

    @Inject
    private TablesPartitioningService partitionService;

    private final static String WO_QUERY_PATTERN = "select count(*) from create_new_wo_partition('%s', '%s', '%s')";

    

    @Interceptors(JobLoggingInterceptor.class)
    public void createNewWOPartition(JobExecutionResultImpl result) {
        partitionService.createNewPartition(WO_QUERY_PATTERN, partitionService.WO_PARTITION_SOURCE, "WO", result);
    }
}
