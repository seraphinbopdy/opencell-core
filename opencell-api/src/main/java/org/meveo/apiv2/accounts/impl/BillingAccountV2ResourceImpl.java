package org.meveo.apiv2.accounts.impl;

import org.meveo.api.account.BillingAccountApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.account.BillingAccountDto;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.apiv2.accounts.resource.BillingAccountV2Resource;
import org.meveo.model.billing.BillingAccount;

import javax.inject.Inject;

public class BillingAccountV2ResourceImpl extends BaseRs implements BillingAccountV2Resource {
    
    @Inject
    private BillingAccountApi billingAccountApi;
    
    public ActionStatus create(BillingAccountDto postData) {

        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            BillingAccount billingAccount = billingAccountApi.create(postData, BillingAccountApi.Version.V2);
            result.setEntityCode(billingAccount.getCode());
            result.setEntityId(billingAccount.getId());
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

}
