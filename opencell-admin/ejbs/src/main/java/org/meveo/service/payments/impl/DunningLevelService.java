package org.meveo.service.payments.impl;

import javax.ejb.Stateless;

import org.meveo.model.dunning.DunningLevel;
import org.meveo.model.dunning.DunningModeEnum;
import org.meveo.service.base.BusinessService;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation to manage DunningLevel entity. It extends {@link BusinessService} class
 * 
 * @author YIZEM
 * @version 12.0
 *
 */
@Stateless
public class DunningLevelService extends BusinessService<DunningLevel> {

    /**
     * Update dunning levels by setting active = true or false according to the selected dunning settings (INVOICE_LEVEL or CUSTOMER_LEVEL)
     * @param pDunningMode {@link DunningModeEnum}
     */
    public void updateDunningLevelAfterCreatingOrUpdatingDunningSetting(DunningModeEnum pDunningMode) {
        getEntityManager().createNamedQuery("DunningLevel.activateByDunningMode").setParameter("dunningMode", pDunningMode).executeUpdate();
        getEntityManager().createNamedQuery("DunningLevel.deactivateByDunningMode").setParameter("dunningMode", pDunningMode).executeUpdate();
    }

    /**
     * Get Reminder Dunning Level
     * @param dunningPolicyLevelIds Dunning policy level ids
     * @return Reminder Dunning Level
     */
    public Optional<DunningLevel> getReminderDunningLevel(List<Long> dunningPolicyLevelIds) {
        return dunningPolicyLevelIds.stream()
                .map(this::findById)
                .filter(dunningLevel -> dunningLevel != null && dunningLevel.isReminder())
                .findFirst();
    }
}
