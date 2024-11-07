package org.meveo.service.payments.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.dunning.DunningAction;
import org.meveo.model.dunning.DunningActionInstance;
import org.meveo.model.dunning.DunningActionInstanceStatusEnum;
import org.meveo.model.dunning.DunningCollectionPlan;
import org.meveo.model.dunning.DunningLevelInstance;
import org.meveo.model.dunning.DunningLevelInstanceStatusEnum;
import org.meveo.model.dunning.DunningPolicyLevel;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

@Stateless
public class DunningActionInstanceService extends PersistenceService<DunningActionInstance> {
	
    /**
     * Find a dunning action instance by code and dunning level instance
     *
     * @param code                Code
     * @param dunningLevelInstance Level instance
     * @return A dunning action instance
     */
	public DunningActionInstance findByCodeAndDunningLevelInstance(String code, Long dunningLevelInstance) {
		QueryBuilder qb = new QueryBuilder(DunningActionInstance.class, "d", Arrays.asList("dunningLevelInstance"));
		qb.addCriterion("d.code", "=", code, true);
		qb.addCriterion("d.dunningLevelInstance.id", "=", dunningLevelInstance, false);

        try {
            return (DunningActionInstance) qb.getQuery(getEntityManager()).getSingleResult();
        } catch (NoResultException exception) {
            return null;
        }
	}

    /**
     * Update the status of a dunning action instance
     *
     * @param actionStatus         Action status
     * @param dunningLevelInstance Level instance
     * @return The number of updated records
     */
	public int updateStatus(DunningActionInstanceStatusEnum actionStatus, DunningLevelInstance dunningLevelInstance) {
        return getEntityManager()
                .createNamedQuery("DunningActionInstance.updateStatus")
                .setParameter("actionStatus", actionStatus)
                .setParameter("dunningLevelInstance", dunningLevelInstance)
                .executeUpdate();
    }

    /**
     * Create a list of action instances
     *
     * @param pDunningPolicyLevel   Policy level
     * @param pDunningLevelInstance Level instance
     * @return A list of action instances
     */
    public List<DunningActionInstance> createDunningActionInstances(DunningCollectionPlan collectionPlan, DunningPolicyLevel pDunningPolicyLevel, DunningLevelInstance pDunningLevelInstance) {
        List<DunningActionInstance> actionInstances = new ArrayList<>();

        for (DunningAction action : pDunningPolicyLevel.getDunningLevel().getDunningActions()) {
            DunningActionInstance dunningActionInstance = createDunningActionInstance(action, pDunningLevelInstance);
            dunningActionInstance.setDunningLevelInstance(pDunningLevelInstance);
            dunningActionInstance.setCode(action.getCode());
            dunningActionInstance.setDescription(action.getDescription());
            dunningActionInstance.setCfValues(action.getCfValues());

            if(collectionPlan != null) {
                dunningActionInstance.setCollectionPlan(collectionPlan);
}

            this.create(dunningActionInstance);
            actionInstances.add(dunningActionInstance);
        }

        return actionInstances;
    }

    /**
     * Create a dunning action instance
     *
     * @param action               Dunning action
     * @param pDunningLevelInstance Level instance
     * @return A dunning action instance
     */
    public DunningActionInstance createDunningActionInstance(DunningAction action, DunningLevelInstance pDunningLevelInstance) {
        DunningActionInstance dunningActionInstance = new DunningActionInstance();
        dunningActionInstance.setDunningAction(action);
        dunningActionInstance.setActionType(action.getActionType());
        dunningActionInstance.setActionMode(action.getActionMode());
        dunningActionInstance.setActionOwner(action.getAssignedTo());

        if (pDunningLevelInstance.getLevelStatus() == DunningLevelInstanceStatusEnum.DONE) {
            dunningActionInstance.setActionStatus(DunningActionInstanceStatusEnum.DONE);
        } else {
            dunningActionInstance.setActionStatus(DunningActionInstanceStatusEnum.TO_BE_DONE);
        }

        return dunningActionInstance;
    }
}
