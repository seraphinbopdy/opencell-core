package org.meveo.apiv2.accounts.impl;

import org.meveo.api.account.UserAccountApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.account.UserAccountDto;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.apiv2.accounts.resource.UserAccountV2Resource;
import org.meveo.apiv2.accounts.service.UserAccountsApiService;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.billing.UserAccount;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;

@Interceptors({ WsRestApiInterceptor.class })
public class UserAccountsV2ResourceImpl extends BaseRs implements UserAccountV2Resource {

    @Inject
    private UserAccountsApiService userAccountsApiService;
	
	@Inject
	private UserAccountApi userAccountApi;

	@Override
	public ActionStatus create(UserAccountDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			UserAccount userAccount = userAccountApi.create(postData, UserAccountApi.Version.V2);
			result.setEntityCode(userAccount.getCode());
			result.setEntityId(userAccount.getId());
		} catch (Exception e) {
			processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus update(UserAccountDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			UserAccount userAccount = userAccountApi.update(postData, UserAccountApi.Version.V2);
			result.setEntityCode(userAccount.getCode());
			result.setEntityId(userAccount.getId());
		} catch (Exception e) {
			processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus createOrUpdate(UserAccountDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			UserAccount userAccount = userAccountApi.createOrUpdate(postData, UserAccountApi.Version.V2);
			if (StringUtils.isBlank(postData.getCode())) {
				result.setEntityCode(userAccount.getCode());
			}
		} catch (Exception e) {
			processException(e, result);
		}

		return result;
	}

}
