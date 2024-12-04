package org.meveo.apiv2.accounts.impl;

import org.meveo.api.account.CustomerAccountApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.account.CustomerAccountDto;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.apiv2.accounts.resource.CustomerAccountV2Resource;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.payments.CustomerAccount;

import jakarta.inject.Inject;

public class CustomerAccountV2ResourceImpl extends BaseRs implements CustomerAccountV2Resource {
    
    @Inject
    private CustomerAccountApi customerAccountApi;
    
    @Override
    public ActionStatus create(CustomerAccountDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            CustomerAccount customerAccount = customerAccountApi.create(postData, CustomerAccountApi.Version.V2);
            result.setEntityCode(customerAccount.getCode());
            result.setEntityId(customerAccount.getId());
        } catch (Exception exception) {
            processException(exception, result);
        }

        return result;
    }

    @Override
    public ActionStatus update(CustomerAccountDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            CustomerAccount customerAccount = customerAccountApi.update(postData, CustomerAccountApi.Version.V2);
            result.setEntityCode(customerAccount.getCode());
            result.setEntityId(customerAccount.getId());
        } catch (Exception exception) {
            processException(exception, result);
        }

        return result;
    }

    @Override
    public ActionStatus createOrUpdate(CustomerAccountDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            CustomerAccount customerAccount = customerAccountApi.createOrUpdate(postData, CustomerAccountApi.Version.V2);
            if (StringUtils.isBlank(postData.getCode())) {
                result.setEntityCode(customerAccount.getCode());
            }
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }
    
}
