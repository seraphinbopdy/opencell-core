package org.meveo.service.payments.impl;

import java.util.List;

import org.meveo.model.payments.PaymentRejectionCodesGroup;
import org.meveo.service.base.BusinessService;

import jakarta.ejb.Stateless;

@Stateless
public class PaymentRejectionCodesGroupService extends BusinessService<PaymentRejectionCodesGroup> {

    public int remove(List<PaymentRejectionCodesGroup> rejectionCodesGroup) {
        rejectionCodesGroup.forEach(this::remove);
        return rejectionCodesGroup.size();
    }
}
