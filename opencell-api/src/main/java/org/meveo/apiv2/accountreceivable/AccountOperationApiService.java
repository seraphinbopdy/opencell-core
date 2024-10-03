package org.meveo.apiv2.accountreceivable;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Hibernate;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.api.dto.payment.UnMatchingOperationRequestDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.apiv2.AcountReceivable.AccountOperationAndSequence;
import org.meveo.apiv2.AcountReceivable.AmountToTransferDto;
import org.meveo.apiv2.AcountReceivable.AmountsTransferDto;
import org.meveo.apiv2.AcountReceivable.CustomerAccount;
import org.meveo.apiv2.AcountReceivable.LitigationInput;
import org.meveo.apiv2.AcountReceivable.UnMatchingAccountOperationDetail;
import org.meveo.apiv2.generic.exception.ConflictException;
import org.meveo.apiv2.ordering.services.ApiService;
import org.meveo.model.MatchingReturnObject;
import org.meveo.model.PartialMatchingOccToSelect;
import org.meveo.model.billing.AccountingCode;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.AccountOperationStatus;
import org.meveo.model.payments.MatchingAmount;
import org.meveo.model.payments.MatchingStatusEnum;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.model.payments.OperationCategoryEnum;
import org.meveo.model.payments.OtherCreditAndCharge;
import org.meveo.model.payments.RecordedInvoice;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.MatchingCodeService;
import org.meveo.service.payments.impl.OCCTemplateService;
import org.meveo.service.payments.impl.OtherCreditAndChargeService;
import org.meveo.service.payments.impl.PaymentPlanService;
import org.meveo.service.payments.impl.RecordedInvoiceService;
import org.meveo.service.securityDeposit.impl.SecurityDepositTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.meveo.model.payments.AccountOperationStatus.EXPORTED;
import static org.meveo.model.payments.AccountOperationStatus.POSTED;

public class AccountOperationApiService implements ApiService<AccountOperation> {
	
	protected static Logger log = LoggerFactory.getLogger(AccountOperationApiService.class);
    @Inject
    protected ResourceBundle resourceMessages;
    
	@Inject
	private AccountOperationService accountOperationService;

	@Inject
	private CustomerAccountService customerAccountService;

	@Inject
	private MatchingCodeService matchingCodeService;

	@Inject
	private PaymentPlanService paymentPlanService;

	@Inject
	private SecurityDepositTransactionService securityDepositTransactionService;

	@Inject
	private RecordedInvoiceService recordedInvoiceService;
	
	@Inject
	private OCCTemplateService occTemplateService;
	
	
	@Override
	public List<AccountOperation> list(Long offset, Long limit, String sort, String orderBy, String filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getCount(String filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<AccountOperation> findById(Long id) {
		return ofNullable(accountOperationService.findById(id));
	}

	@Override
	public AccountOperation create(AccountOperation baseEntity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<AccountOperation> update(Long id, AccountOperation baseEntity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<AccountOperation> patch(Long id, AccountOperation baseEntity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<AccountOperation> delete(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<AccountOperation> findByCode(String code) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void changeStatus(ChangeStatusDto changeStatusDto) {
		List<Long> ids = changeStatusDto.getAccountOperations();
		AccountOperationStatus status = changeStatusDto.getStatus();
		ids = new ArrayList<>(new TreeSet<>(ids));// removeDuplicates

		List<AccountOperation> accountOperations = accountOperationService.findByIds(ids);
		if (accountOperations == null || ids.size() > accountOperations.size()) {
			List<Long> dbIds = accountOperations.stream().map(ao -> ao.getId()).collect(Collectors.toList());
			List<Long> wrongIds = ids.stream().filter(id -> !dbIds.contains(id)).collect(Collectors.toList());
			throw new EntityDoesNotExistsException("AccountOperation", wrongIds);
		}
		if (AccountOperationStatus.EXPORTED.equals(status)) {
			Map<Boolean, List<AccountOperation>> statusGroups = accountOperations.stream()
					.collect(Collectors.partitioningBy(ao -> POSTED.equals(ao.getStatus())));
			accountOperationService.updateStatusInNewTransaction(statusGroups.get(true), status, null);
			if (!CollectionUtils.isEmpty(statusGroups.get(false))) {
				throw new ConflictException("The status of following account operations can not be updated: "
						+ statusGroups.get(false).stream().map(ao -> ao.getId() + ", ").reduce("", String::concat));
			}
		} else {
			throw new ConflictException("not possible to change accountOperation status to '" + status + "'");
		}
	}

	public Optional<AccountOperation> assignAccountOperation(Long accountOperationId,
																					 CustomerAccount customerAccountInput) {
		AccountOperation accountOperation = accountOperationService.findById(accountOperationId);
		if(accountOperation == null) {
			throw new NotFoundException("Account operation does not exits");
		} else {
			org.meveo.model.payments.CustomerAccount customerAccount = getCustomerAccount(customerAccountInput);
			if(customerAccount == null) {
				throw new NotFoundException("Customer account does not exits");
			}
			accountOperation.setCustomerAccount(customerAccount);
			if (accountOperation.getStatus() == EXPORTED) {
				accountOperation.setStatus(POSTED);
				// In this case, OperationNumber shall be incremented (https://opencellsoft.atlassian.net/browse/INTRD-7017)
				accountOperationService.fillOperationNumber(accountOperation);
			}

			try {
				accountOperationService.update(accountOperation);
			} catch (Exception exception) {
				throw new BadRequestException(exception.getMessage());
			}
			return of(accountOperation);
		}
	}

	public MatchingReturnObject matchOperations(List<AccountOperationAndSequence> accountOperations) {
		// Get and Sort AccountOperation by sequence (sort used for send order in matchingOperation service)
		List<Long> aoIds = accountOperations.stream()
				.sorted(Comparator.comparing(AccountOperationAndSequence::getSequence))
				.map(AccountOperationAndSequence::getId)
				.collect(Collectors.toList());

		// Check existence of all passed accountOperation
		List<AccountOperation> aos = new ArrayList<>();
		aoIds.forEach(aoId -> aos.add(accountOperationService.findById(aoId)));
		checkIncompatibleAccountOperationTypes(aos);
		if (aoIds.size() != aos.size()) {
			throw new EntityDoesNotExistsException("One or more AccountOperations passed for matching are not found");
		}

		// Check that all AOs have the same customer account except for orphan AOs. if not throw an exception.
		List<Long> customerIds = aos.stream()
				.map(AccountOperation::getCustomerAccount)
				.filter(Objects::nonNull)
				.map(org.meveo.model.payments.CustomerAccount::getId)
				.collect(Collectors.toList());

		if (new HashSet<>(customerIds).size() > 1) {
			throw new BusinessApiException("Matching action is failed : AccountOperations passed for matching are linked to different CustomerAccount");
		}

		// Can not match AOs of  type SecurityDeposit(CRD_SD/DEB_SD/REF_SD)
		Set<String> aoCodes = aos.stream()
				.map(AccountOperation::getCode)
				.collect(Collectors.toSet());

		Set<String> unExpectedAoCodes = Stream.of("CRD_SD", "REF_SD")
				.collect(Collectors.toSet());

		if (!Collections.disjoint(unExpectedAoCodes, aoCodes)) {
			throw new BusinessApiException("Matching action is failed : AccountOperations passed for matching contains one of unexpected codes "
					+ unExpectedAoCodes);
		}

		// Update orphans AO by setting the same customerAccount
		org.meveo.model.payments.CustomerAccount customer = customerAccountService.findById(customerIds.get(0));
		if (customer == null) {
			throw new BusinessApiException("Matching action is failed : No CustomerAccount found with id " + customerIds.get(0) + " for matching");
		}
		
		aos.stream().forEach(accountOperation -> {
	          // check amount to match
	        Optional<AccountOperationAndSequence> accountOperationAndSequenceOptional = accountOperations.stream().filter(aoas -> aoas.getId().equals(accountOperation.getId())).findFirst();
			if (accountOperationAndSequenceOptional.isPresent()) {
				if (accountOperationAndSequenceOptional.get().getAmountToMatch() != null) {
					BigDecimal amountToMatch = accountOperationAndSequenceOptional.get().getAmountToMatch();
					Integer sequence = accountOperationAndSequenceOptional.get().getSequence();
					if (amountToMatch != null) {
						if (amountToMatch.compareTo(BigDecimal.ZERO) <= 0) {
							throw new BusinessApiException("The amount to match must be greater than 0");
						} else if (amountToMatch.compareTo(accountOperation.getUnMatchingAmount()) > 0) {
							throw new BusinessApiException("The amount to match must be less than : " + accountOperation.getUnMatchingAmount().doubleValue() + " for sequence : " + sequence);
						}
						accountOperation.setAmountForUnmatching(amountToMatch);
					}
				} else {
					accountOperation.setAmountForUnmatching(accountOperation.getUnMatchingAmount());
				}
			}
		});
		
		Optional.of(aos.stream().filter(accountOperation -> accountOperation.getCustomerAccount() == null)
						.collect(Collectors.toList())).orElse(Collections.emptyList())
				.forEach(accountOperation -> {
					accountOperation.setCustomerAccount(customer);
					// change status of orphan AO after CA assignement : new requirement added as bug https://opencellsoft.atlassian.net/browse/INTRD-8217
					accountOperation.setStatus(POSTED);
					accountOperationService.update(accountOperation);
				});

		try {
			// First AO is Credit, and shall be add with DEBIT to do unitary matching
			Long creditAoId = aos.stream().filter(ao -> OperationCategoryEnum.CREDIT == ao.getTransactionCategory()).findFirst()
					.orElseThrow(() -> new BusinessApiException("No credit AO passed for matching")).getId();
			
			// Split AO debit & credit
			List<AccountOperation> creditAOs = aos.stream().filter(ao -> OperationCategoryEnum.CREDIT == ao.getTransactionCategory()).collect(Collectors.toList());
			if (creditAOs == null || creditAOs.isEmpty()) {
				throw new BusinessApiException("No credit AO passed for matching");
			}
			List<AccountOperation> debitAOs = aos.stream().filter(ao -> OperationCategoryEnum.DEBIT == ao.getTransactionCategory()).collect(Collectors.toList());
			if (debitAOs == null || debitAOs.isEmpty()) {
				throw new BusinessApiException("No debit AO passed for matching");
			}

			MatchingReturnObject matchingResult = new MatchingReturnObject();
			List<PartialMatchingOccToSelect> partialMatchingOcc = new ArrayList<>();
			matchingResult.setPartialMatchingOcc(partialMatchingOcc);
			if (CollectionUtils.isNotEmpty(aos)) {
			    TradingCurrency theFirstTradingCurrency = aos.get(0).getTransactionalCurrency();
				var debitAOIds = debitAOs.stream().map(AccountOperation::getId).collect(Collectors.toList());
					for (AccountOperation creditAO : creditAOs) {
						if (!theFirstTradingCurrency.getId().equals(creditAO.getTransactionalCurrency().getId())) {
							throw new BusinessApiException(resourceMessages.getString("accountOperation.error.sameCurrency"));
						}
						if (creditAO.getMatchingStatus() != MatchingStatusEnum.O && creditAO.getMatchingStatus() != MatchingStatusEnum.P) {
							continue;
						}
						MatchingReturnObject unitaryResult = matchingCodeService.matchOperations(customer.getId(),	customer.getCode(), debitAOIds, creditAO.getId(), creditAO.getAmountForUnmatching());

						if (matchingResult.getPartialMatchingOcc() != null) {
							partialMatchingOcc.addAll(matchingResult.getPartialMatchingOcc());
						}

						matchingResult.setOk(unitaryResult.isOk());
					}
			}

			if (partialMatchingOcc.isEmpty()) {
				// Reload AO to get updated MatchingStatus
				List<AccountOperation> aoPartially = accountOperationService.findByIds(aoIds).stream()
						.filter(accountOperation -> accountOperation.getMatchingStatus() == MatchingStatusEnum.P)
						.collect(Collectors.toList());

				if (!aoPartially.isEmpty()) {
					PartialMatchingOccToSelect p = new PartialMatchingOccToSelect();
					p.setAccountOperation(aoPartially.get(0));
					p.setPartialMatchingAllowed(true);
					matchingResult.getPartialMatchingOcc().add(p);
				}
			}

			// update PaymentPlan
			List<Long> debitAos = new ArrayList<>(aoIds);
			debitAos.remove(creditAoId);
			paymentPlanService.toComplete(debitAos);

			return matchingResult;

		} catch (Exception e) {
			throw new BusinessApiException(e.getMessage());
		}
	}

	public List<UnMatchingOperationRequestDto> validateAndGetAOForUnmatching(List<UnMatchingAccountOperationDetail> accountOperations){
		// Get AccountOperation
		List<Long> aoIds = accountOperations.stream()
				.map(UnMatchingAccountOperationDetail::getId)
				.collect(Collectors.toList());

		// Check existence of all passed accountOperation
		List<AccountOperation> aos = accountOperationService.findByIds(aoIds);

		if (aoIds.size() != aos.size()) {
			throw new EntityDoesNotExistsException("One or more AccountOperations passed for unmatching are not found");
		}

		// Check if AO is already used at SecurityDepositTransaction : Can not unMatch AO used by the SecurityDeposit
		// Unitary check
		aoIds.forEach(id -> {
			List<String> securityDepositCodes = securityDepositTransactionService.getSecurityDepositCodesByAoIds(id);
			if (securityDepositCodes != null && !securityDepositCodes.isEmpty()) {
				throw new BusinessApiException("Unmatching action is failed : Cannot unmatch AO used by the SecurityDeposit codes: "
						+ new HashSet<>(securityDepositCodes));
			}
		});

		List<UnMatchingOperationRequestDto> toUnmatch = new ArrayList<>(aos.size());

		aos.forEach(ao -> {
			UnMatchingOperationRequestDto unM = new UnMatchingOperationRequestDto();
			unM.setAccountOperationId(ao.getId());

			UnMatchingAccountOperationDetail unMatchingAccountOperationDetail = accountOperations.stream()
					.filter(aoRequest -> ao.getId().equals(aoRequest.getId()))
					.findAny()
					.orElse(null);

			if (unMatchingAccountOperationDetail != null) {
				unM.setMatchingAmountIds(unMatchingAccountOperationDetail.getMatchingAmountIds());
			}
			unM.setCustomerAccountCode(customerAccountService.findById(ao.getCustomerAccount().getId()).getCode());

			toUnmatch.add(unM);
		});

		return toUnmatch;

	}

	private org.meveo.model.payments.CustomerAccount getCustomerAccount(CustomerAccount customerAccountInput) {
		org.meveo.model.payments.CustomerAccount customerAccount = null;
		if(customerAccountInput.getId() != null) {
			customerAccount = customerAccountService.findById(customerAccountInput.getId());
		}
		if(customerAccountInput.getCode() != null && customerAccount == null) {
			customerAccount = customerAccountService.findByCode(customerAccountInput.getCode());
		}
		return customerAccount;
	}

	/**
	 * @param accountOperationId recordedInvoice id.
	 * @param litigationInput litigation input.
	 * @return id of the updated recordedInvoice
	 */
	public Long setLitigation(Long accountOperationId, LitigationInput litigationInput) {
		RecordedInvoice recordedInvoice = ofNullable(recordedInvoiceService.findById(accountOperationId))
				.orElseThrow(() -> new NotFoundException("Account operation does not exits"));
		try {
			return recordedInvoiceService.setLitigation(recordedInvoice, litigationInput.getLitigationReason()).getId();
		} catch (BusinessException exception) {
			throw new BusinessApiException(exception.getMessage());
		}
	}

	/**
	 * @param accountOperationId recordedInvoice id.
	 * @param litigationInput litigation input.
	 * @return id of the updated recordedInvoice
	 */
	public Long removeLitigation(Long accountOperationId, LitigationInput litigationInput) {
		RecordedInvoice recordedInvoice = ofNullable(recordedInvoiceService.findById(accountOperationId))
				.orElseThrow(() -> new NotFoundException("Account operation does not exits"));
		try {
			return recordedInvoiceService.removeLitigation(recordedInvoice, litigationInput.getLitigationReason()).getId();
		} catch (BusinessException exception) {
			throw new BusinessApiException(exception.getMessage());
		}
	}
	
	public void transferAmounts(Long accountOperationId, AmountsTransferDto amountsTransferDto) {
		if(amountsTransferDto.getAmountsToTransfer() == null || amountsTransferDto.getAmountsToTransfer().size() == 0) {
			throw new BadRequestException("Amounts to transfer are required");
		}
		log.info("start transfering amount from account operation id : " + accountOperationId + " to customer accounts ");
		// check that all customer accounts exist
		checkCustomerAccountsExist(amountsTransferDto);
		// return account operation by id
		AccountOperation accountOperation = accountOperationService.findById(accountOperationId, List.of("customerAccount"));
		if(accountOperation == null) {
			throw new EntityDoesNotExistsException(AccountOperation.class, accountOperationId);
		}
		// check that all customer accounts have the same currency
		checkCustomerAccountsCurrency((org.meveo.model.payments.CustomerAccount) Hibernate.unproxy(accountOperation.getCustomerAccount()), amountsTransferDto);
		// check that sum of amount are lower of equal to source’s unmatched amount
		checkAmountsToTransfer(accountOperation, amountsTransferDto);
		// transfer amounts
		transferAmountsForCustomerAccounts(accountOperation, amountsTransferDto);
	}
	/**
	 * Transfer amounts for customer accounts
	 * @param accountOperation : account operation
	 * @param amountsTransferDto : amounts to transfer
	 */
	private void transferAmountsForCustomerAccounts(AccountOperation accountOperation, AmountsTransferDto amountsTransferDto) {
		final String CRD_TRS = "CRD_TRS";
		final String DBT_TRS = "DBT_TRS";
		OCCTemplate occTemplateCrdTrs = occTemplateService.findByCode(CRD_TRS, List.of("accountingCode"));
		OCCTemplate occTemplateDebTrs = occTemplateService.findByCode(DBT_TRS, List.of("accountingCode"));
		if(occTemplateCrdTrs == null || occTemplateDebTrs == null) {
			throw new EntityDoesNotExistsException(AccountingCode.class, CRD_TRS +"/"+ DBT_TRS);
		}
		// for each amount to transfer
		amountsTransferDto.getAmountsToTransfer().forEach(amountToTransferDto -> {
			// get customer account to transfer
			org.meveo.model.payments.CustomerAccount customerAccountTarget = getCustomerAccount(amountToTransferDto.getCustomerAccount());
			// if account operation is CREDIT
			// create a OCC of type  DEB_TRS on source Customer Account and match it with the OCC of type CREDIT
			// create a OCC of type  CRD_TRS on target Customer Account and match it with the OCC of type DEBIT
			if (OperationCategoryEnum.CREDIT == accountOperation.getTransactionCategory()) {
				createAccountOperation(accountOperation, accountOperation.getCustomerAccount(), OperationCategoryEnum.DEBIT, occTemplateDebTrs, amountToTransferDto.getAmount());
				createAccountOperation(accountOperation, customerAccountTarget, OperationCategoryEnum.CREDIT, occTemplateCrdTrs, amountToTransferDto.getAmount());
			} else {
				// if account operation is DEBIT
				// create a OCC of type  CRD_TRS on source Customer Account and match it with the OCC of type DEBIT
				// create a OCC of type  DEB_TRS on target Customer Account and match it with the OCC of type CREDIT
				createAccountOperation(accountOperation, accountOperation.getCustomerAccount(), OperationCategoryEnum.CREDIT, occTemplateCrdTrs, amountToTransferDto.getAmount());
				createAccountOperation(accountOperation, customerAccountTarget, OperationCategoryEnum.DEBIT, occTemplateDebTrs, amountToTransferDto.getAmount());
			}
		});
	}
	
	/**
	 * Create account operation
	 * @param accountOperation : account operation
	 * @param operationCategoryEnum : operation category CREDIT/DEBIT
	 */
	private void createAccountOperation(AccountOperation accountOperation,
	                                    org.meveo.model.payments.CustomerAccount customerAccountTarget,
	                                    OperationCategoryEnum operationCategoryEnum, OCCTemplate occTemplate, BigDecimal amountToTransfer) {
		Date currentDate = new Date();
		OtherCreditAndCharge accountOperationToTransfer = new OtherCreditAndCharge();
		accountOperationToTransfer.setCode(occTemplate.getCode());
		accountOperationToTransfer.setDescription(occTemplate.getAccountingCode() != null ? occTemplate.getAccountingCode().getDescription(): null);
		accountOperationToTransfer.setAccountingCode(accountOperation.getAccountingCode());
		accountOperationToTransfer.setAccountingDate(currentDate);
		accountOperationToTransfer.setAmount(amountToTransfer);
		accountOperationToTransfer.setTransactionalUnMatchingAmount(amountToTransfer);
		accountOperationToTransfer.setCustomerAccount(customerAccountTarget);
		accountOperationToTransfer.setMatchingStatus(MatchingStatusEnum.O);
		accountOperationToTransfer.setStatus(AccountOperationStatus.POSTED);
		accountOperationToTransfer.setTransactionCategory(operationCategoryEnum);
		accountOperationToTransfer.setTransactionDate(currentDate);
		accountOperationToTransfer.setUnMatchingAmount(amountToTransfer);
		accountOperationToTransfer.setReference(operationCategoryEnum.toString().charAt(0) + "_" + accountOperation.getId() + "_" + accountOperation.getReference()) ;
		accountOperationToTransfer.setSourceAccountOperation(accountOperation);
		accountOperationToTransfer.setUuid(UUID.randomUUID().toString());
		accountOperationToTransfer.setSeller(accountOperation.getSeller());
		accountOperationToTransfer.setJournal(occTemplate.getJournal());
		accountOperationToTransfer.setTransactionalCurrency(accountOperation.getTransactionalCurrency());
		
		accountOperationService.create(accountOperationToTransfer);
		
		/*if((accountOperation.getTransactionCategory() == OperationCategoryEnum.CREDIT && occTemplate.getCode().equals("DBT_TRS") ) ||
				(accountOperation.getTransactionCategory() == OperationCategoryEnum.DEBIT && occTemplate.getCode().equals("CRD_TRS")) ) {
			try {
				List<Long> operationIds = new ArrayList<>();
				operationIds.add(accountOperation.getId());
				operationIds.add(accountOperationToTransfer.getId());
				matchingCodeService.matchOperations(accountOperation.getCustomerAccount().getId(), null, operationIds, null);
			} catch (Exception e) {
				throw new BusinessException(e.getMessage(), e);
			}
		}*/
	}
	/**
	 * Check that sum of amount are lower of equal to source’s unmatched amount
	 * @param accountOperation : account operation
	 * @param amountsTransferDto : amounts to transfer
	 * @throws BadRequestException if sum of amount are greater to source’s unmatched amount
	 */
	private void checkAmountsToTransfer(AccountOperation accountOperation, AmountsTransferDto amountsTransferDto) {
		log.info("check if sum of amount are lower of equal to source’s unmatched amount : start");
		BigDecimal sumOfAmounts = BigDecimal.ZERO;
		for (AmountToTransferDto amountToTransferDto : amountsTransferDto.getAmountsToTransfer()) {
			sumOfAmounts = sumOfAmounts.add(amountToTransferDto.getAmount());
		}
		if (sumOfAmounts.compareTo(accountOperation.getUnMatchingAmount()) > 0) {
			log.info("check if sum of amount are lower of equal to source’s unmatched amount : error, sum are greater");
			throw new BadRequestException("Sum of dispatched amounts must be lower or equal to source account operation’s unmatched amount : " + accountOperation.getUnMatchingAmount().doubleValue());
		}
		log.info("check if sum of amount are lower of equal to source’s unmatched amount : sum are lower");
	}
	/**
	 * Check that all customer accounts exist
	 * @param amountsTransferDto : amounts to transfer
	 * @throws EntityDoesNotExistsException if customer account does not exist
	 */
	private void checkCustomerAccountsExist(AmountsTransferDto amountsTransferDto) {
		log.info("check if all customer exist : start");
			for (AmountToTransferDto amountToTransferDto : amountsTransferDto.getAmountsToTransfer()) {
				if (amountToTransferDto.getCustomerAccount() != null) {
					org.meveo.model.payments.CustomerAccount customerAccountToTransfer = getCustomerAccount(amountToTransferDto.getCustomerAccount());
					if (customerAccountToTransfer == null) {
						log.error("check if all customer exist : rollback ");
						throw new EntityDoesNotExistsException(CustomerAccount.class, amountToTransferDto.getCustomerAccount().getCode());
					}
				}else {
					throw new BadRequestException("Customer account is required");
				}
			}
		log.info("check if all customer exist : all customer exist");
	}
	/*
	 * Check that all customer accounts have the same currency
	 * @param customerAccount : customer account
	 * @param amountsTransferDto : amounts to transfer
	 * @throws BadRequestException if customer accounts have different currencies
	 */
	private void checkCustomerAccountsCurrency(org.meveo.model.payments.CustomerAccount customerAccount, AmountsTransferDto amountsTransferDto) {
		log.info("check if all customer have same currency : start");
		TradingCurrency tradingCurrency = customerAccount.getTradingCurrency();
		for (AmountToTransferDto amountToTransferDto : amountsTransferDto.getAmountsToTransfer()) {
			org.meveo.model.payments.CustomerAccount customerAccountToTransfer = getCustomerAccount(amountToTransferDto.getCustomerAccount());
			if (customerAccountToTransfer != null) {
				if (!tradingCurrency.getId().equals(customerAccountToTransfer.getTradingCurrency().getId())) {
					log.info("check if all customer have same currency : error customer accounts : " + customerAccountToTransfer.getCode() + "have different currencies : ");
					throw new BadRequestException("Customer accounts have different currencies");
				}
				
			}
		}
		log.info("check if all customer have same currency : all customer have same currency");
	}
	
	public void closeOperations(List<Long> aoIdToBeClosed) {
		List<AccountOperation> accountOperations = accountOperationService.findByIds(aoIdToBeClosed, List.of("matchingAmounts", "customerAccount"));
		if(CollectionUtils.isEmpty(accountOperations)) {
			throw new EntityDoesNotExistsException("No entity found with ids : " + aoIdToBeClosed);
		}
		// check if all account has code INT_ADV
		List<AccountOperation> accountOperationsWithCode = accountOperations.stream()
				.filter(accountOperation -> accountOperation.getCode().contentEquals("INV_ADV"))
				.collect(Collectors.toList());
		if(CollectionUtils.isEmpty(accountOperationsWithCode)) {
			throw new BusinessApiException("Only Account Operation having the code INV_ADV can be closed");
		}
		// check if all account operations have INT_ADV and its matching status are OPEN or PARTIAL
		List<AccountOperation> accountOperationsToBeClosed = accountOperationsWithCode.stream()
				.filter(accountOperation -> accountOperation.getMatchingStatus() == MatchingStatusEnum.O || accountOperation.getMatchingStatus() == MatchingStatusEnum.P)
				.collect(Collectors.toList());
		if(CollectionUtils.isEmpty(accountOperationsToBeClosed)) {
			throw new BusinessApiException("Only Account Operation having matchingStatus = Open or partially matched can be closed");
		}
		accountOperationService.closeAccountOperations(accountOperationsToBeClosed.stream().map(AccountOperation::getId).collect(Collectors.toList()));
		
		
	}
	
	private void checkIncompatibleAccountOperationTypes(List<AccountOperation> accountOperations) {
			if (accountOperations.size() <= 1) {
				return;
			}
			Map<String, Set<String>> validMatches = Map.of(
					"INV_ADV", Set.of("PAY_ADV", "CLOSED_ADV"),
					"PAY_ADV", Set.of("INV_ADV", "REJ_ADV"),
					"REJ_ADV", Set.of("PAY_ADV"),
					"CLOSED_ADV", Set.of("INV_ADV")
			);
			
			String firstCode = accountOperations.get(0).getCode();
			Set<String> validCodes = validMatches.get(firstCode);
			
			if (validCodes == null) {
				return;
			}
			
			boolean allMatch = accountOperations.stream()
					.skip(1)
					.allMatch(ao -> validCodes.contains(ao.getCode()));
			
			if (!allMatch) {
				throw new BusinessException("Incompatible account operation types. " + firstCode + " account operations can only be matched with " + validCodes + "  account operations");
			}
		}
}
