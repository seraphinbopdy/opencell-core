package org.meveo.service.payments.impl;

import org.meveo.model.payments.PaymentRejectionCodesGroup;
import org.meveo.service.base.BusinessService;

import javax.ejb.Stateless;
import java.util.List;

@Stateless
public class PaymentRejectionCodesGroupService extends BusinessService<PaymentRejectionCodesGroup> {

    public int remove(List<PaymentRejectionCodesGroup> rejectionCodesGroup) {
        rejectionCodesGroup.forEach(this::remove);
        return rejectionCodesGroup.size();
    }
}
