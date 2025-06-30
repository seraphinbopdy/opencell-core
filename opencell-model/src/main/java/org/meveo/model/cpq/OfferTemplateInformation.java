package org.meveo.model.cpq;

import org.meveo.model.billing.SubscriptionRenewal;

import java.io.Serializable;

public class OfferTemplateInformation implements Serializable, Cloneable {

    private Long id;
    private String code;
    private boolean disabled;
    private SubscriptionRenewal subscriptionRenewal;
    private boolean autoEndOfEngagement;

    public OfferTemplateInformation() {
    }

    public OfferTemplateInformation(Long id,
                                    String code,
                                    boolean disabled,
                                    SubscriptionRenewal subscriptionRenewal,
                                    boolean autoEndOfEngagement) {
        this.id = id;
        this.code = code;
        this.disabled = disabled;
        this.subscriptionRenewal = subscriptionRenewal;
        this.autoEndOfEngagement = autoEndOfEngagement;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public SubscriptionRenewal getSubscriptionRenewal() {
        return subscriptionRenewal;
    }

    public void setSubscriptionRenewal(SubscriptionRenewal subscriptionRenewal) {
        this.subscriptionRenewal = subscriptionRenewal;
    }

    public boolean isAutoEndOfEngagement() {
        return autoEndOfEngagement;
    }

    public void setAutoEndOfEngagement(boolean autoEndOfEngagement) {
        this.autoEndOfEngagement = autoEndOfEngagement;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public OfferTemplateInformation clone() {
        return new OfferTemplateInformation(id, code, disabled, subscriptionRenewal, autoEndOfEngagement);
    }
}
