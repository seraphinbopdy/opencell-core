package org.meveo.service.script.payment;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.meveo.admin.exception.BusinessException;
import org.meveo.service.script.Script;

public class TestRejectionActionScript extends Script {

	private static final long serialVersionUID = 1L;

	@Override
	public void execute(Map<String, Object> context) throws BusinessException {

		String executionResult = String.valueOf(context.get("executionResult"));
		String report = String.valueOf(context.get("report"));
		Integer delay = (Integer) context.get("delay");

		try {
			TimeUnit.SECONDS.sleep(delay);
		} catch (InterruptedException e) {
			log.error("", e);
		}

		if ("failure".equals(executionResult)) {
			throw new BusinessException(report);
		}

		context.put(Script.RESULT_VALUE, report);
	}

}
