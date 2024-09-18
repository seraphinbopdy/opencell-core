package org.meveo.model.catalog;

import java.util.List;

import org.meveo.model.EnableBusinessCFEntity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@Cacheable
public abstract class ServiceCharge extends EnableBusinessCFEntity {

    private static final long serialVersionUID = 8209018747977179548L;

    public abstract List<ServiceChargeTemplateRecurring> getServiceRecurringCharges();

    public abstract List<ServiceChargeTemplateSubscription> getServiceSubscriptionCharges();

    public abstract List<ServiceChargeTemplateTermination> getServiceTerminationCharges();

    public abstract List<ServiceChargeTemplateUsage> getServiceUsageCharges();

    public ServiceChargeTemplateRecurring getServiceRecurringChargeByChargeCode(String chargeCode) {
        ServiceChargeTemplateRecurring result = null;
        for (ServiceChargeTemplateRecurring sctr : getServiceRecurringCharges()) {
            if (sctr.getChargeTemplate().getCode().equals(chargeCode)) {
                result = sctr;
                break;
            }
        }
        return result;
    }

    public ServiceChargeTemplateSubscription getServiceChargeTemplateSubscriptionByChargeCode(String chargeCode) {
        ServiceChargeTemplateSubscription result = null;
        for (ServiceChargeTemplateSubscription sctr : getServiceSubscriptionCharges()) {
            if (sctr.getChargeTemplate().getCode().equals(chargeCode)) {
                result = sctr;
                break;
            }
        }
        return result;
    }

    public ServiceChargeTemplateTermination getServiceChargeTemplateTerminationByChargeCode(String chargeCode) {
        ServiceChargeTemplateTermination result = null;
        for (ServiceChargeTemplateTermination sctr : getServiceTerminationCharges()) {
            if (sctr.getChargeTemplate().getCode().equals(chargeCode)) {
                result = sctr;
                break;
            }
        }
        return result;
    }
}