package org.meveo.model.payments;

import java.util.List;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.NumericBooleanConverter;
import org.meveo.model.BusinessEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "ar_customer_balance")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "ar_customer_balance_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "CustomerBalance.findDefaultOne", query = "select c from CustomerBalance c where c.defaultBalance = :default"),
        @NamedQuery(name = "CustomerBalance.findDefaultCustomerBalance", query = "SELECT cb FROM CustomerBalance cb WHERE cb.defaultBalance = true") })
public class CustomerBalance extends BusinessEntity {

    /** */
    private static final long serialVersionUID = 1L;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "default_balance")
    private boolean defaultBalance;

    /**
     * An expression to decide whether the balance should be applied or not.
     */
    @Column(name = "balance_el", length = 2000)
    @Size(max = 2000)
    private String balanceEl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ar_customer_balance_templates", joinColumns = @JoinColumn(name = "customer_balance_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "template_id", referencedColumnName = "id"))
    private List<OCCTemplate> occTemplates;

    public boolean isDefaultBalance() {
        return defaultBalance;
    }

    public void setDefaultBalance(boolean defaultBalance) {
        this.defaultBalance = defaultBalance;
    }

    public String getBalanceEl() {
        return balanceEl;
    }

    public void setBalanceEl(String balanceEl) {
        this.balanceEl = balanceEl;
    }

    public List<OCCTemplate> getOccTemplates() {
        return occTemplates;
    }

    public void setOccTemplates(List<OCCTemplate> templates) {
        this.occTemplates = templates;
    }
}