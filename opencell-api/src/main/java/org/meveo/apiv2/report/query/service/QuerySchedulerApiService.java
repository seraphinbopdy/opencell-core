package org.meveo.apiv2.report.query.service;

import static java.util.Optional.empty;

import java.util.*;

import jakarta.ejb.ScheduleExpression;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.apiv2.ordering.services.ApiService;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.MeveoJobCategoryEnum;
import org.meveo.model.jobs.TimerEntity;
import org.meveo.model.report.query.QueryScheduler;
import org.meveo.model.report.query.QueryTimer;
import org.meveo.model.report.query.ReportQuery;
import org.meveo.security.CurrentUser;
import org.meveo.security.MeveoUser;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.job.TimerEntityService;
import org.meveo.service.report.QuerySchedulerService;

public class QuerySchedulerApiService implements ApiService<QueryScheduler> {

    @Inject
    private QuerySchedulerService querySchedulerService;

	@Inject
	private TimerEntityService timerEntityService;
    
    @Inject
    private JobInstanceService jobInstanceService;
    
    @Inject
    private UserService userService;

    @Inject
    @CurrentUser
    protected MeveoUser currentUser;

	@Transactional
    @Override
    public QueryScheduler create(QueryScheduler inputQueryScheduler) {
        try {
			inputQueryScheduler.setUsersToNotify(getUsersToNotify(inputQueryScheduler));

        	ReportQuery reportQuery = inputQueryScheduler.getReportQuery();
        	String code = reportQuery.getCode() + "_Job";
			JobInstance jobInstance = jobInstanceService.findByCode(code);
			boolean isDisabledJob = !inputQueryScheduler.getIsQueryScheduler();
			TimerEntity inputTimer = toTimer(inputQueryScheduler.getQueryTimer());
			if (jobInstance == null) {
				jobInstance = new JobInstance();
				jobInstance.setTimerEntity(inputTimer);
			}else if(jobInstance.getQueryScheduler() != null){
				updateExistingQueryScheduler(jobInstance, inputQueryScheduler);
			}
			updateOrCreateTimer(jobInstance, inputTimer);
			jobInstance.setCode(code);
			jobInstance.setDescription("Job for report query='" + reportQuery.getCode() + "'");
			jobInstance.setJobCategoryEnum(MeveoJobCategoryEnum.REPORTING_QUERY);
			jobInstance.setJobTemplate("ReportQueryJob");
			jobInstance.setCfValue("reportQuery", reportQuery);
			jobInstance.setDisabled(isDisabledJob);
			if(jobInstance.getId() == null) {
				inputQueryScheduler.setJobInstance(jobInstance);
				querySchedulerService.create(inputQueryScheduler);
				jobInstanceService.create(jobInstance);
			}else{
				jobInstanceService.update(jobInstance);
			}
            return inputQueryScheduler;
        } catch (Exception exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

	private void updateOrCreateTimer(JobInstance jobInstance, TimerEntity inputTimer) {
		if(jobInstance.getTimerEntity() != null && jobInstance.getTimerEntity().getId() != null ){
			TimerEntity timerEntity = jobInstance.getTimerEntity();
			timerEntity.setActive(inputTimer.isActive());
			timerEntity.setCode(inputTimer.getCode());
			timerEntity.setJobInstances(List.of(jobInstance));
			timerEntity.setHour(inputTimer.getHour());
			timerEntity.setMinute(inputTimer.getMinute());
			timerEntity.setYear(inputTimer.getYear());
			timerEntity.setSecond(inputTimer.getSecond());
			timerEntity.setMonth(inputTimer.getMonth());
			timerEntity.setDayOfMonth(inputTimer.getDayOfMonth());
			timerEntity.setDayOfWeek(inputTimer.getDayOfWeek());
			timerEntity.setEnd(inputTimer.getEnd());
			timerEntity.setStart(inputTimer.getStart());
			timerEntity.setDescription(inputTimer.getDescription());
			timerEntity.setDisabled(inputTimer.isDisabled());
			timerEntityService.update(timerEntity);
		}else{
			timerEntityService.create(inputTimer);
			jobInstance.setTimerEntity(inputTimer);
		}
	}

	private void updateExistingQueryScheduler(JobInstance jobInstance, QueryScheduler inputQueryScheduler) {
		QueryScheduler existingQueryScheduler = jobInstance.getQueryScheduler();
		if(existingQueryScheduler != null) {
			existingQueryScheduler.setVersion(existingQueryScheduler.getVersion()+1);
			existingQueryScheduler.setIsQueryScheduler(inputQueryScheduler.getIsQueryScheduler());
			existingQueryScheduler.setQueryTimer(inputQueryScheduler.getQueryTimer());
			existingQueryScheduler.setUsersToNotify(inputQueryScheduler.getUsersToNotify());
			existingQueryScheduler.setEmailsToNotify(inputQueryScheduler.getEmailsToNotify());
			existingQueryScheduler.setFileFormat(inputQueryScheduler.getFileFormat());
			existingQueryScheduler.setIsQueryScheduler(inputQueryScheduler.getIsQueryScheduler());
		}else {
			jobInstance.setQueryScheduler(inputQueryScheduler);
		}
		querySchedulerService.update(jobInstance.getQueryScheduler());
	}

	private List<User> getUsersToNotify(QueryScheduler entity) {
		List<User> usersToNotify = new ArrayList<>();
		for(User element: entity.getUsersToNotify()) {
			User user = userService.findByUsername(element.getUserName(), false, false);
			if(user == null && element.getId() != null) {
				user = userService.findById(element.getId());
			}
			if(user == null) {
				throw new NotFoundException("The user with id {" + element.getId() + "} or userName {" + element.getUserName() + "} does not exists");
			}
			user.getUserRoles().size();
			usersToNotify.add(user);
		}
		return usersToNotify;
	}

	private TimerEntity toTimer(QueryTimer queryTimer) {
		TimerEntity timer = new TimerEntity();
		timer.setDayOfMonth(StringUtils.isBlank(queryTimer.getDayOfMonth()) ? "*" : (queryTimer.isEveryDayOfMonth() ? "*/" + queryTimer.getDayOfMonth() : queryTimer.getDayOfMonth()));
		timer.setDayOfWeek(StringUtils.isBlank(queryTimer.getDayOfWeek()) ? "*" : (queryTimer.isEveryDayOfWeek() ? "*/" + queryTimer.getDayOfWeek() : queryTimer.getDayOfWeek()));
		timer.setHour(StringUtils.isBlank(queryTimer.getHour()) ? "*" : (queryTimer.isEveryHour() ? "*/" + queryTimer.getHour() : queryTimer.getHour()));
		timer.setMinute(StringUtils.isBlank(queryTimer.getMinute()) ? "*" : (queryTimer.isEveryMinute() ? "*/" + queryTimer.getMinute() : queryTimer.getMinute()));
		timer.setMonth(StringUtils.isBlank(queryTimer.getMonth()) ? "*" : (queryTimer.isEveryMonth() ? "*/" + queryTimer.getMonth() : queryTimer.getMonth()));
		timer.setSecond(StringUtils.isBlank(queryTimer.getSecond()) ? "*" : (queryTimer.isEverySecond() ? "*/" + queryTimer.getSecond() : queryTimer.getSecond()));
		timer.setYear(StringUtils.isBlank(queryTimer.getYear()) ? "*" : queryTimer.getYear());
		timer.setCode(toCronFormat(getScheduleExpression(timer)));
		return timer;
	}
	private ScheduleExpression getScheduleExpression(TimerEntity timerEntity) {
		ScheduleExpression expression = new ScheduleExpression();
		expression.dayOfMonth(timerEntity.getDayOfMonth());
		expression.dayOfWeek(timerEntity.getDayOfWeek());
		expression.end(timerEntity.getEnd());
		expression.hour(timerEntity.getHour());
		expression.minute(timerEntity.getMinute());
		expression.month(timerEntity.getMonth());
		expression.second(timerEntity.getSecond());
		expression.start(timerEntity.getStart());
		expression.year(timerEntity.getYear());
		return expression;
	}
	private String toCronFormat(ScheduleExpression schedule) {
		return schedule.getSecond() + " " +
				schedule.getMinute() + " " +
				schedule.getHour() + " " +
				schedule.getDayOfMonth() + " " +
				schedule.getMonth() + " " +
				schedule.getDayOfWeek() + " " +
				schedule.getYear();
	}
	@Override
	public List<QueryScheduler> list(Long offset, Long limit, String sort, String orderBy, String filter) {
		return Collections.emptyList();
		
	}

	@Override
	public Long getCount(String filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<QueryScheduler> findById(Long id) {
		return empty();
	}

	@Override
	public Optional<QueryScheduler> update(Long id, QueryScheduler baseEntity) {
		return empty();
	}

	@Override
	public Optional<QueryScheduler> patch(Long id, QueryScheduler baseEntity) {
		return empty();
	}

	@Override
	public Optional<QueryScheduler> delete(Long id) {
		return empty();
	}

	@Override
	public Optional<QueryScheduler> findByCode(String code) {
		return empty();
	}

	public Optional<QueryScheduler> findByReportQueryId(ReportQuery reportQuery) {
		PaginationConfiguration paginationConfiguration = new PaginationConfiguration(Map.of("eq reportQuery", reportQuery));
		paginationConfiguration.setFetchFields(List.of("usersToNotify", "usersToNotify.userRoles"));
		List<QueryScheduler> list = querySchedulerService.list(paginationConfiguration);
		list.forEach(qs -> qs.getUsersToNotify().forEach(u -> u.getUserRoles().size()));
		return Optional.ofNullable(list.isEmpty() ? null : list.getFirst());
	}

}