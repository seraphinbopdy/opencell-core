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

package org.meveo.apiv2.customaction;

import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.meveo.admin.exception.ConflictCustomActionException;
import org.meveo.admin.exception.ElementNotFoundException;
import org.meveo.api.EntityCustomActionApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.models.Resource;

@Interceptors({ WsRestApiInterceptor.class })
public class CustomActionResourceImpl implements CustomActionResource {

	@Inject
	private EntityCustomActionApi entityCustomActionApi;

	@Override
	public Response execute(String entityType, String actionCode, Resource customActionDto) {
		ActionStatus responseStatus = new ActionStatus();
		try {
			entityCustomActionApi.execute(actionCode, entityType, customActionDto);
			responseStatus.setStatus(ActionStatusEnum.SUCCESS);
			return Response.ok(responseStatus).build();
		} catch (ElementNotFoundException e) {
			return toError(e, Status.NOT_FOUND);
		} catch (ConflictCustomActionException e) {
			return toError(e, Status.CONFLICT);
		} catch (Exception e) {
			return toError(e, Status.BAD_REQUEST);
		}
	}

	private Response toError(Exception e, Status s) {
		ResponseBuilder rb = Response.status(s);
		rb.entity(e.getLocalizedMessage());
		return rb.build();
	}

}
