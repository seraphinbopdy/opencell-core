package org.meveo.apiv2.accounts.impl;

import org.meveo.api.account.CustomerApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.account.CustomerDto;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.apiv2.accounts.resource.CustomerV2Resource;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.crm.Customer;

import javax.inject.Inject;

public class CustomerV2ResourceImpl extends BaseRs implements CustomerV2Resource {
    
    @Inject
    private CustomerApi customerApi;
    
    @Override
    public ActionStatus create(CustomerDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            Customer customer = customerApi.create(postData, CustomerApi.Version.V2);
            result.setEntityCode(customer.getCode());
            result.setEntityId(customer.getId());
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus update(CustomerDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            Customer customer = customerApi.update(postData, CustomerApi.Version.V2);
            result.setEntityId(customer.getId());
            result.setEntityCode(customer.getCode());
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus createOrUpdate(CustomerDto postData) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            Customer customer = customerApi.createOrUpdate(postData, CustomerApi.Version.V2);
            if (StringUtils.isBlank(postData.getCode())) {
                result.setEntityCode(customer.getCode());
            }
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }
    
}
