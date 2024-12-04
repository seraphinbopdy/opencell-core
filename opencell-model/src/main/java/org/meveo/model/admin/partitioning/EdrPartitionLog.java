package org.meveo.model.admin.partitioning;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tech_edr_partition_log")
public class EdrPartitionLog extends AbstractPartitionLog {
}
