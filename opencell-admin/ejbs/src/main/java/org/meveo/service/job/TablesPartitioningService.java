package org.meveo.service.job;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

@Stateless
public class TablesPartitioningService extends NativePersistenceService {

	private static final String PARTITION_SUFFIX_PATTERN = "yyyyMM";
	private static final String PARTITION_DATE_PATTERN = "yyyy-MM-dd";

	public final static String EDR_PARTITION_SOURCE = "rating_edr_other";
	public final static String WO_PARTITION_SOURCE = "billing_wallet_operation_other";
	public final static String RT_PARTITION_SOURCE = "billing_rated_transaction_other";

	private static final String GET_LAST_PARTITIONS_QUERY = "select max(range_from) as range_from, max(range_to) as range_to from tech_%s_partition_log";
	private static final String LIST_PARTITIONS_QUERY = "select to_char(range_from, '%s') from tech_%s_partition_log order by range_from desc";
			
	
	@Inject
	@MeveoJpa
	private EntityManagerWrapper emWrapper;

	private static final Logger LOG = LoggerFactory.getLogger(TablesPartitioningService.class);

	@Interceptors(JobLoggingInterceptor.class)
	public void createNewPartition(String partitionQueryPattern, String partitionSource, String alias,
			JobExecutionResultImpl result) {

		// Next partition:
		Date nextPartitionStartDate = getLastPartitionDate(alias)[1];

		String startingRange = getDateAsString(nextPartitionStartDate);

		// prepare partition name
		SimpleDateFormat dateFormat = new SimpleDateFormat(PARTITION_SUFFIX_PATTERN);
		String partitionName = partitionSource + "_" + dateFormat.format(nextPartitionStartDate);

		String newPartitionQuery = String.format(partitionQueryPattern, alias, partitionSource, partitionName, startingRange);

		try {
			EntityManager entityManager = emWrapper.getEntityManager();
			Query nativeQuery = entityManager.createNativeQuery(newPartitionQuery);
			LOG.info("Create new partition [{}] starting from [{}], query: {}", partitionName, startingRange, newPartitionQuery);
			nativeQuery.getSingleResult();
			result.registerSucces();
			result.addReport(partitionName + " created with success");
		} catch (Exception e) {
			String message = handleException(e, alias);
			result.registerError(message);
			result.setStatus(JobExecutionResultStatusEnum.FAILED);
		}
	}

	private String handleException(Exception e, String alias) {
		LOG.error("Error while trying to create new partition for {}", alias, e);
		String message = e.getMessage();
		if (e instanceof PersistenceException) {
			Throwable cause = e.getCause();
			if (cause instanceof JDBCException) {
				message = ((JDBCException) cause).getSQLException().getMessage();
			}
		}
		return message;
	}

	public String getLastPartitionStartingDateAsString(String alias) {
		Date resultDate = getLastPartitionDate(alias)[0];
		return getDateAsString(resultDate);
	}

	public String getDateAsString(Date resultDate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(PARTITION_DATE_PATTERN);
		return resultDate == null ? null : dateFormat.format(resultDate);
	}
	
	public Date parseDate(String dateString) {
	       SimpleDateFormat formatter = new SimpleDateFormat(PARTITION_DATE_PATTERN);
	        try {
	            return formatter.parse(dateString);
	        } catch (Exception e) {
	        	LOG.error("Error trying to parse date {} ",dateString,e);
	            return null;
	        }
	}

	public Date[] getLastPartitionDate(String alias) {
		EntityManager entityManager = emWrapper.getEntityManager();
		NativeQuery nativeQuery = (NativeQuery) entityManager.createNativeQuery(String.format(GET_LAST_PARTITIONS_QUERY, alias));
		try {
			Object[] result = (Object[])nativeQuery.getSingleResult();
			return new Date[]{(Date) result[0], (Date) result[1]};
		} catch (NoResultException e) {
			return null;
		}
	}
	
	public List<String> listPartitionsStartDate(String alias) {
		EntityManager entityManager = emWrapper.getEntityManager();
		NativeQuery nativeQuery = (NativeQuery) entityManager.createNativeQuery(String.format(LIST_PARTITIONS_QUERY, PARTITION_DATE_PATTERN, alias));
		try {
			List<Object> result = nativeQuery.getResultList();
			return result != null ? result.stream().map(Object::toString).collect(Collectors.toList()) : null;
		} catch (NoResultException e) {
			return null;
		}
	}

}