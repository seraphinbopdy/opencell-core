package org.meveo.apiv2.accounts.impl;

import static jakarta.ws.rs.core.Response.ok;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.meveo.api.dto.account.UserAccountsDto;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.accounts.resource.UserAccountsResource;
import org.meveo.apiv2.accounts.service.UserAccountsApiService;

@Interceptors({ WsRestApiInterceptor.class })
public class UserAccountsResourceImpl implements UserAccountsResource {

    @Inject
    private UserAccountsApiService userAccountsApiService;

	@Override
	public Response allowedUserAccountParents(String userAccountCode) {
		UserAccountsDto userAccounts = userAccountsApiService.allowedUserAccountParents(userAccountCode);
		ResponseBuilder response = ok();
		if( userAccounts != null ) {
			response.entity(userAccounts);
		}
		return response.build();
		
	}


}
