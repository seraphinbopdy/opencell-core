package org.meveo.service.payments.impl;

public class RejectionCodeDeleteException extends RuntimeException {

    private String code;
    private String message;

    public RejectionCodeDeleteException(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public RejectionCodeDeleteException(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
