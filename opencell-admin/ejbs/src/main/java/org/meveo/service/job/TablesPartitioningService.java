package org.meveo.service.job;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.hibernate.JDBCException;
import org.hibernate.query.NativeQuery;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobExecutionResultStatusEnum;
import org.meveo.service.base.NativePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class TablesPartitioningService extends NativePersistenceService {

	private static final String PARTITION_SUFFIX_PATTERN = "yyyyMM";
	private static final String PARTITION_DATE_PATTERN = "yyyy-MM-dd";

	public final static String EDR_PARTITION_SOURCE = "rating_edr_other";
	public final static String WO_PARTITION_SOURCE = "billing_wallet_operation_other";
	public final static String RT_PARTITION_SOURCE = "billing_rated_transaction_other";

	private static final String LIST_PARTITIONS_QUERY = "	select TO_DATE(SUBSTRING(child.relname FROM '\\d{6}$'), :datePattern) as partition_date FROM pg_inherits \n"
			+ "			JOIN pg_class parent ON pg_inherits.inhparent = parent.oid\n"
			+ "	    	JOIN pg_class child ON pg_inherits.inhrelid = child.oid\n"
			+ "		WHERE parent.relname = :tableName \n" 
			+ "		ORDER BY partition_date desc nulls last limit 1";

	@Inject
	@MeveoJpa
	private EntityManagerWrapper emWrapper;

	private static final Logger LOG = LoggerFactory.getLogger(TablesPartitioningService.class);

	@Interceptors(JobLoggingInterceptor.class)
	public void createNewPartition(String partitionQueryPattern, String partitionSource, String jobName,
			JobExecutionResultImpl result) {

		// Partition for next month
		LocalDate firstDayOfNextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1);

		String startingRange = firstDayOfNextMonth.format(DateTimeFormatter.ofPattern(PARTITION_DATE_PATTERN));

		// prepare partition name
		String partitionName = partitionSource + "_"
				+ firstDayOfNextMonth.format(DateTimeFormatter.ofPattern(PARTITION_SUFFIX_PATTERN));

		String newPartitionQuery = String.format(partitionQueryPattern, partitionSource, partitionName, startingRange);

		try {
			LOG.info("Create new partition [{}] starting from [{}]", partitionName, startingRange);
			EntityManager entityManager = emWrapper.getEntityManager();
			Query nativeQuery = entityManager.createNativeQuery(newPartitionQuery);
			nativeQuery.getSingleResult();
			result.registerSucces();
		} catch (Exception e) {
			String message = handleException(e, jobName);
			result.registerError(message);
			result.setStatus(JobExecutionResultStatusEnum.FAILED);
		}
	}

	private String handleException(Exception e, String jobName) {
		LOG.error("Error while trying to create new partition for {}", jobName, e);
		String message = e.getMessage();
		if (e instanceof PersistenceException) {
			Throwable cause = e.getCause();
			if (cause instanceof JDBCException) {
				message = ((JDBCException) cause).getSQLException().getMessage();
			}
		}
		return message;
	}

	public String getLastPartitionDate(String tableName) {

		EntityManager entityManager = emWrapper.getEntityManager();
		NativeQuery nativeQuery = (NativeQuery) entityManager.createNativeQuery(LIST_PARTITIONS_QUERY);
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(PARTITION_DATE_PATTERN);
			Date resultDate = (Date) nativeQuery.setParameter("tableName", tableName).setParameter("datePattern", PARTITION_SUFFIX_PATTERN).getSingleResult();
			return resultDate == null ? null : dateFormat.format(resultDate);
		} catch (NoResultException e) {
			return null;
		}
	}

}