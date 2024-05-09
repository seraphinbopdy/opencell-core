package org.meveo.service.crm.impl;

import org.meveo.admin.exception.BusinessException;

public class SubscriptionActivationException extends BusinessException {

    private String code;

    public SubscriptionActivationException(String message) {
        super(message);
    }

    public SubscriptionActivationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
