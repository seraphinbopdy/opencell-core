package org.meveo.model.billing;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessEntity;
import org.meveo.model.HugeEntity;
import org.meveo.model.ObservableEntity;
import org.meveo.model.admin.Seller;
import org.meveo.model.cpq.enums.ContractAccountLevel;
import org.meveo.model.crm.Customer;
import org.meveo.model.payments.CustomerAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

@Entity
@HugeEntity
@ObservableEntity
@Table(name = "billing_purchase_order", uniqueConstraints = @UniqueConstraint(columnNames = { "number", "code" }))
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "billing_purchase_order_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "PurchaseOrder.findByNumber", query = "SELECT po FROM PurchaseOrder po WHERE po.number = :number") })
public class PurchaseOrder extends BusinessEntity {

    @Column(name = "number", length = 100)
    private String number;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date")
    private Date endDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "delivery_date")
    private Date deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_level")
    private ContractAccountLevel accountLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id")
    private CustomerAccount customerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_account_id")
    private BillingAccount billingAccount;


    /**
     * subscriptions which are related to the purchase order.
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "purchaseOrders", cascade = CascadeType.ALL)
    private Set<Subscription> subscriptions = new HashSet<>();

    /**
     * invoices which are related to the purchase order.
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "purchaseOrders", cascade = CascadeType.ALL)
    private Set<Invoice> invoices = new HashSet<>();

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public ContractAccountLevel getAccountLevel() {
        return accountLevel;
    }

    public void setAccountLevel(ContractAccountLevel accountLevel) {
        this.accountLevel = accountLevel;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public CustomerAccount getCustomerAccount() {
        return customerAccount;
    }

    public void setCustomerAccount(CustomerAccount customerAccount) {
        this.customerAccount = customerAccount;
    }

    public BillingAccount getBillingAccount() {
        return billingAccount;
    }

    public void setBillingAccount(BillingAccount billingAccount) {
        this.billingAccount = billingAccount;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Set<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Set<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Set<Invoice> getInvoices() {
        return invoices;
    }

    public void setInvoices(Set<Invoice> invoices) {
        this.invoices = invoices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PurchaseOrder))
            return false;
        if (!super.equals(o))
            return false;
        PurchaseOrder that = (PurchaseOrder) o;
        return Objects.equals(getNumber(), that.getNumber()) && Objects.equals(getStartDate(), that.getStartDate()) && Objects.equals(getEndDate(), that.getEndDate())
                && Objects.equals(getDeliveryDate(), that.getDeliveryDate()) && getAccountLevel() == that.getAccountLevel() && Objects.equals(getSeller(), that.getSeller())
                && Objects.equals(getCustomer(), that.getCustomer()) && Objects.equals(getCustomerAccount(), that.getCustomerAccount()) && Objects.equals(getBillingAccount(), that.getBillingAccount());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getNumber(), getStartDate(), getEndDate(), getDeliveryDate(), getAccountLevel(), getSeller(), getCustomer(), getCustomerAccount(), getBillingAccount());
    }
}
