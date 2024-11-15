package org.meveo.api.dto.billing;

import org.meveo.api.dto.BusinessEntityDto;
import org.meveo.model.cpq.enums.ContractAccountLevel;

import java.util.Date;
import java.util.Objects;

public class PurchaseOrderDto extends BusinessEntityDto {
	
	public final class AccountLevel {
		private Long id;
		public Long getId() {
			return id;
		}
	}
	
	private String number;
	private Date startDate;
	private Date endDate;
	private Date deliveryDate;
	private ContractAccountLevel accountLevel;
	private AccountLevel seller;
	private AccountLevel customer;
	private AccountLevel customerAccount;
	private AccountLevel billingAccount;
	
	
	public AccountLevel getBillingAccount() {
		return billingAccount;
	}
	
	public void setBillingAccount(AccountLevel billingAccount) {
		this.billingAccount = billingAccount;
	}
	
	public AccountLevel getCustomerAccount() {
		return customerAccount;
	}
	
	public void setCustomerAccount(AccountLevel customerAccount) {
		this.customerAccount = customerAccount;
	}
	
	public AccountLevel getCustomer() {
		return customer;
	}
	
	public void setCustomer(AccountLevel customer) {
		this.customer = customer;
	}
	
	public AccountLevel getSeller() {
		return seller;
	}
	
	public void setSeller(AccountLevel seller) {
		this.seller = seller;
	}
	
	public ContractAccountLevel getAccountLevel() {
		return accountLevel;
	}
	
	public void setAccountLevel(ContractAccountLevel accountLevel) {
		this.accountLevel = accountLevel;
	}
	
	public Date getDeliveryDate() {
		return deliveryDate;
	}
	
	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
	}
	
	public Date getEndDate() {
		return endDate;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	public String getNumber() {
		return number;
	}
	
	public void setNumber(String number) {
		this.number = number;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PurchaseOrderDto)) return false;
		PurchaseOrderDto that = (PurchaseOrderDto) o;
		return Objects.equals(getNumber(), that.getNumber()) && Objects.equals(getStartDate(), that.getStartDate()) && Objects.equals(getEndDate(), that.getEndDate()) && Objects.equals(getDeliveryDate(), that.getDeliveryDate()) && getAccountLevel() == that.getAccountLevel() && Objects.equals(getSeller(), that.getSeller()) && Objects.equals(getCustomer(), that.getCustomer()) && Objects.equals(getCustomerAccount(), that.getCustomerAccount()) && Objects.equals(getBillingAccount(), that.getBillingAccount());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getNumber(), getStartDate(), getEndDate(), getDeliveryDate(), getAccountLevel(), getSeller(), getCustomer(), getCustomerAccount(), getBillingAccount());
	}
}
