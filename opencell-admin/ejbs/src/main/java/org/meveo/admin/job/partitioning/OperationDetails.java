package org.meveo.admin.job.partitioning;
public class OperationDetails {
	private String entityName;
	private String tableName;
	private String columnName;
	private String partitionSource;
	private String alias;
	public OperationDetails(String entityName, String tableName, String columnName, String partitionSource,
			String alias) {
		this.alias = alias;
		this.entityName = entityName;
		this.tableName = tableName;
		this.columnName = columnName;
		this.partitionSource = partitionSource;
	}
	public String getAlias() {
		return alias;
	}
	public String getEntityName() {
		return entityName;
	}
	public String getTableName() {
		return tableName;
	}
	public String getColumnName() {
		return columnName;
	}
	public String getPartitionSource() {
		return partitionSource;
	}
}