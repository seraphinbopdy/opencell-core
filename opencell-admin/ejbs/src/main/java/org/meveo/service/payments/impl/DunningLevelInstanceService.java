package org.meveo.service.payments.impl;

import static org.meveo.model.dunning.DunningLevelInstanceStatusEnum.DONE;
import static org.meveo.model.shared.DateUtils.addDaysToDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.meveo.model.billing.Invoice;
import org.meveo.model.dunning.DunningActionInstanceStatusEnum;
import org.meveo.model.dunning.DunningCollectionPlan;
import org.meveo.model.dunning.DunningCollectionPlanStatus;
import org.meveo.model.dunning.DunningLevel;
import org.meveo.model.dunning.DunningLevelInstance;
import org.meveo.model.dunning.DunningLevelInstanceStatusEnum;
import org.meveo.model.dunning.DunningPolicy;
import org.meveo.model.dunning.DunningPolicyLevel;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.PersistenceService;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class DunningLevelInstanceService extends PersistenceService<DunningLevelInstance> {

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    @Inject
    private DunningActionInstanceService dunningActionInstanceService;

    @Inject
    private DunningLevelService dunningLevelService;

    public List<DunningLevelInstance> findByCollectionPlan(DunningCollectionPlan collectionPlan) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findByCollectionPlan", entityClass)
                    .setParameter("collectionPlan", collectionPlan)
                    .getResultList();
        } catch (Exception exception) {
            return null;
        }
    }

    public DunningLevelInstance findByLevelId(Long levelId) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findByLevelId", entityClass)
                    .setParameter("levelId", levelId)
                    .getSingleResult();
        } catch (Exception exception) {
            return null;
        }
    }

    public DunningLevelInstance findByCurrentLevelSequence(DunningCollectionPlan collectionPlan) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findBySequence", entityClass)
                    .setParameter("collectionPlan", collectionPlan)
                    .setParameter("sequence", collectionPlan.getCurrentDunningLevelSequence())
                    .getSingleResult();
        } catch (Exception exception) {
            return null;
        }
    }
    
    public DunningLevelInstance findBySequence(DunningCollectionPlan collectionPlan, Integer sequence) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findBySequence", entityClass)
                    .setParameter("collectionPlan", collectionPlan)
                    .setParameter("sequence", sequence)
                    .getSingleResult();
        } catch (Exception exception) {
            return null;
        }
    }

    public DunningLevelInstance findLastLevelInstance(DunningCollectionPlan collectionPlan) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findLastLevelInstance", entityClass)
                    .setParameter("collectionPlan", collectionPlan)
                    .getSingleResult();
        } catch (Exception exception) {
            return null;
        }
    }

    public boolean checkDaysOverdueIsAlreadyExist(DunningCollectionPlan collectionPlan, Integer daysOverdue) {
        return getEntityManager()
                .createNamedQuery("DunningLevelInstance.checkDaysOverdueIsAlreadyExist", Long.class)
                .setParameter("collectionPlan", collectionPlan)
                .setParameter("daysOverdue", daysOverdue)
                .getSingleResult() > 0;
    }

    public Integer getMinSequenceByDaysOverdue(DunningCollectionPlan collectionPlan, Integer daysOverdue) {
        return getEntityManager()
                .createNamedQuery("DunningLevelInstance.minSequenceByDaysOverdue", Integer.class)
                .setParameter("collectionPlan", collectionPlan)
                .setParameter("daysOverdue", daysOverdue)
                .getSingleResult();
    }

    public void incrementSequecesGreaterThanDaysOverdue(DunningCollectionPlan collectionPlan, Integer daysOverdue) {
        getEntityManager()
            .createNamedQuery("DunningLevelInstance.incrementSequencesByDaysOverdue")
            .setParameter("collectionPlan", collectionPlan)
            .setParameter("daysOverdue", daysOverdue)
            .executeUpdate();
    }

    public void decrementSequecesGreaterThanDaysOverdue(DunningCollectionPlan collectionPlan, Integer daysOverdue) {
        getEntityManager()
            .createNamedQuery("DunningLevelInstance.decrementSequencesByDaysOverdue")
            .setParameter("collectionPlan", collectionPlan)
            .setParameter("daysOverdue", daysOverdue)
            .executeUpdate();
    }

    /**
     * Find dunning level instance by invoice when collection plan is null.
     *
     * @param pInvoice the invoice
     * @return the list of DunningLevelInstance
     */
    public List<DunningLevelInstance> findByInvoiceAndEmptyCollectionPlan(Invoice pInvoice) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findByInvoiceAndEmptyCollectionPlan", entityClass)
                    .setParameter("invoice", pInvoice)
                    .getResultList();
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Find dunning level instance by invoice.
     *
     * @param pInvoice the invoice
     * @return the list of DunningLevelInstance
     */
    public List<DunningLevelInstance> findByInvoice(Invoice pInvoice) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findByInvoice", entityClass)
                    .setParameter("invoice", pInvoice)
                    .getResultList();
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Create a level instance
     *
     * @param pInvoice           Invoice
     * @param pCustomerAccount   Customer account
     * @param pDunningPolicyLevel Policy level
     * @return A new level instance
     */
    public DunningLevelInstance createDunningLevelInstanceWithoutCollectionPlan(CustomerAccount pCustomerAccount, Invoice pInvoice, DunningPolicyLevel pDunningPolicyLevel, DunningLevelInstanceStatusEnum status) {
        DunningLevelInstance levelInstance = new DunningLevelInstance();
        levelInstance.setLevelStatus(status);
        levelInstance.setExecutionDate(new Date());
        levelInstance.setSequence(pDunningPolicyLevel.getSequence());
        levelInstance.setDunningLevel(pDunningPolicyLevel.getDunningLevel());
        levelInstance.setDaysOverdue(pDunningPolicyLevel.getDunningLevel().getDaysOverdue());
        levelInstance.setInvoice(pInvoice);
        levelInstance.setCustomerAccount(pCustomerAccount);
        levelInstance.setCfValues(pDunningPolicyLevel.getDunningLevel().getCFValuesCopy());
        this.create(levelInstance);

        if (pDunningPolicyLevel.getDunningLevel().getDunningActions() != null && !pDunningPolicyLevel.getDunningLevel().getDunningActions().isEmpty()) {
            levelInstance.setActions(dunningActionInstanceService.createDunningActionInstances(null, pDunningPolicyLevel, levelInstance));
            this.update(levelInstance);
}

        if (levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.DONE) || levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.IN_PROGRESS)) {
            levelInstance.setExecutionDate(new Date());
        } else if (levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.IGNORED)) {
            levelInstance.setExecutionDate(null);
        }

        return levelInstance;
    }

    /**
     * Create a level instance
     * @param collectionPlan The Collection Plan
     * @param collectionPlanStatus The Collection Plan Status
     * @param policyLevel The Dunning Policy Level
     * @param status The Dunning Level Instance Status
     * @return created DunningLevelInstance
     */
    public DunningLevelInstance createDunningLevelInstanceWithCollectionPlan(DunningCollectionPlan collectionPlan, DunningCollectionPlanStatus collectionPlanStatus, DunningPolicyLevel policyLevel, DunningLevelInstanceStatusEnum status) {
        DunningLevelInstance levelInstance = new DunningLevelInstance();
        levelInstance.setCollectionPlan(collectionPlan);
        levelInstance.setCollectionPlanStatus(collectionPlanStatus);
        levelInstance.setLevelStatus(status);
        levelInstance.setSequence(policyLevel.getSequence());
        levelInstance.setDunningLevel(policyLevel.getDunningLevel());
        levelInstance.setCfValues(policyLevel.getDunningLevel().getCFValuesCopy());
        levelInstance.setDaysOverdue(policyLevel.getDunningLevel().getDaysOverdue());
        levelInstance.setExecutionDate(addDaysToDate(collectionPlan.getStartDate(), policyLevel.getDunningLevel().getDaysOverdue()));

        // Check the related invoice and set it to the level instance
        if (collectionPlan.getRelatedInvoice() != null) {
            levelInstance.setInvoice(collectionPlan.getRelatedInvoice());
        }

        // Check the related customer account and set it to the level instance
        if (collectionPlan.getCustomerAccount() != null) {
            levelInstance.setCustomerAccount(collectionPlan.getCustomerAccount());
        } else if (collectionPlan.getBillingAccount() != null && collectionPlan.getBillingAccount().getCustomerAccount() != null) {
            levelInstance.setCustomerAccount(collectionPlan.getBillingAccount().getCustomerAccount());
        }

        if (levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.DONE) || levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.IN_PROGRESS)) {
            levelInstance.setExecutionDate(new Date());
        } else if (levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.IGNORED)) {
            levelInstance.setExecutionDate(null);
        }

        this.create(levelInstance);

        if (policyLevel.getDunningLevel().getDunningActions() != null
                && !policyLevel.getDunningLevel().getDunningActions().isEmpty()) {
            levelInstance.setActions(dunningActionInstanceService.createDunningActionInstances(collectionPlan, policyLevel, levelInstance));
            this.update(levelInstance);
        }

        return levelInstance;
    }

    /**
     * Create a level instance
     *
     * @param pInvoice           Invoice
     * @param pCustomerAccount   Customer account
     * @param pDunningPolicyLevel Policy level
     * @return A new level instance
     */
    public DunningLevelInstance createIgnoredDunningLevelInstance(CustomerAccount pCustomerAccount, Invoice pInvoice, DunningPolicyLevel pDunningPolicyLevel) {
        // Check if a dunning level instance already exists for the invoice
        List<DunningLevelInstance> dunningLevelInstances = this.findByInvoice(pInvoice);
        if (dunningLevelInstances != null && !dunningLevelInstances.isEmpty()) {
            // Check if we have already processed the invoice for the current level
            for (DunningLevelInstance dunningLevelInstance : dunningLevelInstances) {
                if (dunningLevelInstance.getDunningLevel().getId().equals(pDunningPolicyLevel.getDunningLevel().getId())) {
                    return dunningLevelInstance;
                }
            }
        }

        // Create a new level instance
        DunningLevelInstance dunningLevelInstanceWithoutCollectionPlan = createDunningLevelInstanceWithoutCollectionPlan(pCustomerAccount, pInvoice, pDunningPolicyLevel, DunningLevelInstanceStatusEnum.IGNORED);
        dunningLevelInstanceWithoutCollectionPlan.getActions().forEach(action -> {
            action.setActionStatus(DunningActionInstanceStatusEnum.IGNORED);
            action.setExecutionDate(null);
            dunningActionInstanceService.update(action);
        });
        return dunningLevelInstanceWithoutCollectionPlan;
    }

    /**
     * Create dunning level instances
     * @param policy The Dunning Policy
     * @param collectionPlan The Collection Plan
     * @param collectionPlanStatus The Collection Plan Status
     * @return List of DunningLevelInstance
     */
    public List<DunningLevelInstance> createDunningLevelInstancesWithCollectionPlan(CustomerAccount customerAccount, Invoice pInvoice, DunningPolicy policy,
                                                                                    DunningCollectionPlan collectionPlan, DunningCollectionPlanStatus collectionPlanStatus) {
        Date today = new Date();
        List<DunningLevelInstance> levelInstances = new ArrayList<>();

        for (DunningPolicyLevel policyLevel : policy.getDunningLevels()) {
            // Check if a dunning level instance already exists for this invoice - Case when Reminder is already triggered
            List<DunningLevelInstance> dunningLevelInstances = this.findByInvoice(collectionPlan.getRelatedInvoice());
            Optional<DunningLevelInstance> foundDunningLevelInstance = dunningLevelInstances.stream()
                    .filter(dunningLevelInstance -> dunningLevelInstance.getDunningLevel().getId().equals(policyLevel.getDunningLevel().getId()))
                    .findFirst();

            // If the level is not already triggered, we create a new level instance
            if (foundDunningLevelInstance.isEmpty()) {
                // Check if the current dunning level is reminder level
                DunningLevel reminderLevel = dunningLevelService.findById(policyLevel.getDunningLevel().getId(), List.of("dunningActions"));

                if (reminderLevel != null && reminderLevel.isReminder()) {
                    // If the current level is reminder level, we check if the due date of the invoice is equal to the reminder level days overdue
                    Date dateToCompare = DateUtils.addDaysToDate(pInvoice.getDueDate(), reminderLevel.getDaysOverdue());

                    if (simpleDateFormat.format(dateToCompare).equals(simpleDateFormat.format(today)) && !pInvoice.isReminderLevelTriggered()) {
                        DunningLevelInstance levelInstance = createDunningLevelInstanceWithCollectionPlan(collectionPlan, collectionPlanStatus, policyLevel, DunningLevelInstanceStatusEnum.TO_BE_DONE);
                        levelInstances.add(levelInstance);
                    } else {
                        DunningLevelInstance ignoredDunningLevelInstance = this.createIgnoredDunningLevelInstance(customerAccount, pInvoice, policyLevel);
                        ignoredDunningLevelInstance.setCollectionPlan(collectionPlan);
                        levelInstances.add(ignoredDunningLevelInstance);
                    }
                } else {
                    DunningLevelInstance dunningLevelInstanceWithCollectionPlan = this.createDunningLevelInstanceWithCollectionPlan(collectionPlan, collectionPlanStatus, policyLevel, DunningLevelInstanceStatusEnum.TO_BE_DONE);
                    levelInstances.add(dunningLevelInstanceWithCollectionPlan);
                }
            } else {
                levelInstances.add(foundDunningLevelInstance.get());
            }
        }

        return levelInstances;
    }

    /**
     * Find dunning level instance by customer account when collection plan is null.
     * @param pCustomerAccount the customer account
     * @return the list of DunningLevelInstance
     */
    public List<DunningLevelInstance> findByCustomerAccountAndEmptyCollectionPlan(CustomerAccount pCustomerAccount) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findByCustomerAccountAndEmptyCollectionPlan", entityClass)
                    .setParameter("customerAccount", pCustomerAccount)
                    .getResultList();
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    /**
     * Create dunning level instances
     * @param policy The Dunning Policy
     * @param collectionPlan The Collection Plan
     * @param collectionPlanStatus The Collection Plan Status
     * @return List of DunningLevelInstance
     */
    public List<DunningLevelInstance> createDunningLevelInstancesWithCollectionPlanForCustomerLevel(CustomerAccount customerAccount, DunningPolicy policy,
                                                                                    DunningCollectionPlan collectionPlan, DunningCollectionPlanStatus collectionPlanStatus) {
        Date today = new Date();
        List<DunningLevelInstance> levelInstances = new ArrayList<>();

        for (DunningPolicyLevel policyLevel : policy.getDunningLevels()) {
            // Check if a dunning level instance already exists for this invoice - Case when Reminder is already triggered
            List<DunningLevelInstance> dunningLevelInstances = this.findByCustomerAccount(customerAccount);
            Optional<DunningLevelInstance> foundDunningLevelInstance = dunningLevelInstances.stream()
                    .filter(dunningLevelInstance -> dunningLevelInstance.getDunningLevel().getId().equals(policyLevel.getDunningLevel().getId()))
                    .findFirst();

            // If the level is not already triggered, we create a new level instance
            if (foundDunningLevelInstance.isEmpty()) {
                // Check if the current dunning level is reminder level
                DunningLevel reminderLevel = dunningLevelService.findById(policyLevel.getDunningLevel().getId(), List.of("dunningActions"));

                if (reminderLevel != null && reminderLevel.isReminder()) {
                    // If the current level is reminder level, we check if the due date of the invoice is equal to the reminder level days overdue
                    Date dateToCompare = DateUtils.addDaysToDate(collectionPlan.getStartDate(), reminderLevel.getDaysOverdue());

                    if (simpleDateFormat.format(dateToCompare).equals(simpleDateFormat.format(today))) {
                        DunningLevelInstance levelInstance = createDunningLevelInstanceWithCollectionPlan(collectionPlan, collectionPlanStatus, policyLevel, DunningLevelInstanceStatusEnum.TO_BE_DONE);
                        levelInstances.add(levelInstance);
                    } else {
                        DunningLevelInstance ignoredDunningLevelInstance = this.createIgnoredDunningLevelInstance(customerAccount, policyLevel);
                        ignoredDunningLevelInstance.setCollectionPlan(collectionPlan);
                        levelInstances.add(ignoredDunningLevelInstance);
                    }
                } else {
                    DunningLevelInstance dunningLevelInstanceWithCollectionPlan = this.createDunningLevelInstanceWithCollectionPlan(collectionPlan, collectionPlanStatus, policyLevel, DunningLevelInstanceStatusEnum.TO_BE_DONE);
                    levelInstances.add(dunningLevelInstanceWithCollectionPlan);
                }
            } else {
                levelInstances.add(foundDunningLevelInstance.get());
            }
        }

        return levelInstances;
    }

    /**
     * Create a level instance
     *
     * @param pCustomerAccount   Customer account
     * @param pDunningPolicyLevel Policy level
     * @return A new level instance
     */
    public DunningLevelInstance createIgnoredDunningLevelInstance(CustomerAccount pCustomerAccount, DunningPolicyLevel pDunningPolicyLevel) {
        // Check if a dunning level instance already exists for the invoice
        List<DunningLevelInstance> dunningLevelInstances = this.findByCustomerAccount(pCustomerAccount);
        if (dunningLevelInstances != null && !dunningLevelInstances.isEmpty()) {
            // Check if we have already processed the invoice for the current level
            for (DunningLevelInstance dunningLevelInstance : dunningLevelInstances) {
                if (dunningLevelInstance.getDunningLevel().getId().equals(pDunningPolicyLevel.getDunningLevel().getId())) {
                    return dunningLevelInstance;
                }
            }
        }

        // Create a new level instance
        DunningLevelInstance dunningLevelInstanceWithoutCollectionPlan = createDunningLevelInstanceWithoutCollectionPlanForCustomerLevel(pCustomerAccount, pDunningPolicyLevel, DunningLevelInstanceStatusEnum.IGNORED);
        dunningLevelInstanceWithoutCollectionPlan.getActions().forEach(action -> {
            action.setActionStatus(DunningActionInstanceStatusEnum.IGNORED);
            action.setExecutionDate(null);
            dunningActionInstanceService.update(action);
        });
        return dunningLevelInstanceWithoutCollectionPlan;
    }

    /**
     * Create dunning level instances
     * @param pCustomerAccount The Customer Account
     * @return List of DunningLevelInstance
     */
    public List<DunningLevelInstance> findByCustomerAccount(CustomerAccount pCustomerAccount) {
        try {
            return getEntityManager()
                    .createNamedQuery("DunningLevelInstance.findByCustomerAccount", entityClass)
                    .setParameter("customerAccount", pCustomerAccount)
                    .getResultList();
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Create a level instance
     *
     * @param pCustomerAccount   Customer account
     * @param pDunningPolicyLevel Policy level
     * @return A new level instance
     */
    public DunningLevelInstance createDunningLevelInstanceWithoutCollectionPlanForCustomerLevel(CustomerAccount pCustomerAccount, DunningPolicyLevel pDunningPolicyLevel, DunningLevelInstanceStatusEnum status) {
        DunningLevelInstance levelInstance = new DunningLevelInstance();
        levelInstance.setLevelStatus(status);
        levelInstance.setSequence(pDunningPolicyLevel.getSequence());
        levelInstance.setDunningLevel(pDunningPolicyLevel.getDunningLevel());
        levelInstance.setDaysOverdue(pDunningPolicyLevel.getDunningLevel().getDaysOverdue());
        levelInstance.setCustomerAccount(pCustomerAccount);
        this.create(levelInstance);

        if (pDunningPolicyLevel.getDunningLevel().getDunningActions() != null && !pDunningPolicyLevel.getDunningLevel().getDunningActions().isEmpty()) {
            levelInstance.setActions(dunningActionInstanceService.createDunningActionInstances(null, pDunningPolicyLevel, levelInstance));
            this.update(levelInstance);
        }

        if (levelInstance.getLevelStatus().equals(DONE) || levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.IN_PROGRESS)) {
            levelInstance.setExecutionDate(new Date());
        } else if (levelInstance.getLevelStatus().equals(DunningLevelInstanceStatusEnum.IGNORED)) {
            levelInstance.setExecutionDate(null);
        }

        return levelInstance;
    }
}
