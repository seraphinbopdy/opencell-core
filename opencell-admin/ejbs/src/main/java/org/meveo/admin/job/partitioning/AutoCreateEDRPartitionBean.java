package org.meveo.admin.job.partitioning;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.job.BaseJobBean;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.service.job.TablesPartitioningService;

@Stateless
public class AutoCreateEDRPartitionBean extends BaseJobBean {

    @Inject
    private TablesPartitioningService partitionService;

    private final static String EDR_QUERY_PATTERN = "select count(*) from create_new_edr_partition('%s', '%s', '%s')";

    @Interceptors(JobLoggingInterceptor.class)
    public void createNewEDRPartition(JobExecutionResultImpl result) {
        partitionService.createNewPartition(EDR_QUERY_PATTERN, partitionService.EDR_PARTITION_SOURCE, "EDR", result);
    }
}

