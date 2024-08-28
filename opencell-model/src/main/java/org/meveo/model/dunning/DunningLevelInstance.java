package org.meveo.model.dunning;

import static javax.persistence.FetchType.LAZY;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;
import org.meveo.model.billing.Invoice;
import org.meveo.model.payments.CustomerAccount;

@Entity
@Table(name = "dunning_level_instance")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "dunning_level_instance_seq") })
@NamedQueries({
        @NamedQuery(name = "DunningLevelInstance.findByCollectionPlan", query = "SELECT li FROM DunningLevelInstance li where li.collectionPlan = :collectionPlan order by li.daysOverdue"),
        @NamedQuery(name = "DunningLevelInstance.findByLevelId", query = "SELECT li FROM DunningLevelInstance li where li.dunningLevel.id = :levelId AND li.collectionPlan is NULL"),
        @NamedQuery(name = "DunningLevelInstance.findBySequence", query = "SELECT li FROM DunningLevelInstance li LEFT JOIN FETCH li.actions where li.collectionPlan = :collectionPlan and li.sequence = :sequence"),
        @NamedQuery(name = "DunningLevelInstance.findLastLevelInstance", query = "SELECT li FROM DunningLevelInstance li LEFT JOIN li.dunningLevel d where li.collectionPlan = :collectionPlan and d.isEndOfDunningLevel = true"),
        @NamedQuery(name = "DunningLevelInstance.checkDaysOverdueIsAlreadyExist", query = "SELECT count(li) FROM DunningLevelInstance li where li.collectionPlan = :collectionPlan and li.daysOverdue = :daysOverdue"),
        @NamedQuery(name = "DunningLevelInstance.minSequenceByDaysOverdue", query = "SELECT min(li.sequence) FROM DunningLevelInstance li where li.collectionPlan = :collectionPlan and li.daysOverdue > :daysOverdue"),
        @NamedQuery(name = "DunningLevelInstance.findByInvoiceAndEmptyCollectionPlan", query = "SELECT li FROM DunningLevelInstance li where li.invoice = :invoice and li.collectionPlan is NULL"),
        @NamedQuery(name = "DunningLevelInstance.findByCustomerAccountAndEmptyCollectionPlan", query = "SELECT li FROM DunningLevelInstance li where li.customerAccount = :customerAccount and li.collectionPlan is NULL"),
        @NamedQuery(name = "DunningLevelInstance.findByInvoice", query = "SELECT li FROM DunningLevelInstance li where li.invoice = :invoice"),
        @NamedQuery(name = "DunningLevelInstance.findByCustomerAccount", query = "SELECT li FROM DunningLevelInstance li where li.customerAccount = :customerAccount and li.collectionPlan is NULL"),
        @NamedQuery(name = "DunningLevelInstance.incrementSequencesByDaysOverdue", query = "UPDATE DunningLevelInstance li set li.sequence = li.sequence+1 where li.collectionPlan = :collectionPlan and li.daysOverdue > :daysOverdue"),
        @NamedQuery(name = "DunningLevelInstance.decrementSequencesByDaysOverdue", query = "UPDATE DunningLevelInstance li set li.sequence = li.sequence-1 where li.collectionPlan = :collectionPlan and li.daysOverdue > :daysOverdue") })
public class DunningLevelInstance extends AuditableEntity {

    private static final long serialVersionUID = -5809793412586160209L;

    @Column(name = "sequence")
    @NotNull
    private Integer sequence;

    @Column(name = "days_overdue")
    @NotNull
    private Integer daysOverdue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dunning_collection_plan_id")
    private DunningCollectionPlan collectionPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dunning_collection_plan_status_id")
    private DunningCollectionPlanStatus collectionPlanStatus;

    @OneToMany(mappedBy = "dunningLevelInstance", fetch = LAZY)
    private List<DunningActionInstance> actions;

    @Column(name = "level_status")
    @Enumerated(EnumType.STRING)
    @NotNull
    private DunningLevelInstanceStatusEnum levelStatus = DunningLevelInstanceStatusEnum.TO_BE_DONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dunning_level_id")
    @NotNull
    private DunningLevel dunningLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id")
    private CustomerAccount customerAccount;

    public DunningLevelInstance() {
        super();
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(Integer daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public DunningCollectionPlan getCollectionPlan() {
        return collectionPlan;
    }

    public void setCollectionPlan(DunningCollectionPlan collectionPlan) {
        this.collectionPlan = collectionPlan;
    }

    public DunningCollectionPlanStatus getCollectionPlanStatus() {
        return collectionPlanStatus;
    }

    public void setCollectionPlanStatus(DunningCollectionPlanStatus collectionPlanStatus) {
        this.collectionPlanStatus = collectionPlanStatus;
    }

    public List<DunningActionInstance> getActions() {
        return actions;
    }

    public void setActions(List<DunningActionInstance> actions) {
        this.actions = actions;
    }

    public DunningLevelInstanceStatusEnum getLevelStatus() {
        return levelStatus;
    }

    public void setLevelStatus(DunningLevelInstanceStatusEnum levelStatus) {
        this.levelStatus = levelStatus;
    }

    public DunningLevel getDunningLevel() {
        return dunningLevel;
    }

    public void setDunningLevel(DunningLevel dunningLevel) {
        this.dunningLevel = dunningLevel;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public DunningLevelInstance setInvoice(Invoice invoice) {
        this.invoice = invoice;
        return this;
    }

    public CustomerAccount getCustomerAccount() {
        return customerAccount;
    }

    public DunningLevelInstance setCustomerAccount(CustomerAccount customerAccount) {
        this.customerAccount = customerAccount;
        return this;
    }
}
