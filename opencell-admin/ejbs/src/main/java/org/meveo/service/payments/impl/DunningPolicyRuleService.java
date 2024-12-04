package org.meveo.service.payments.impl;

import java.util.List;

import org.meveo.model.dunning.DunningPolicyRule;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;

@Stateless
public class DunningPolicyRuleService extends PersistenceService<DunningPolicyRule> {

    public List<DunningPolicyRule> findByDunningPolicy(long dunningPolicyId) {
        return getEntityManager().createNamedQuery("DunningPolicyRule.findByDunningPolicyId")
                        .setParameter("policyId", dunningPolicyId)
                        .getResultList();
    }
}