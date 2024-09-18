package org.meveo.service.payments.impl;

import java.util.List;

import org.meveo.model.dunning.DunningPolicyLevel;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;

@Stateless
public class DunningPolicyLevelService extends PersistenceService<DunningPolicyLevel> {

    public List<DunningPolicyLevel> findByPolicyID(Long policyID) {
        return getEntityManager().createNamedQuery("DunningPolicyLevel.findDunningPolicyLevels", DunningPolicyLevel.class)
                .setParameter("policyId", policyID)
                .getResultList();
    }
}
