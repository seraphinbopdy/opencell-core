package org.meveo.model.billing;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessEntity;
import org.meveo.model.HugeEntity;
import org.meveo.model.ObservableEntity;
import org.meveo.model.admin.Seller;
import org.meveo.model.cpq.enums.ContractAccountLevel;
import org.meveo.model.crm.Customer;
import org.meveo.model.payments.CustomerAccount;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import java.util.Date;
import java.util.Objects;

@Entity
@HugeEntity
@ObservableEntity
@Table(name = "billing_purchase_order", uniqueConstraints = @UniqueConstraint(columnNames = { "number", "code" }))
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = { @Parameter(name = "sequence_name", value = "billing_purchase_order_seq"), })
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
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PurchaseOrder)) return false;
		if (!super.equals(o)) return false;
		PurchaseOrder that = (PurchaseOrder) o;
		return Objects.equals(getNumber(), that.getNumber()) && Objects.equals(getStartDate(), that.getStartDate()) && Objects.equals(getEndDate(), that.getEndDate()) && Objects.equals(getDeliveryDate(), that.getDeliveryDate()) && getAccountLevel() == that.getAccountLevel() && Objects.equals(getSeller(), that.getSeller()) && Objects.equals(getCustomer(), that.getCustomer()) && Objects.equals(getCustomerAccount(), that.getCustomerAccount()) && Objects.equals(getBillingAccount(), that.getBillingAccount());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getNumber(), getStartDate(), getEndDate(), getDeliveryDate(), getAccountLevel(), getSeller(), getCustomer(), getCustomerAccount(), getBillingAccount());
	}
}
