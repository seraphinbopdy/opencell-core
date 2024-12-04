package org.meveo.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.IsoIcd;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.crm.Customer;
import org.meveo.model.payments.CustomerAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@ObservableEntity
@Table(name = "account_registration_number")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "account_registration_number_seq"), @Parameter(name = "increment_size", value = "1") })
public class RegistrationNumber extends AuditableEntity {

    @Column(name = "registration_no")
    private String registrationNo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icd_id")
    private IsoIcd isoIcd;

    @Transient
    private AccountEntity accountEntity;

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
    @JoinColumn(name = "user_account_id")
    private UserAccount userAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_account_id")
    private BillingAccount billingAccount;

    public RegistrationNumber() {
    }

    public RegistrationNumber(String registrationNo, IsoIcd isoIcd, AccountEntity accountEntity) {
        this.registrationNo = registrationNo;
        this.isoIcd = isoIcd;
        setAccountEntity(accountEntity);
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public RegistrationNumber setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
        return this;
    }

    public IsoIcd getIsoIcd() {
        return isoIcd;
    }

    public RegistrationNumber setIsoIcd(IsoIcd isoIcd) {
        this.isoIcd = isoIcd;
        return this;
    }

    public AccountEntity getAccountEntity() {
        return accountEntity;
    }

    public AccountEntity getAccountEntity(AccountEntity accountEntity) {
        if (accountEntity instanceof Seller)
            return this.seller;
        else if (accountEntity instanceof Customer)
            return this.customer;
        else if (accountEntity instanceof CustomerAccount)
            return this.customerAccount;
        else if (accountEntity instanceof BillingAccount)
            return this.billingAccount;
        else if (accountEntity instanceof UserAccount)
            return this.userAccount;
        return this.accountEntity;
    }

    public RegistrationNumber setAccountEntity(AccountEntity accountEntity) {
        this.accountEntity = accountEntity;
        if (accountEntity == null) {
            this.seller = null;
            this.customer = null;
            this.customerAccount = null;
            this.billingAccount = null;
            this.userAccount = null;
            return this;
        }
        if (accountEntity instanceof Seller)
            this.seller = (Seller) accountEntity;
        else if (accountEntity instanceof Customer)
            this.customer = (Customer) accountEntity;
        else if (accountEntity instanceof CustomerAccount)
            this.customerAccount = (CustomerAccount) accountEntity;
        else if (accountEntity instanceof BillingAccount)
            this.billingAccount = (BillingAccount) accountEntity;
        else
            this.userAccount = (UserAccount) accountEntity;
        return this;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void setCustomerAccount(CustomerAccount customerAccount) {
        this.customerAccount = customerAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public void setBillingAccount(BillingAccount billingAccount) {
        this.billingAccount = billingAccount;
    }
}
