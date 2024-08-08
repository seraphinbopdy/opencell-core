package org.meveo.apiv2.accounts.impl;

import org.meveo.api.account.AccountHierarchyApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.account.CustomerHierarchyDto;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.apiv2.accounts.resource.AccountHierarchyV2Resource;

import javax.inject.Inject;

public class AccountHierarchyV2ResourceImpl extends BaseRs implements AccountHierarchyV2Resource {
    
    @Inject
    private AccountHierarchyApi accountHierarchyApi;

    public ActionStatus customerHierarchyUpdate(CustomerHierarchyDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            accountHierarchyApi.customerHierarchyUpdate(postData, AccountHierarchyApi.Version.V2);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }
}
