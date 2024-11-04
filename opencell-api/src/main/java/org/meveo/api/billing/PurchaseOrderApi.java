package org.meveo.api.billing;

import org.apache.commons.lang3.StringUtils;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.billing.PurchaseOrderDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.model.AccountEntity;
import org.meveo.model.BusinessEntity;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.PurchaseOrder;
import org.meveo.model.cpq.enums.ContractAccountLevel;
import org.meveo.model.crm.Customer;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.service.admin.impl.CustomGenericEntityCodeService;
import org.meveo.service.admin.impl.SellerService;
import org.meveo.service.base.AccountService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.PurchaseOrderService;
import org.meveo.service.crm.impl.CustomerService;
import org.meveo.service.payments.impl.CustomerAccountService;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class PurchaseOrderApi extends BaseApi {
	
	@Inject
	private SellerService sellerService;
	@Inject
	private CustomerService customerService;
	@Inject
	private CustomerAccountService customerAccountService;
	@Inject
	private BillingAccountService billingAccountService;
	@Inject
	private PurchaseOrderService purchaseOrderService;
	@Inject
	protected CustomGenericEntityCodeService customGenericEntityCodeService;
	
	public PurchaseOrderDto create(PurchaseOrderDto purchaseOrderDto) {
		if (purchaseOrderDto == null) {
			throw new IllegalArgumentException("PurchaseOrderDto is null");
		}
		handleMissingParameters(purchaseOrderDto, "number", "accountLevel");
		var purchaseOrder = new PurchaseOrder();
		purchaseOrder.setNumber(purchaseOrderDto.getNumber());
		// check if the number exists
		if (purchaseOrderService.findByNumber(purchaseOrderDto.getNumber()) != null) {
			throw new EntityAlreadyExistsException(PurchaseOrder.class, purchaseOrderDto.getNumber());
		}
		
		// generate code if not provided
		purchaseOrder.setCode(purchaseOrderDto.getCode());
		if(StringUtils.isBlank(purchaseOrderDto.getCode()) ) {
			purchaseOrder.setCode(customGenericEntityCodeService.getGenericEntityCode(purchaseOrder));
		}else{
			if(purchaseOrderService.findByCode(purchaseOrderDto.getCode()) != null) {
				throw new EntityAlreadyExistsException(PurchaseOrder.class, purchaseOrderDto.getCode());
			}
		}
		purchaseOrderDto.setCode(purchaseOrder.getCode());
		purchaseOrder.setDescription(purchaseOrderDto.getDescription());
		purchaseOrder.setAccountLevel(purchaseOrderDto.getAccountLevel());
		// check if the corresponding accountLevel is not null
		switch (purchaseOrderDto.getAccountLevel()) {
			case SELLER:
				handleMissingParameters(purchaseOrderDto, "seller");
				purchaseOrder.setSeller(getAccountLevelEntity(purchaseOrderDto.getSeller().getId(), sellerService, Seller.class));
				break;
			case CUSTOMER:
				handleMissingParameters(purchaseOrderDto, "customer");
				purchaseOrder.setCustomer(getAccountLevelEntity(purchaseOrderDto.getCustomer().getId(), customerService, Customer.class));
				break;
			case CUSTOMER_ACCOUNT:
				handleMissingParameters(purchaseOrderDto, "customerAccount");
				purchaseOrder.setCustomerAccount(getAccountLevelEntity(purchaseOrderDto.getCustomerAccount().getId(), customerAccountService, CustomerAccount.class));
				break;
			case BILLING_ACCOUNT:
				handleMissingParameters(purchaseOrderDto, "billingAccount");
				purchaseOrder.setBillingAccount(getAccountLevelEntity(purchaseOrderDto.getBillingAccount().getId(), billingAccountService, BillingAccount.class));
				break;
			default:
				throw new IllegalArgumentException("AccountLevel not supported: " + purchaseOrderDto.getAccountLevel());
		}
		
		purchaseOrder.setStartDate(purchaseOrderDto.getStartDate());
		purchaseOrder.setEndDate(purchaseOrderDto.getEndDate());
		purchaseOrder.setDeliveryDate(purchaseOrderDto.getDeliveryDate());
		purchaseOrderService.create(purchaseOrder);
		purchaseOrderDto.setId(purchaseOrder.getId());
		return purchaseOrderDto;
	}
	
	public void update(Long id, PurchaseOrderDto purchaseOrderDto) {
		if (purchaseOrderDto == null) {
			throw new IllegalArgumentException("PurchaseOrderDto is null");
		}
		var purchaseOrder = purchaseOrderService.findById(id);
		if (purchaseOrder == null) {
			throw new EntityDoesNotExistsException(PurchaseOrder.class, id);
		}
		if(purchaseOrder.getDescription() != null) {
			purchaseOrder.setDescription(purchaseOrderDto.getDescription());
		}
		purchaseOrder.setStartDate(purchaseOrderDto.getStartDate() != null ? purchaseOrderDto.getStartDate() : purchaseOrder.getStartDate());
		purchaseOrder.setEndDate(purchaseOrderDto.getEndDate() != null ? purchaseOrderDto.getEndDate() : purchaseOrder.getEndDate());
		purchaseOrder.setDeliveryDate(purchaseOrderDto.getDeliveryDate() != null ? purchaseOrderDto.getDeliveryDate() : purchaseOrder.getDeliveryDate());
		purchaseOrderService.update(purchaseOrder);
	}
	
	private <T extends AccountEntity> T getAccountLevelEntity(Long id, AccountService<T> accountService, Class clazz) {
		try{
			return (T) accountService.getEntityManager().createQuery("SELECT a FROM " + clazz.getSimpleName() + " a WHERE a.id = :id", clazz)
					.setParameter("id", id)
					.getSingleResult();
		}catch (Exception e) {
			throw new EntityDoesNotExistsException(clazz, id);
		}
	}
}
