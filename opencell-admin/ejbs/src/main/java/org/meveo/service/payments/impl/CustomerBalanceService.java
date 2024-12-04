package org.meveo.service.payments.impl;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ValidationException;
import org.meveo.api.dto.account.CustomerAccountDto;
import org.meveo.api.dto.payment.AccountOperationDto;
import org.meveo.apiv2.payments.AccountOperationsDetails;
import org.meveo.apiv2.payments.AccountOperationsResult;
import org.meveo.apiv2.payments.ImmutableAccountOperationsResult;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.crm.custom.CustomFieldInheritanceEnum;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.CustomerBalance;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.model.payments.OperationCategoryEnum;
import org.meveo.service.admin.impl.TradingCurrencyService;
import org.meveo.service.api.EntityToDtoConverter;
import org.meveo.service.base.BusinessService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.ValueExpressionWrapper;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

/**
 * Service implementation to manage CustomerBalance entity.
 * It extends {@link PersistenceService} class
 * 
 * @author zelmeliani
 * @version 15.0.0
 *
 */
@Stateless
public class CustomerBalanceService extends BusinessService<CustomerBalance> {

    @Inject
    private OCCTemplateService occTemplateService;
    @Inject
    private CustomerAccountService customerAccountService;
    @Inject
    private TradingCurrencyService tradingCurrencyService;
    @Inject
    private AccountOperationService accountOperationService;
    @Inject
    protected EntityToDtoConverter entityToDtoConverter;

	/**
	 * Get the default CustomerBalance
	 * @return
	 */
	public CustomerBalance getDefaultOne() throws NoResultException, NonUniqueResultException {
		try {
			return getEntityManager().createNamedQuery("CustomerBalance.findDefaultOne", CustomerBalance.class)
			.setParameter("default", true)
			.getSingleResult();
		} catch (NoResultException e) {
	        return null;
	    } catch (NonUniqueResultException e) {
	        throw new BusinessException("there are multiple customer balance as default");
	    }
	}

    /**
     * Find default customer balance
     *
     * @return optional customer balance, empty if no result found
     */
    public Optional<CustomerBalance> findDefaultCustomerBalance() {
        try {
            return of((CustomerBalance) getEntityManager()
                    .createNamedQuery("CustomerBalance.findDefaultCustomerBalance")
                    .getSingleResult());
        } catch (NoResultException exception) {
            return empty();
        }
    }

    @Override
    public void create(CustomerBalance entity) {
        final int maxLimitCustomerBalance = paramBeanFactory
                .getInstance()
                .getPropertyAsInteger("max.customer.balance", 6);
        if(entity.getDescription() == null) {
            throw new BusinessException("Customer balance description is mandatory");
        }
        if (entity.isDefaultBalance() && findDefaultCustomerBalance().isPresent()) {
            throw new BusinessException("One default balance already exists");
        }
        if (count() >= maxLimitCustomerBalance) {
            throw new BusinessException("Customer balance number reached limit, max balance allowed : "
                    + maxLimitCustomerBalance);
        }
        if (entity.getOccTemplates() != null) {
            entity.setOccTemplates(validateAndAttachTemplates(entity.getOccTemplates()));
        }
        super.create(entity);
    }

    private List<OCCTemplate> validateAndAttachTemplates(List<OCCTemplate> templates) {
        int credit = 0;
        int debit = 0;
        List<OCCTemplate> attachedTemplates = new ArrayList<>();
        long id;
        for (OCCTemplate template : templates) {
            id = template.getId();
            template = occTemplateService.findById(id);
            if(template == null) {
                throw new NotFoundException("Occ template with id " + id + " does not exists");
            }
            if(template.getCode().endsWith("_FAE")) {
                throw new ValidationException("FAE not allowed in balance definition");
            }
            if(OperationCategoryEnum.DEBIT == template.getOccCategory()) {
                debit++;
            }
            if(OperationCategoryEnum.CREDIT == template.getOccCategory()) {
                credit++;
            }
            attachedTemplates.add(template);
        }
        if(credit == 0) {
            throw new ValidationException("Credit line should not be empty");
        }
        if(debit == 0) {
            throw new ValidationException("Debit line should not be empty");
        }
        return attachedTemplates;
    }

    @Override
    public CustomerBalance update(CustomerBalance entity) {
        CustomerBalance toUpdate = ofNullable(findById(entity.getId(), asList("occTemplates")))
                .orElseThrow(() -> new NotFoundException("Customer balance with id "
                        + entity.getId() + " and code " + entity.getCode() + " does not exists"));
        ofNullable(entity.getDescription()).ifPresent(toUpdate::setDescription);
        if (entity.getOccTemplates() != null && !entity.getOccTemplates().isEmpty()) {
            List<OCCTemplate> templates = validateAndAttachTemplates(entity.getOccTemplates());
            toUpdate.getOccTemplates().clear();
            toUpdate.setOccTemplates(templates);
        }
        toUpdate.setBalanceEl(entity.getBalanceEl());
        return super.update(toUpdate);
    }

    /**
     * Retrieves account operations based on the provided details.
     * This method retrieves account operations for a given customer balance and account,
     * filters them based on transactional currency and excluded account operations,
     * and calculates totals for credit, debit, and balance.
     *
     * @param resource The details of the account operations to retrieve.
     * @return An {@link AccountOperationsResult} object containing the retrieved account operations
     *         along with total credit, total debit, and balance.
     * @throws IllegalArgumentException if any of the input parameters are invalid.
     */
    public AccountOperationsResult getAccountOperations(AccountOperationsDetails resource) {
        // Validate customer balance and account
        CustomerBalance customerBalance = validateAndGetCustomerBalance(resource.customerBalance());
        CustomerAccount customerAccount = validateAndGetCustomerAccount(resource.customerAccount());

        // Determine transactional currency
        TradingCurrency transactionalCurrency = getTransactionalCurrency(resource);

        // Get OCC template codes to use
        List<String> linkedOccTemplates = getOccTemplateCodesToUse(customerBalance);

        // Retrieve account operations
        List<AccountOperation> accountOperations = accountOperationService.getAccountOperations(
                customerAccount.getId(),
                transactionalCurrency != null ? transactionalCurrency.getId() : null,
                linkedOccTemplates,
                resource.excludeAOs());

        // Filter account operations based on customer balance
        List<AccountOperationDto> result = filterAccountOperations(accountOperations, customerBalance);

        // Calculate totals for credit, debit, and balance
        BigDecimal credit = result.stream()
                .filter(aod -> aod.getTransactionCategory().equals(OperationCategoryEnum.CREDIT))
                .map(AccountOperationDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = result.stream()
                .filter(aod -> aod.getTransactionCategory().equals(OperationCategoryEnum.DEBIT))
                .map(AccountOperationDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = debit.subtract(credit);

        List<Long> aoIds = new ArrayList<>();
        for (AccountOperationDto accountOperationDto : result) {
            aoIds.add(accountOperationDto.getId());
        }

        // Build and return the result
        return ImmutableAccountOperationsResult.builder()
                .accountOperationIds(aoIds)
                .totalCredit(credit)
                .totalDebit(debit)
                .balance(balance)
                .build();
    }

    /**
     * Validates and retrieves a customer balance based on the provided {@link org.meveo.apiv2.payments.CustomerBalance}.
     *
     * @param customerBalance The {@link org.meveo.apiv2.payments.CustomerBalance} to validate and retrieve.
     * @return The validated and retrieved {@link CustomerBalance} object.
     * @throws BadRequestException if the provided customer balance is null or if its ID is null,
     *         or if the customer balance with the given ID does not exist.
     */
    private CustomerBalance validateAndGetCustomerBalance(org.meveo.apiv2.payments.CustomerBalance customerBalance) {
        if (customerBalance != null) {
            if (customerBalance.getId() != null) {
                return ofNullable(findById(customerBalance.getId()))
                        .orElseThrow(() -> new BadRequestException("Customer Balance does not exist"));
            } else {
                throw new BadRequestException("Customer Balance id is required");
            }
        } else {
            throw new BadRequestException("Customer Balance id is required");
        }
    }

    /**
     * Validates and retrieves a customer account based on the provided {@link CustomerAccountDto}.
     *
     * @param customerAccount The {@link CustomerAccountDto} to validate and retrieve.
     * @return The validated and retrieved {@link CustomerAccount} object.
     * @throws BadRequestException if the provided customer account is null or if its ID is null and the code is blank,
     *         or if neither ID nor code is provided, or if the customer account with the given ID or code does not exist.
     */
    private CustomerAccount validateAndGetCustomerAccount(CustomerAccountDto customerAccount) {
        if (customerAccount != null) {
            if (customerAccount.getId() != null) {
                return ofNullable(customerAccountService.findById(customerAccount.getId()))
                        .orElseThrow(() -> new BadRequestException("Customer Account does not exist"));
            } else if (!StringUtils.isBlank(customerAccount.getCode())) {
                return ofNullable(customerAccountService.findByCode(customerAccount.getCode()))
                        .orElseThrow(() -> new BadRequestException("Customer Account does not exist"));
            } else {
                throw new BadRequestException("Customer Account id or code is required");
            }
        } else {
            throw new BadRequestException("Customer Account id or code is required");
        }
    }

    /**
     * Retrieves the transactional currency based on the provided {@link AccountOperationsDetails}.
     *
     * @param resource The {@link AccountOperationsDetails} containing transactional currency information.
     * @return The retrieved {@link TradingCurrency} object or null if the transactional currency is not provided.
     * @throws BadRequestException if the provided transactional currency ID does not exist.
     */
    private TradingCurrency getTransactionalCurrency(AccountOperationsDetails resource) {
        if (resource.transactionalCurrency() != null) {
            return ofNullable(resource.transactionalCurrency())
                    .map(currency -> tradingCurrencyService.findById(currency.getId()))
                    .orElseThrow(() -> new BadRequestException("Transactional currency does not exist"));
        } else {
            return null;
        }
    }

    /**
     * Retrieves the OCC template codes to use based on the provided {@link CustomerBalance}.
     *
     * @param customerBalance The {@link CustomerBalance} from which to retrieve OCC template codes.
     * @return A {@link List} of OCC template codes.
     */
    private List<String> getOccTemplateCodesToUse(CustomerBalance customerBalance) {
        return customerBalance.getOccTemplates().stream()
                .map(OCCTemplate::getCode)
                .collect(Collectors.toList());
    }

    /**
     * Filters the list of {@link AccountOperation} objects based on the provided {@link CustomerBalance}.
     * It evaluates the balance expression of the customer balance to determine whether each account operation should be included.
     *
     * @param accountOperations The list of {@link AccountOperation} objects to filter.
     * @param customerBalance The {@link CustomerBalance} used for filtering account operations.
     * @return A list of {@link AccountOperationDto} objects that meet the filtering criteria.
     */
    private List<AccountOperationDto> filterAccountOperations(List<AccountOperation> accountOperations, CustomerBalance customerBalance) {
        List<AccountOperationDto> filteredOperations = new ArrayList<>();

        for (AccountOperation operation : accountOperations) {
            if (customerBalance.getBalanceEl() != null && !customerBalance.getBalanceEl().isEmpty()) {
                Map<Object, Object> expressionMap = new HashMap<>();
                expressionMap.put("accountOperation", operation);

                if (customerBalance.getBalanceEl().contains("currentDate")) {
                    expressionMap.put("currentDate", new Date());
                }

                Boolean passesFilter = ValueExpressionWrapper.evaluateExpression(customerBalance.getBalanceEl(), expressionMap, Boolean.class);

                if (Boolean.TRUE.equals(passesFilter)) {
                    AccountOperationDto operationDto = new AccountOperationDto(operation, entityToDtoConverter.getCustomFieldsDTO(operation, CustomFieldInheritanceEnum.INHERIT_NO_MERGE));
                    filteredOperations.add(operationDto);
                }
            } else {
                AccountOperationDto operationDto = new AccountOperationDto(operation, entityToDtoConverter.getCustomFieldsDTO(operation, CustomFieldInheritanceEnum.INHERIT_NO_MERGE));
                filteredOperations.add(operationDto);
            }
        }
        return filteredOperations;
    }
}
