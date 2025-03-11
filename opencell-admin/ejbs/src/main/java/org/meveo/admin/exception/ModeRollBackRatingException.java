/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.admin.exception;

import jakarta.ejb.ApplicationException;
import org.meveo.apiv2.billing.ProcessCdrListResult;


/**
 * Groups rating related exceptions
 * 
 * @author Andrius Karpavicius
 */
@ApplicationException(rollback = true)
public class ModeRollBackRatingException extends RuntimeException {

    private static final long serialVersionUID = -4006839791846079300L;

	private ProcessCdrListResult processCdrListResult;

    public ModeRollBackRatingException() {
        super();
    }

    public ModeRollBackRatingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModeRollBackRatingException(String message) {
        super(message);
    }

    public ModeRollBackRatingException(Throwable cause) {
        super(cause);
    }
	
	public ModeRollBackRatingException(ProcessCdrListResult processCdrListResult, String message) {
		super(message);
		this.processCdrListResult = processCdrListResult;
	}
	
	public ProcessCdrListResult getProcessCdrListResult() {
		return processCdrListResult;
	}



	
}