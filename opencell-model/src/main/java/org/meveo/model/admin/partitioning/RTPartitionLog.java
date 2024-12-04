package org.meveo.model.admin.partitioning;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tech_rt_partition_log")
public class RTPartitionLog extends AbstractPartitionLog {
}
