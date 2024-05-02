/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.api.payment;

import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.meveo.apiv2.payments.ImmutableRejectionGroup.builder;
import static org.meveo.apiv2.payments.SequenceActionType.DOWN;
import static org.meveo.apiv2.payments.SequenceActionType.UP;
import static org.meveo.commons.utils.StringUtils.isBlank;
import static org.meveo.model.payments.MatchingStatusEnum.O;
import static org.meveo.model.payments.PaymentStatusEnum.REJECTED;
import static org.meveo.model.payments.RejectedType.MANUAL;
import static org.meveo.service.payments.impl.PaymentRejectionCodeService.ENCODED_FILE_RESULT_LABEL;
import static org.meveo.service.payments.impl.PaymentRejectionCodeService.EXPORT_SIZE_RESULT_LABEL;
import static org.meveo.service.payments.impl.PaymentRejectionCodeService.FILE_PATH_RESULT_LABEL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.NoAllOperationUnmatchedException;
import org.meveo.admin.exception.UnbalanceAmountException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.payment.AccountOperationDto;
import org.meveo.api.dto.payment.AccountOperationsDto;
import org.meveo.api.dto.payment.PayByCardOrSepaDto;
import org.meveo.api.dto.payment.PaymentDto;
import org.meveo.api.dto.payment.PaymentHistoriesDto;
import org.meveo.api.dto.payment.PaymentHistoryDto;
import org.meveo.api.dto.payment.PaymentResponseDto;
import org.meveo.api.dto.response.CustomerPaymentsResponse;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.api.dto.response.PagingAndFiltering.SortOrder;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.security.Interceptor.SecuredBusinessEntityMethodInterceptor;
import org.meveo.api.security.config.annotation.FilterProperty;
import org.meveo.api.security.config.annotation.FilterResults;
import org.meveo.api.security.config.annotation.SecureMethodParameter;
import org.meveo.api.security.config.annotation.SecuredBusinessEntityMethod;
import org.meveo.api.security.filter.ListFilter;
import org.meveo.apiv2.generic.exception.ConflictException;
import org.meveo.apiv2.models.Resource;
import org.meveo.apiv2.payments.ClearingResponse;
import org.meveo.apiv2.payments.ImmutableClearingResponse;
import org.meveo.apiv2.payments.ImmutableRejectionCodesExportResult;
import org.meveo.apiv2.payments.ImportRejectionCodeInput;
import org.meveo.apiv2.payments.PaymentGatewayInput;
import org.meveo.apiv2.payments.RejectionAction;
import org.meveo.apiv2.payments.RejectionCode;
import org.meveo.apiv2.payments.RejectionCodeClearInput;
import org.meveo.apiv2.payments.RejectionCodesExportResult;
import org.meveo.apiv2.payments.RejectionGroup;
import org.meveo.apiv2.payments.RejectionPayment;
import org.meveo.apiv2.payments.SequenceAction;
import org.meveo.apiv2.payments.resource.RejectionActionMapper;
import org.meveo.apiv2.payments.resource.RejectionCodeMapper;
import org.meveo.apiv2.payments.resource.RejectionGroupMapper;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.ExchangeRate;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.crm.Customer;
import org.meveo.model.crm.custom.CustomFieldInheritanceEnum;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.AutomatedPayment;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.MatchingAmount;
import org.meveo.model.payments.MatchingTypeEnum;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.model.payments.OtherCreditAndCharge;
import org.meveo.model.payments.Payment;
import org.meveo.model.payments.PaymentGateway;
import org.meveo.model.payments.PaymentHistory;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.model.payments.PaymentRejectionAction;
import org.meveo.model.payments.PaymentRejectionActionReport;
import org.meveo.model.payments.PaymentRejectionCode;
import org.meveo.model.payments.PaymentRejectionCodesGroup;
import org.meveo.model.payments.PaymentStatusEnum;
import org.meveo.model.payments.RecordedInvoice;
import org.meveo.model.payments.RejectedPayment;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.billing.impl.JournalService;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.ImportRejectionCodeConfig;
import org.meveo.service.payments.impl.MatchingCodeService;
import org.meveo.service.payments.impl.OCCTemplateService;
import org.meveo.service.payments.impl.PaymentGatewayService;
import org.meveo.service.payments.impl.PaymentHistoryService;
import org.meveo.service.payments.impl.PaymentRejectionActionReportService;
import org.meveo.service.payments.impl.PaymentRejectionActionService;
import org.meveo.service.payments.impl.PaymentRejectionCodeService;
import org.meveo.service.payments.impl.PaymentRejectionCodesGroupService;
import org.meveo.service.payments.impl.PaymentService;
import org.meveo.service.payments.impl.RecordedInvoiceService;
import org.meveo.service.script.ScriptInstanceService;

/**
 * @author Edward P. Legaspi
 * @author Youssef IZEM
 * @author melyoussoufi
 * @lastModifiedVersion 10.0
 **/
@Stateless
@Interceptors(SecuredBusinessEntityMethodInterceptor.class)
public class PaymentApi extends BaseApi {

	private static final String DEFAULT_SORT_ORDER_ID = "id";
	private static final int FIRST_ACTION_SEQUENCE = 0;

	@Inject
    private PaymentService paymentService;

    @Inject
    private AccountOperationService accountOperationService;

    @Inject
    private RecordedInvoiceService recordedInvoiceService;

    @Inject
    private MatchingCodeService matchingCodeService;

    @Inject
    private CustomerAccountService customerAccountService;

    @Inject
    private OCCTemplateService oCCTemplateService;

    @Inject
    private PaymentHistoryService paymentHistoryService;

	@Inject
	private JournalService journalService;

	@Inject
	private PaymentGatewayService paymentGatewayService;

	@Inject
	private PaymentRejectionCodeService rejectionCodeService;

	@Inject
	private PaymentRejectionActionService paymentRejectionActionService;

	@Inject
	private ScriptInstanceService scriptInstanceService;

	@Inject
	private PaymentRejectionCodesGroupService paymentRejectionCodesGroupService;

	@Inject
	private PaymentRejectionActionReportService paymentRejectionActionReportService;

	private static final String PAYMENT_GATEWAY_NOT_FOUND_ERROR_MESSAGE = "Payment gateway not found";
	private static final String PAYMENT_REJECTION_CODE_NOT_FOUND_ERROR_MESSAGE = "Payment rejection code not found";
	private final RejectionCodeMapper rejectionCodeMapper = new RejectionCodeMapper();
	private final RejectionActionMapper rejectionActionMapper = new RejectionActionMapper();
	private final RejectionGroupMapper groupMapper = new RejectionGroupMapper();

	/**
     * @param paymentDto payment object which encapsulates the input data sent by client
     * @return the id of payment if created successful otherwise null
     * @throws NoAllOperationUnmatchedException no all operation un matched exception
     * @throws UnbalanceAmountException balance amount exception
     * @throws BusinessException business exception
     * @throws MeveoApiException opencell api exception
     */
    public Long createPayment(PaymentDto paymentDto) throws NoAllOperationUnmatchedException, UnbalanceAmountException, BusinessException, MeveoApiException {
        log.info("create payment for amount:" + paymentDto.getAmount() + " paymentMethodEnum:" + paymentDto.getPaymentMethod() + " isToMatching:" + paymentDto.isToMatching()
                + "  customerAccount:" + paymentDto.getCustomerAccountCode() + "...");

        if (isBlank(paymentDto.getAmount())) {
            missingParameters.add("amount");
        }
        if (isBlank(paymentDto.getOccTemplateCode())) {
            missingParameters.add("occTemplateCode");
        }
        if (isBlank(paymentDto.getReference())) {
            missingParameters.add("reference");
        }
        if (isBlank(paymentDto.getPaymentMethod())) {
            missingParameters.add("paymentMethod");
        }
        handleMissingParameters();

        CustomerAccount customerAccount = customerAccountService.findByCode(paymentDto.getCustomerAccountCode());
        OCCTemplate occTemplate = oCCTemplateService.findByCode(paymentDto.getOccTemplateCode());
        if (occTemplate == null) {
            throw new BusinessException("Cannot find OCC Template with code=" + paymentDto.getOccTemplateCode());
        }
        if (!occTemplate.isManualCreationEnabled()) {
            throw new BusinessException(format("Creation is prohibited; occTemplate %s is not allowed for manual creation", paymentDto.getOccTemplateCode()));
        }

        Payment payment = new Payment();
		paymentService.calculateAmountsByTransactionCurrency(payment, customerAccount,
				paymentDto.getAmount(), paymentDto.getTransactionalCurrency(), payment.getTransactionDate());

		payment.setJournal(occTemplate.getJournal());
        payment.setPaymentMethod(paymentDto.getPaymentMethod());
        payment.setAccountingCode(occTemplate.getAccountingCode());
        payment.setCode(occTemplate.getCode());
        payment.setDescription(isBlank(paymentDto.getDescription()) ? occTemplate.getDescription() : paymentDto.getDescription());
        payment.setTransactionCategory(occTemplate.getOccCategory());
        payment.setAccountCodeClientSide(occTemplate.getAccountCodeClientSide());
        payment.setCustomerAccount(customerAccount);
        payment.setReference(paymentDto.getReference());
        payment.setDueDate(paymentDto.getDueDate() == null ? new Date() : paymentDto.getDueDate());
        payment.setTransactionDate(paymentDto.getTransactionDate() == null ? new Date() : paymentDto.getTransactionDate());
        payment.setMatchingStatus(O);
        payment.setPaymentOrder(paymentDto.getPaymentOrder());
        payment.setFees(paymentDto.getFees());
        payment.setComment(paymentDto.getComment());
        payment.setBankLot(paymentDto.getBankLot());
        payment.setPaymentInfo(paymentDto.getPaymentInfo());
        payment.setPaymentInfo1(paymentDto.getPaymentInfo1());
        payment.setPaymentInfo2(paymentDto.getPaymentInfo2());
        payment.setPaymentInfo3(paymentDto.getPaymentInfo3());
        payment.setPaymentInfo4(paymentDto.getPaymentInfo4());
        payment.setPaymentInfo5(paymentDto.getPaymentInfo5());
        payment.setPaymentInfo6(paymentDto.getPaymentInfo6());
        payment.setBankCollectionDate(paymentDto.getBankCollectionDate());
		payment.setCollectionDate(paymentDto.getCollectionDate() == null ? paymentDto.getBankCollectionDate() : paymentDto.getCollectionDate());
		payment.setAccountingDate(new Date());
		accountOperationService.handleAccountingPeriods(payment);

        // populate customFields
        try {
            populateCustomFields(paymentDto.getCustomFields(), payment, true);
        } catch (MissingParameterException | InvalidParameterException e) {
            log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }

		payment.setJournal(journalService.findByCode("BAN"));
		setPaymentGateway(payment, customerAccount);
		paymentService.create(payment);

		paymentHistoryService.addHistory(customerAccount,
				payment,
				null, paymentDto.getAmount().multiply(new BigDecimal(100)).longValue(),
				PaymentStatusEnum.ACCEPTED, null, null, payment.getReference(), null, null,
				null,null,paymentDto.getListAoIdsForMatching());
		if (paymentDto.isToMatching()) {
			matchPayment(paymentDto, customerAccount, payment);
        } else {
            log.info("no matching created ");
        }
        log.debug("payment created for amount:" + payment.getAmount());

        return payment.getId();

    }

	private void setPaymentGateway(Payment payment, CustomerAccount customerAccount) {
		if(payment.getPaymentGateway() == null && payment.getCustomerAccount() != null) {
			PaymentMethod preferredPaymentMethod = payment.getCustomerAccount().getPreferredPaymentMethod();
			PaymentGateway paymentGateway =
					paymentGatewayService.getPaymentGateway(customerAccount, preferredPaymentMethod, null);
			ofNullable(paymentGateway).ifPresent(payment::setPaymentGateway);
		}
	}

	private ExchangeRate getExchangeRate(TradingCurrency tradingCurrency, TradingCurrency functionalCurrency, Date transactionDate) {
		Date exchangeDate = transactionDate != null ? transactionDate : new Date();
		ExchangeRate exchangeRate = tradingCurrency.getExchangeRate(exchangeDate);
		if (exchangeRate == null || exchangeRate.getExchangeRate() == null) {
			throw new EntityDoesNotExistsException("No valid exchange rate found for currency " + tradingCurrency.getCurrencyCode()
					+ " on " + exchangeDate);
		}
		return exchangeRate;
	}

	private void checkTransactionalCurrency(String transactionalcurrency, TradingCurrency tradingCurrency) {
		if (tradingCurrency == null || isBlank(tradingCurrency)) {
			throw new InvalidParameterException("Currency " + transactionalcurrency +
					" is not recorded a trading currency in Opencell. Only currencies declared as trading currencies can be used to record account operations.");
		}
	}


	private void matchPayment(PaymentDto paymentDto, CustomerAccount customerAccount, Payment payment)
			throws BusinessApiException, BusinessException, NoAllOperationUnmatchedException, UnbalanceAmountException {
		List<Long> listReferenceToMatch = new ArrayList<>();
		if (paymentDto.getListAoIdsForMatching()!=null && !paymentDto.getListAoIdsForMatching().isEmpty() ) {
			listReferenceToMatch.addAll(paymentDto.getListAoIdsForMatching());
		} else if (paymentDto.getListOCCReferenceforMatching() != null) {
		    for (String Reference: paymentDto.getListOCCReferenceforMatching()) {
		        List<RecordedInvoice> accountOperationToMatch = recordedInvoiceService.getRecordedInvoice(Reference);
		        if (accountOperationToMatch == null || accountOperationToMatch.isEmpty()) {
		            throw new BusinessApiException("Cannot find account operation with reference:" + Reference );
		        } else if (accountOperationToMatch.size() > 1) {
		            throw new BusinessApiException("More than one account operation with reference:" + Reference +". Please use ListAoIdsForMatching instead of ListOCCReferenceforMatching");
		        }
		        listReferenceToMatch.add(accountOperationToMatch.get(0).getId());
		    }
		}
		List<AccountOperation> aosToPaid = new ArrayList<>();
		for(Long id : listReferenceToMatch ) {
			AccountOperation ao = accountOperationService.findById(id);
			if(ao == null) {
				 throw new BusinessApiException("Cannot find account operation with id:" + id );
			}
			aosToPaid.add(ao);
		}
		 Collections.sort(aosToPaid, comparing(AccountOperation::getDueDate));
		if(checkAccountOperationCurrency(aosToPaid, paymentDto.getTransactionalCurrency())) {
			throw new BusinessApiException("Transaction currency is different from account operation currency");
		}
		for(AccountOperation ao :aosToPaid ) {
			if(ZERO.compareTo(payment.getUnMatchingAmount()) == 0) {
				break;
			}
			List<Long> aosIdsToMatch = new ArrayList<>();
			aosIdsToMatch.add(ao.getId());
			aosIdsToMatch.add(payment.getId());
			matchingCodeService.matchOperations(null, customerAccount.getCode(), aosIdsToMatch, payment.getId(), MatchingTypeEnum.A);
		}
	}

	private boolean checkAccountOperationCurrency(List<AccountOperation> aosToPaid, String transactionalCurrency) {
		return aosToPaid.stream()
				.anyMatch(accountOperation -> ! accountOperation.getCode().endsWith("_SD") &&
						!accountOperation.getTransactionalCurrency().getCurrencyCode().equalsIgnoreCase(transactionalCurrency));
	}


	/**
	 * Get payment list by customer account code
	 * 
	 * @param customerAccountCode customer account code
	 * @param pagingAndFiltering
	 * @return list of payment dto
	 * @throws Exception exception.
	 * @author akadid abdelmounaim
	 * @lastModifiedVersion 5.0
	 */

    @SecuredBusinessEntityMethod(validate = @SecureMethodParameter(entityClass = CustomerAccount.class))
	public CustomerPaymentsResponse getPaymentList(String customerAccountCode, PagingAndFiltering pagingAndFiltering) throws Exception {

		CustomerPaymentsResponse result = new CustomerPaymentsResponse();
		CustomerAccount customerAccount = customerAccountService.findByCode(customerAccountCode);

		if (customerAccount == null) {
			throw new EntityDoesNotExistsException(CustomerAccount.class, customerAccountCode);
		}

		if (pagingAndFiltering == null) {
			pagingAndFiltering = new PagingAndFiltering();
		}
		PaginationConfiguration paginationConfiguration = preparePaginationConfiguration(pagingAndFiltering, customerAccount);

		List<AccountOperation> ops = accountOperationService.list(paginationConfiguration);
		Long total = accountOperationService.count(paginationConfiguration);

		// Remove the filters added by preparePaginationConfiguration function
		pagingAndFiltering.getFilters().remove("type_class");
		pagingAndFiltering.getFilters().remove("customerAccount");

		pagingAndFiltering.setTotalNumberOfRecords(total.intValue());
		result.setPaging(pagingAndFiltering);

		for (AccountOperation op : ops) {
			if (op instanceof Payment) {
				Payment p = (Payment) op;
				PaymentDto paymentDto = new PaymentDto();
				paymentDto.setType(p.getType());
				paymentDto.setAmount(p.getAmount());
				paymentDto.setDueDate(p.getDueDate());
				paymentDto.setOccTemplateCode(p.getCode());
				paymentDto.setPaymentMethod(p.getPaymentMethod());
				paymentDto.setReference(p.getReference());
				paymentDto.setTransactionDate(p.getTransactionDate());
				paymentDto.setPaymentOrder(p.getPaymentOrder());
				paymentDto.setFees(p.getFees());
				paymentDto.setComment(p.getComment());
				paymentDto.setCustomFields(entityToDtoConverter.getCustomFieldsDTO(op, CustomFieldInheritanceEnum.INHERIT_NO_MERGE));
				if (p instanceof AutomatedPayment) {
					AutomatedPayment ap = (AutomatedPayment) p;
					paymentDto.setBankCollectionDate(ap.getBankCollectionDate());
					paymentDto.setBankLot(ap.getBankLot());
					paymentDto.setDepositDate(ap.getDepositDate());
				}
				result.addPaymentDto(paymentDto);
			} else if (op instanceof OtherCreditAndCharge) {
				OtherCreditAndCharge occ = (OtherCreditAndCharge) op;
				PaymentDto paymentDto = new PaymentDto();
				paymentDto.setType(occ.getType());
				paymentDto.setDescription(op.getDescription());
				paymentDto.setAmount(occ.getAmount());
				paymentDto.setDueDate(occ.getDueDate());
				paymentDto.setOccTemplateCode(occ.getCode());
				paymentDto.setReference(occ.getReference());
				paymentDto.setTransactionDate(occ.getTransactionDate());
				result.addPaymentDto(paymentDto);
			}
		}
		return result;
	}

	/**
	 * Prepare paginationConfiguration to get only Payment and OtherCreditAndCharge
	 * operations related to the customerAccount
	 * 
	 * @param pagingAndFiltering
	 * @param customerAccount
	 * @return
	 * @throws Exception
	 */
	private PaginationConfiguration preparePaginationConfiguration(PagingAndFiltering pagingAndFiltering, CustomerAccount customerAccount) throws Exception {

		PaginationConfiguration paginationConfiguration = toPaginationConfiguration(DEFAULT_SORT_ORDER_ID, SortOrder.ASCENDING, null, pagingAndFiltering, AccountOperation.class);

		List<String> classFilter = Arrays.asList("org.meveo.model.payments.Payment", "org.meveo.model.payments.AutomatedPayment", "org.meveo.model.payments.OtherCreditAndCharge");
		pagingAndFiltering.addFilter("customerAccount", customerAccount);
		pagingAndFiltering.addFilter("type_class", classFilter);

		return paginationConfiguration;
	}

	/**
	 * @param customerAccountCode customer account code
	 * @return balance for customer account
	 * @throws BusinessException business exception
	 */
    @SecuredBusinessEntityMethod(validate = @SecureMethodParameter(entityClass = CustomerAccount.class))
	public double getBalance(String customerAccountCode) throws BusinessException {

		CustomerAccount customerAccount = customerAccountService.findByCode(customerAccountCode);

		return customerAccountService.customerAccountBalanceDue(customerAccount, new Date()).doubleValue();
	}

	
	
	
	/**
	 * 
	 * @param sepaPaymentRequestDto
	 * @return
	 * @throws Exception
	 */
	public PaymentResponseDto payBySepa(PayByCardOrSepaDto sepaPaymentRequestDto)
			throws Exception {

		if (isBlank(sepaPaymentRequestDto.getCtsAmount())) {
			missingParameters.add("ctsAmount");
		}

		if (isBlank(sepaPaymentRequestDto.getCustomerAccountCode())) {
			missingParameters.add("customerAccountCode");
		}

		if (sepaPaymentRequestDto.isToMatch() && sepaPaymentRequestDto.getAoToPay() == null
				|| sepaPaymentRequestDto.getAoToPay().isEmpty()) {
			missingParameters.add("aoToPay");
		}

		handleMissingParameters();

		CustomerAccount customerAccount = customerAccountService
				.findByCode(sepaPaymentRequestDto.getCustomerAccountCode());
		if (customerAccount == null) {
			throw new EntityDoesNotExistsException(CustomerAccount.class,
					sepaPaymentRequestDto.getCustomerAccountCode());
		}

		PaymentMethodEnum preferedMethod = customerAccount.getPreferredPaymentMethodType();
		if (preferedMethod != null && PaymentMethodEnum.DIRECTDEBIT != preferedMethod) {
			throw new BusinessApiException("Can not process payment as prefered payment method is " + preferedMethod);
		}

		return paymentService.payByMandat(customerAccount, sepaPaymentRequestDto.getCtsAmount(),
				sepaPaymentRequestDto.getAoToPay(), sepaPaymentRequestDto.isCreateAO(),
				sepaPaymentRequestDto.isToMatch(), null);
	}

	
	/**
	 * @param cardPaymentRequestDto card payment request
	 * @return payment by card response
	 * @throws Exception 
	 */
	public PaymentResponseDto payByCard(PayByCardOrSepaDto cardPaymentRequestDto)
			throws Exception {

		if (isBlank(cardPaymentRequestDto.getCtsAmount())) {
			missingParameters.add("ctsAmount");
		}

		if (isBlank(cardPaymentRequestDto.getCustomerAccountCode())) {
			missingParameters.add("customerAccountCode");
		}
		boolean useCard = false;

		// case card payment
		if (!isBlank(cardPaymentRequestDto.getCardNumber())) {
			useCard = true;
			if (isBlank(cardPaymentRequestDto.getCvv())) {
				missingParameters.add("cvv");
			}
			if (isBlank(cardPaymentRequestDto.getExpiryDate()) || cardPaymentRequestDto.getExpiryDate().length() != 4
					|| !org.apache.commons.lang3.StringUtils.isNumeric(cardPaymentRequestDto.getExpiryDate())) {

				missingParameters.add("expiryDate");
			}
			if (isBlank(cardPaymentRequestDto.getOwnerName())) {
				missingParameters.add("ownerName");
			}
			if (isBlank(cardPaymentRequestDto.getCardType())) {
				missingParameters.add("cardType");
			}
		}
		if (cardPaymentRequestDto.isToMatch() && cardPaymentRequestDto.getAoToPay() == null || cardPaymentRequestDto.getAoToPay().isEmpty()) {			
				missingParameters.add("aoToPay");			
		}

		handleMissingParameters();

		CustomerAccount customerAccount = customerAccountService.findByCode(cardPaymentRequestDto.getCustomerAccountCode());
		if (customerAccount == null) {
			throw new EntityDoesNotExistsException(CustomerAccount.class, cardPaymentRequestDto.getCustomerAccountCode());
		}

		PaymentMethodEnum preferedMethod = customerAccount.getPreferredPaymentMethodType();
		if (preferedMethod != null && PaymentMethodEnum.CARD != preferedMethod) {
			throw new BusinessApiException("Can not process payment as prefered payment method is " + preferedMethod);
		}

		PaymentResponseDto doPaymentResponseDto = null;
		if (useCard) {

			doPaymentResponseDto = paymentService.payByCard(customerAccount, cardPaymentRequestDto.getCtsAmount(), cardPaymentRequestDto.getCardNumber(),
					cardPaymentRequestDto.getOwnerName(), cardPaymentRequestDto.getCvv(), cardPaymentRequestDto.getExpiryDate(), cardPaymentRequestDto.getCardType(),
					cardPaymentRequestDto.getAoToPay(), cardPaymentRequestDto.isCreateAO(), cardPaymentRequestDto.isToMatch(), null);
		} else {
			doPaymentResponseDto = paymentService.payByCardToken(customerAccount, cardPaymentRequestDto.getCtsAmount(), cardPaymentRequestDto.getAoToPay(),
					cardPaymentRequestDto.isCreateAO(), cardPaymentRequestDto.isToMatch(), null);
		}

		return doPaymentResponseDto;
	}

	/**
	 * List payment histories matching filtering and query criteria
	 * 
	 * @param pagingAndFiltering Paging and filtering criteria.
	 * @return A list of payment history
	 * @throws InvalidParameterException invalid parameter exception
	 */
	@SecuredBusinessEntityMethod(resultFilter = ListFilter.class)
	@FilterResults(propertyToFilter = "paymentHistories", itemPropertiesToFilter = {
			@FilterProperty(property = "sellerCode", entityClass = Seller.class, allowAccessIfNull = false),
			@FilterProperty(property = "customerAccountCode", entityClass = CustomerAccount.class, allowAccessIfNull = false),
			@FilterProperty(property = "customerCode", entityClass = Customer.class, allowAccessIfNull = false) })
	public PaymentHistoriesDto list(PagingAndFiltering pagingAndFiltering) throws InvalidParameterException {
		PaginationConfiguration paginationConfig = toPaginationConfiguration("id", SortOrder.ASCENDING, Arrays.asList("payment", "refund"), pagingAndFiltering,
				PaymentHistory.class);
		Long totalCount = paymentHistoryService.count(paginationConfig);
		PaymentHistoriesDto paymentHistoriesDto = new PaymentHistoriesDto();
		paymentHistoriesDto.setPaging(pagingAndFiltering != null ? pagingAndFiltering : new PagingAndFiltering());
		paymentHistoriesDto.getPaging().setTotalNumberOfRecords(totalCount.intValue());

		if (totalCount > 0) {
			List<PaymentHistory> paymentHistories = paymentHistoryService.list(paginationConfig);
			for (PaymentHistory paymentHistory : paymentHistories) {
				paymentHistoriesDto.getPaymentHistories().add(fromEntity(paymentHistory));
			}
		}
		return paymentHistoriesDto;
	}

	/**
	 * Return list AO matched with a payment or refund
	 * 
	 * @param paymentOrRefund
	 * @return list AO matched
	 */
	private List<AccountOperationDto> getAosPaidByPayment(AccountOperation paymentOrRefund) {
		List<AccountOperationDto> result = new ArrayList<AccountOperationDto>();
		if (paymentOrRefund == null) {
			return result;
		}
		if (paymentOrRefund.getMatchingAmounts() != null && !paymentOrRefund.getMatchingAmounts().isEmpty()) {
			for (MatchingAmount ma : paymentOrRefund.getMatchingAmounts().get(0).getMatchingCode().getMatchingAmounts()) {
				if (ma.getAccountOperation().getTransactionCategory() != paymentOrRefund.getTransactionCategory()) {
					result.add(new AccountOperationDto(ma.getAccountOperation(),
							entityToDtoConverter.getCustomFieldsDTO(ma.getAccountOperation(), CustomFieldInheritanceEnum.INHERIT_NO_MERGE)));
				}
			}
		}
		return result;
	}

	public PaymentHistoryDto fromEntity(PaymentHistory paymentHistory) {
		return fromEntity(paymentHistory, true);
	}

	/**
	 * Build paymentHistory dto from entity
	 * 
	 * @param paymentHistory payment History
	 * @return PaymentHistoryDto
	 */
	public PaymentHistoryDto fromEntity(PaymentHistory paymentHistory, boolean isIncludedAoToPay) {
		PaymentHistoryDto paymentHistoryDto = new PaymentHistoryDto();
		paymentHistoryDto.setAuditableEntity(paymentHistory);
		paymentHistoryDto.setCustomerAccountCode(paymentHistory.getCustomerAccountCode());
		paymentHistoryDto.setCustomerAccountName(paymentHistory.getCustomerAccountName());
		paymentHistoryDto.setSellerCode(paymentHistory.getSellerCode());
		paymentHistoryDto.setCustomerCode(paymentHistory.getCustomerCode());
		paymentHistoryDto.setAmountCts(paymentHistory.getAmountCts());
		paymentHistoryDto.setAsyncStatus(paymentHistory.getAsyncStatus());
		paymentHistoryDto.setErrorCode(paymentHistory.getErrorCode());
		paymentHistoryDto.setErrorMessage(paymentHistory.getErrorMessage());
		paymentHistoryDto.setErrorType(paymentHistory.getErrorType());
		paymentHistoryDto.setExternalPaymentId(paymentHistory.getExternalPaymentId());
		paymentHistoryDto.setOperationCategory(paymentHistory.getOperationCategory());
		paymentHistoryDto.setOperationDate(paymentHistory.getOperationDate());
		paymentHistoryDto.setPaymentGatewayCode(paymentHistory.getPaymentGatewayCode());
		paymentHistoryDto.setPaymentMethodName(paymentHistory.getPaymentMethodName());
		paymentHistoryDto.setPaymentMethodType(paymentHistory.getPaymentMethodType());
		if (paymentHistory.getRefund() != null) {
			paymentHistoryDto.setRefund(new AccountOperationDto(paymentHistory.getRefund(),
					entityToDtoConverter.getCustomFieldsDTO(paymentHistory.getRefund(), CustomFieldInheritanceEnum.INHERIT_NO_MERGE)));
		}
		if (paymentHistory.getPayment() != null) {
			paymentHistoryDto.setPayment(new AccountOperationDto(paymentHistory.getPayment(),
					entityToDtoConverter.getCustomFieldsDTO(paymentHistory.getPayment(), CustomFieldInheritanceEnum.INHERIT_NO_MERGE)));
		}
		paymentHistoryDto.setSyncStatus(paymentHistory.getSyncStatus());
		paymentHistoryDto.setStatus(paymentHistory.getStatus());
		paymentHistoryDto.setLastUpdateDate(paymentHistory.getLastUpdateDate());
		if (isIncludedAoToPay) {
			AccountOperationsDto accountOperationsDto = new AccountOperationsDto();
			// Backward compatibility
			if (paymentHistory.getListAoPaid() == null || paymentHistory.getListAoPaid().isEmpty()) {
				accountOperationsDto.setAccountOperation(getAosPaidByPayment(paymentHistory.getRefund() == null ? paymentHistory.getPayment() : paymentHistory.getRefund()));

			} else {
				for (AccountOperation ao : paymentHistory.getListAoPaid()) {
					accountOperationsDto.getAccountOperation()
							.add(new AccountOperationDto(ao, entityToDtoConverter.getCustomFieldsDTO(ao, CustomFieldInheritanceEnum.INHERIT_NO_MERGE)));
				}
			}
			paymentHistoryDto.setListAoPaid(accountOperationsDto);
		}

		return paymentHistoryDto;
    }

	/**
	 * Create payment rejection code
	 *
	 * @param rejectionCode payment rejection code
	 * @return RejectionCode id
	 */
	public Long createPaymentRejectionCode(RejectionCode rejectionCode) {
		PaymentGateway paymentGateway =
				rejectionCode.getPaymentGateway() != null ? loadPaymentGateway(rejectionCode.getPaymentGateway()) : null;
		if (paymentGateway == null) {
			throw new NotFoundException(PAYMENT_GATEWAY_NOT_FOUND_ERROR_MESSAGE);
		}
		PaymentRejectionCode paymentRejectionCode = rejectionCodeMapper.toEntity(rejectionCode, paymentGateway);
		if(isBlank(paymentRejectionCode.getCode())) {
			paymentRejectionCode.setCode(ofNullable(getGenericCode(PaymentRejectionCode.class.getName()))
					.orElseThrow(() -> new MissingParameterException("Code is missing")));
		}
		rejectionCodeService.create(paymentRejectionCode);
		return paymentRejectionCode.getId();

	}

	private PaymentGateway loadPaymentGateway(Resource paymentGatewayResource) {
		PaymentGateway paymentGateway;
		if (paymentGatewayResource.getId() != null) {
			paymentGateway = paymentGatewayService.findById(paymentGatewayResource.getId());
			if (paymentGateway == null && paymentGatewayResource.getCode() != null) {
				paymentGateway = paymentGatewayService.findByCode(paymentGatewayResource.getCode());
			}
		} else {
			paymentGateway = paymentGatewayService.findByCode(paymentGatewayResource.getCode());
		}
		return paymentGateway;
	}

	/**
	 * Update payment rejection code
	 *
	 * @param id       payment rejection code id
	 * @param resource payment rejection code
	 * @return RejectionCode updated result
	 */
	public RejectionCode updatePaymentRejectionCode(Long id, RejectionCode resource) {
		PaymentGateway paymentGateway = null;
		boolean checkCodeGatewayConstraint = false;
		if (resource.getPaymentGateway() != null) {
			paymentGateway = loadPaymentGateway(resource.getPaymentGateway());
			if (paymentGateway == null) {
				throw new NotFoundException(PAYMENT_GATEWAY_NOT_FOUND_ERROR_MESSAGE);
			}
		}
		PaymentRejectionCode rejectionCodeToUpdate = ofNullable(rejectionCodeService.findById(id))
				.orElseThrow(() -> new NotFoundException(PAYMENT_REJECTION_CODE_NOT_FOUND_ERROR_MESSAGE));
		if(resource.getCode() != null && !resource.getCode().equals(rejectionCodeToUpdate.getCode())) {
			checkCodeGatewayConstraint = true;
			rejectionCodeToUpdate.setCode(resource.getCode());
		}
		ofNullable(resource.getDescription()).ifPresent(rejectionCodeToUpdate::setDescription);
		ofNullable(resource.getDescriptionI18n()).ifPresent(rejectionCodeToUpdate::setDescriptionI18n);
		if (paymentGateway != null && rejectionCodeToUpdate.getPaymentGateway() != null
				&& !paymentGateway.getId().equals(rejectionCodeToUpdate.getPaymentGateway().getId())) {
			checkCodeGatewayConstraint = true;
			rejectionCodeToUpdate.setPaymentGateway(paymentGateway);
		}
		if(checkCodeGatewayConstraint) {
			rejectionCodeService.findByCodeAndPaymentGateway(rejectionCodeToUpdate.getCode(), rejectionCodeToUpdate.getPaymentGateway().getId())
					.ifPresent(rejectionCode -> { throw new BusinessApiException(format("Rejection code with code %s already exists in gateway %s",
							rejectionCode.getCode(), rejectionCode.getPaymentGateway().getCode()));});
		}

		return rejectionCodeMapper.toResource(rejectionCodeService.update(rejectionCodeToUpdate));
	}

	/**
	 * Delete rejection code
	 *
	 * @param id payment rejection code id
	 * @param forceDelete force rejection code delete
	 */
	public void removeRejectionCode(Long id, boolean forceDelete) {
		PaymentRejectionCode rejectionCode = ofNullable(rejectionCodeService.findById(id))
				.orElseThrow(() -> new NotFoundException(PAYMENT_REJECTION_CODE_NOT_FOUND_ERROR_MESSAGE));
		if(rejectionCode.getPaymentRejectionCodesGroup() == null ||
				(rejectionCode.getPaymentRejectionCodesGroup() != null && forceDelete)) {
			PaymentRejectionCodesGroup rejectionCodesGroup = rejectionCode.getPaymentRejectionCodesGroup();
			if(rejectionCodesGroup != null
					&& rejectionCodesGroup.getPaymentRejectionCodes() != null
					&& rejectionCodesGroup.getPaymentRejectionCodes().size() == 1) {
				removeRejectionCodeGroup(rejectionCodesGroup.getId());
			} else {
				if(rejectionCodesGroup != null
						&& rejectionCodesGroup.getPaymentRejectionCodes() != null
						&& !rejectionCodesGroup.getPaymentRejectionCodes().isEmpty()) {
					rejectionCodesGroup.getPaymentRejectionCodes().remove(rejectionCode);
				}
				rejectionCodeService.remove(rejectionCode);
			}
			paymentRejectionActionReportService.getEntityManager()
						.createNamedQuery("PaymentRejectionActionReport.removeActionReference")
					.setParameter("rejectionCode", rejectionCode.getCode())
					.executeUpdate();
		} else if(rejectionCode.getPaymentRejectionCodesGroup() != null && !forceDelete) {
			throw new ConflictException("Rejection code " + rejectionCode.getCode() + " is used in a rejection codes group." +
					" Use ‘force:true’ to override. If the group becomes empty, it will be deleted too");
		}
	}

	/**
	 * Clear rejectionCodes by gateway
	 *
	 * @param rejectionCodeClearInput RejectionCodeClearInput
	 * @return ClearingResponse
	 */
	public ClearingResponse clearAll(RejectionCodeClearInput rejectionCodeClearInput) {
		PaymentGateway paymentGateway = null;
		if (rejectionCodeClearInput != null && rejectionCodeClearInput.getPaymentGateway() != null) {
			paymentGateway = ofNullable(loadPaymentGateway(rejectionCodeClearInput.getPaymentGateway()))
					.orElseThrow(() -> new NotFoundException(PAYMENT_GATEWAY_NOT_FOUND_ERROR_MESSAGE));
		}
		List<PaymentRejectionCode> rejectionCodesToRemove = paymentGateway != null
				? rejectionCodeService.findBYPaymentGateway(paymentGateway.getId())
				: rejectionCodeService.list();
		if(rejectionCodesToRemove == null || rejectionCodesToRemove.isEmpty()) {
			throw new BadRequestException("No rejection code found to clear");
		}
		int numberOfCodesToClear = rejectionCodesToRemove.size();
		rejectionCodesToRemove.forEach(paymentRejectionCode
				-> removeRejectionCode(paymentRejectionCode.getId(), rejectionCodeClearInput.getForce()));
		return buildResponse(numberOfCodesToClear, paymentGateway);
	}

	private ClearingResponse buildResponse(int clearedCodesCount, PaymentGateway paymentGateway) {
		ImmutableClearingResponse.Builder builder = ImmutableClearingResponse
				.builder()
				.status("SUCCESS")
				.clearedCodesCount(clearedCodesCount);
		if(paymentGateway != null) {
			builder.associatedPaymentGatewayCode(paymentGateway.getCode());
		}
		return builder
				.massage("Rejection codes successfully cleared")
				.build();
	}

	/**
	 * Export rejection codes by payment gateway
	 *
	 * @param paymentGatewayResource payment gateway
	 * @return RejectionCodesExportResult
	 */
	public RejectionCodesExportResult export(PaymentGatewayInput paymentGatewayResource) {
		PaymentGateway paymentGateway = null;
		if (paymentGatewayResource != null && paymentGatewayResource.getPaymentGateway() != null) {
			paymentGateway = ofNullable(loadPaymentGateway(paymentGatewayResource.getPaymentGateway()))
					.orElseThrow(() -> new NotFoundException(PAYMENT_GATEWAY_NOT_FOUND_ERROR_MESSAGE));
		}
		Map<String, Object> exportResult = rejectionCodeService.export(paymentGateway);
		return ImmutableRejectionCodesExportResult.builder()
				.exportSize((Integer) exportResult.get(EXPORT_SIZE_RESULT_LABEL))
				.fileFullPath((String) exportResult.get(FILE_PATH_RESULT_LABEL))
				.encodedFile((String) exportResult.get(ENCODED_FILE_RESULT_LABEL))
				.build();
	}

	/**
	 * Import rejection codes by payment gateway
	 *
	 * @param importRejectionCodeInput Import data
	 * @return number of imported lines
	 */
	public int importRejectionCodes(ImportRejectionCodeInput importRejectionCodeInput) {
		if (isBlank(importRejectionCodeInput.getBase64csv())) {
			throw new BusinessApiException("Encoded file should not be null or empty");
		}
		ImportRejectionCodeConfig config =
				new ImportRejectionCodeConfig(importRejectionCodeInput.getBase64csv(),
						importRejectionCodeInput.getIgnoreLanguageErrors(),
						importRejectionCodeInput.getMode());
		return rejectionCodeService.importRejectionCodes(config);
	}

	/**
	 * Create rejection action
	 *
	 * @param rejectionAction rejectionAction to create
	 */
	public RejectionAction createRejectionAction(RejectionAction rejectionAction) {
		try {
			PaymentRejectionAction entity = rejectionActionMapper.toEntity(rejectionAction);
			if (paymentRejectionActionService.findByCode(rejectionAction.getCode()) != null) {
				throw new EntityAlreadyExistsException("Payment rejection action with code "
						+ rejectionAction.getCode() + " already exists");
			}
			if (entity.getScript() != null) {
				ScriptInstance scriptInstance = entity.getScript().getId() != null
						? scriptInstanceService.findById(entity.getScript().getId())
						: scriptInstanceService.findByCode(entity.getScript().getCode());
				if (scriptInstance == null) {
					throw new NotFoundException("Script instance not found");
				}
				entity.setScript(scriptInstance);
			}
			if(rejectionAction.getRejectionCodeGroup() != null) {
				entity.setPaymentRejectionCodesGroup(loadRejectionCodeGroup(rejectionAction.getRejectionCodeGroup()));
			}
			List<PaymentRejectionAction> actions = ((entity.getPaymentRejectionCodesGroup() != null)
					&& (entity.getPaymentRejectionCodesGroup().getPaymentRejectionActions() != null)) ?
					entity.getPaymentRejectionCodesGroup().getPaymentRejectionActions() : emptyList();
			int actionCount = actions.size();
			if(rejectionAction.getSequence() != null) {
				computeSequence(entity, actionCount, actions);
			} else {
				entity.setSequence(actionCount);
			}
			paymentRejectionActionService.create(entity);
			return rejectionActionMapper.toResource(entity);
		} catch (BusinessException exception) {
			throw new BadRequestException(exception.getMessage());
		}
	}

	private void computeSequence(PaymentRejectionAction entity, int actionCount, List<PaymentRejectionAction> actions) {
		if(entity.getSequence() >= 0 && entity.getSequence() <= actionCount) {
			if(actions.isEmpty()) {
				entity.setSequence(FIRST_ACTION_SEQUENCE);
			} else {
				actions.sort(comparing(PaymentRejectionAction::getSequence));
				for (int index = entity.getSequence(); index < actions.size(); index++) {
					actions.get(index).setSequence(actions.get(index).getSequence() + 1);
				}
			}
		} else {
			throw new BadRequestException("Provided sequence should be between 0 and total action number");
		}
	}

	private PaymentRejectionCodesGroup loadRejectionCodeGroup(Resource rejectionGroup) {
		PaymentRejectionCodesGroup paymentRejectionCodesGroup = new PaymentRejectionCodesGroup();
		paymentRejectionCodesGroup.setId(rejectionGroup.getId());
		paymentRejectionCodesGroup.setCode(rejectionGroup.getCode());
		paymentRejectionCodesGroup = paymentRejectionCodesGroupService.findByIdOrCode(paymentRejectionCodesGroup);
		if (paymentRejectionCodesGroup == null) {
			throw new NotFoundException("Payment rejection code group not found");
		}
		return paymentRejectionCodesGroup;
	}

	/**
	 * Update rejection action
	 *
	 * @param id rejectionAction id
	 * @param rejectionAction updated rejectionAction
	 */
	public RejectionAction updateRejectionAction(Long id, RejectionAction rejectionAction) {
		PaymentRejectionAction toUpdate = ofNullable(paymentRejectionActionService.findById(id))
				.orElseThrow(() -> new NotFoundException("Payment rejection action not found"));
		if (rejectionAction.getCode() != null
				&& paymentRejectionActionService.findByCode(rejectionAction.getCode()) != null) {
			throw new EntityAlreadyExistsException("Payment rejection action with code "
					+ rejectionAction.getCode() + " already exists");
		}
		ofNullable(rejectionAction.getCode()).ifPresent(toUpdate::setCode);
		ofNullable(rejectionAction.getDescription()).ifPresent(toUpdate::setDescription);
		ofNullable(rejectionAction.getScriptParameters()).ifPresent(toUpdate::setScriptParameters);
		if (rejectionAction.getScriptInstance() != null) {
			final Resource script = rejectionAction.getScriptInstance();
			ScriptInstance scriptInstance = script.getId() != null
					? scriptInstanceService.findById(script.getId())
					: scriptInstanceService.findByCode(script.getCode());
			if (scriptInstance == null) {
				throw new NotFoundException("Script instance not found");
			}
			toUpdate.setScript(scriptInstance);
		}
		if(rejectionAction.getRejectionCodeGroup() != null) {
			toUpdate.setPaymentRejectionCodesGroup(loadRejectionCodeGroup(rejectionAction.getRejectionCodeGroup()));
		}
		if(rejectionAction.getSequence() != null) {
			List<PaymentRejectionAction> actions = ((toUpdate.getPaymentRejectionCodesGroup() != null)
					&& (toUpdate.getPaymentRejectionCodesGroup().getPaymentRejectionActions() != null)) ?
					toUpdate.getPaymentRejectionCodesGroup().getPaymentRejectionActions() : emptyList();
			int actionCount = actions.size();
			if(rejectionAction.getSequence() > actionCount - 1 || rejectionAction.getSequence() < 0) {
				throw new BadRequestException("Provided sequence should be between 0 and " + (actionCount - 1));
			} else {
				if(toUpdate.getSequence() != rejectionAction.getSequence()) {
					actions.sort(comparing(PaymentRejectionAction::getSequence));
					actions.get(rejectionAction.getSequence()).setSequence(toUpdate.getSequence());
					toUpdate.setSequence(rejectionAction.getSequence());
				}
			}
		}
		return rejectionActionMapper.toResource(paymentRejectionActionService.update(toUpdate));
	}

	/**
	 * Delete rejection action
	 *
	 * @param id payment rejection action id
	 */
	public void removeRejectionAction(Long id) {
		PaymentRejectionAction rejectionAction = ofNullable(paymentRejectionActionService.findById(id))
				.orElseThrow(() -> new NotFoundException("Payment rejection action not found"));
		rejectionAction.getRejectionActionReports().forEach(paymentRejectionActionReport
				-> paymentRejectionActionReport.setAction(null));
		paymentRejectionActionService.remove(rejectionAction);
	}

	/**
	 * Delete payment rejection code
	 *
	 * @param filters     PagingAndFiltering
	 * @param forceDelete force delete
	 */
	public int removeRejectionCode(PagingAndFiltering filters, boolean forceDelete) {
		PaginationConfiguration configuration = new PaginationConfiguration(castFilters(filters.getFilters()));
		List<PaymentRejectionCode> rejectionCodesToRemove = rejectionCodeService.list(configuration);
		if (rejectionCodesToRemove == null || rejectionCodesToRemove.isEmpty()) {
			throw new NotFoundException("No payment rejection code found");
		}
		rejectionCodesToRemove.forEach(paymentRejectionCode
				-> removeRejectionCode(paymentRejectionCode.getId(), forceDelete));
		return rejectionCodesToRemove.size();
	}

	private Map<String, Object> castFilters(Map<String, Object> filters) {
		for (Map.Entry<String, Object> entry : filters.entrySet()) {
			if (containsIgnoreCase(entry.getKey(), "id")) {
				List<Long> ids = ((List<Object>) entry.getValue())
						.stream()
						.map(Object::toString)
						.map(Long::valueOf)
						.collect(toList());
				entry.setValue(ids);
			}
		}
		return filters;
	}

	/**
	 * Create payment rejection code group
	 *
	 * @param rejectionGroup Payment rejection group
	 * @return created entity
	 */
	public RejectionGroup createRejectionGroup(RejectionGroup rejectionGroup) {
		checkCodeExistence(rejectionGroup);
		PaymentRejectionCodesGroup entityToSave = groupMapper.toEntity(rejectionGroup);
		if(isBlank(entityToSave.getCode())) {
			entityToSave.setCode(ofNullable(getGenericCode(PaymentRejectionCodesGroup.class.getName()))
					.orElseThrow(() -> new MissingParameterException("Code is missing")));
		}
		if (entityToSave.getPaymentGateway() != null) {
			entityToSave.setPaymentGateway(loadPaymentGateway(entityToSave.getPaymentGateway()));
		}
		if (rejectionGroup.getRejectionCodes() != null && !rejectionGroup.getRejectionCodes().isEmpty()) {
			entityToSave.setPaymentRejectionCodes(loadRejectionCode(rejectionGroup.getRejectionCodes(), entityToSave));
		}
		if (rejectionGroup.getRejectionActions() != null && !rejectionGroup.getRejectionActions().isEmpty()) {
			entityToSave.setPaymentRejectionActions(loadRejectionAction(rejectionGroup.getRejectionActions(), entityToSave));
		}
		paymentRejectionCodesGroupService.create(entityToSave);
		return builder()
				.id(entityToSave.getId())
				.code(entityToSave.getCode())
				.build();
	}

	private void checkCodeExistence(RejectionGroup rejectionGroup) {
		if(!isBlank(rejectionGroup.getCode())
				&& (paymentRejectionCodesGroupService.findByCode(rejectionGroup.getCode()) != null)) {
			throw new EntityAlreadyExistsException("Payment rejection codes group with code "
					+  rejectionGroup.getCode()+ " already exists");
		}
	}

	private PaymentGateway loadPaymentGateway(PaymentGateway paymentGatewayEntity) {
		return ofNullable(paymentGatewayService.findByIdOrCode(paymentGatewayEntity))
				.orElseThrow(() -> new NotFoundException("Payment gateway does not exists"));
	}

	private List<PaymentRejectionCode> loadRejectionCode(List<Resource> rejectionCodes,
														 PaymentRejectionCodesGroup rejectionCodesGroupEntity) {
		List<PaymentRejectionCode> paymentRejectionCodes = new ArrayList<>();
		for (Resource rejectionCodeResource : rejectionCodes) {
			if (rejectionCodeResource != null) {
				PaymentRejectionCode rejectionCodeEntity = new PaymentRejectionCode();
				rejectionCodeEntity.setId(rejectionCodeResource.getId());
				rejectionCodeEntity.setCode(rejectionCodeResource.getCode());
				rejectionCodeEntity = ofNullable(rejectionCodeService.findByIdOrCode(rejectionCodeEntity))
						.orElseThrow(() -> new NotFoundException(format("Payment rejection code %s does not exists",
								rejectionCodeResource.getId() != null
                                ? rejectionCodeResource.getId() : rejectionCodeResource.getCode())));
				if(rejectionCodeEntity.getPaymentRejectionCodesGroup() != null) {
					throw new BusinessApiException(format("Rejection code %s can't be added to several rejection sets",
							rejectionCodeEntity.getCode()));
				}
				rejectionCodeEntity.setPaymentRejectionCodesGroup(rejectionCodesGroupEntity);
				paymentRejectionCodes.add(rejectionCodeEntity);
			}
		}

		return paymentRejectionCodes;
	}

	private List<PaymentRejectionAction> loadRejectionAction(List<Resource> rejectionActions,
															 PaymentRejectionCodesGroup entity) {
		List<PaymentRejectionAction> paymentRejectionActions = new ArrayList<>();
		for (Resource rejectionActionResource : rejectionActions) {
			if (rejectionActionResource != null) {
				PaymentRejectionAction paymentRejectionActionEntity = new PaymentRejectionAction();
				paymentRejectionActionEntity.setId(rejectionActionResource.getId());
				paymentRejectionActionEntity.setCode(rejectionActionResource.getCode());
				paymentRejectionActionEntity = ofNullable(paymentRejectionActionService.findByIdOrCode(paymentRejectionActionEntity))
						.orElseThrow(() -> new NotFoundException("Payment rejection action "
								+ (rejectionActionResource.getId() != null
								? rejectionActionResource.getId() : rejectionActionResource.getCode())
								+ " does not exists"));
				paymentRejectionActionEntity.setPaymentRejectionCodesGroup(entity);
				paymentRejectionActions.add(paymentRejectionActionEntity);
			}
		}
		return paymentRejectionActions;
	}

	/**
	 * Update payment rejection code group
	 *
	 * @param id Payment rejection group id
	 * @param rejectionGroup Payment rejection group
	 * @return updated entity
	 */
	public RejectionGroup updateRejectionGroup(Long id, RejectionGroup rejectionGroup) {
		checkCodeExistence(rejectionGroup);
		PaymentRejectionCodesGroup entityToUpdate = ofNullable(paymentRejectionCodesGroupService.findById(id))
				.orElseThrow(() -> new NotFoundException("Payment rejection code group does not exists"));
		entityToUpdate = groupMapper.toEntity(rejectionGroup, entityToUpdate);
		if (entityToUpdate.getPaymentGateway() != null) {
			entityToUpdate.setPaymentGateway(loadPaymentGateway(entityToUpdate.getPaymentGateway()));
		}
		if (rejectionGroup.getRejectionCodes() != null) {
			if(entityToUpdate.getPaymentRejectionCodes() != null) {
				entityToUpdate.getPaymentRejectionCodes()
						.forEach(paymentRejectionCode -> paymentRejectionCode.setPaymentRejectionCodesGroup(null));
			}
			entityToUpdate.setPaymentRejectionCodes(loadRejectionCode(rejectionGroup.getRejectionCodes(), entityToUpdate));
		}
		if (rejectionGroup.getRejectionActions() != null) {
			if(entityToUpdate.getPaymentRejectionActions() != null) {
				entityToUpdate.getPaymentRejectionActions()
						.forEach(paymentRejectionAction -> paymentRejectionAction.setPaymentRejectionCodesGroup(null));
			}
			entityToUpdate.setPaymentRejectionActions(loadRejectionAction(rejectionGroup.getRejectionActions(), entityToUpdate));
		}
		paymentRejectionCodesGroupService.update(entityToUpdate);
		return groupMapper.toResource(entityToUpdate);
	}

	/**
	 * Delete payment rejection code group
	 *
	 * @param rejectionGroupId payment rejection code group id to remove
	 */
	public void removeRejectionCodeGroup(Long rejectionGroupId) {
		PaymentRejectionCodesGroup toRemove =
				ofNullable(paymentRejectionCodesGroupService.findById(rejectionGroupId))
						.orElseThrow(() -> new NotFoundException("Payment rejection codes group with id "
								+ rejectionGroupId + " does not exists"));
		try {
			removeDependencies(toRemove);
			paymentRejectionCodesGroupService.remove(toRemove);
		} catch (Exception exception) {
			throw new BusinessApiException(exception.getMessage());
		}
	}

	private void removeDependencies(PaymentRejectionCodesGroup toRemove) {
		toRemove.getPaymentRejectionActions()
				.forEach(paymentRejectionAction -> {
					if(paymentRejectionAction.getRejectionActionReports() != null
							&& !paymentRejectionAction.getRejectionActionReports().isEmpty()) {
						paymentRejectionAction.getRejectionActionReports().forEach(report -> report.setAction(null));
					}
					paymentRejectionActionService.remove(paymentRejectionAction);});
		toRemove.getPaymentRejectionCodes()
				.forEach(paymentRejectionCode -> rejectionCodeService.remove(paymentRejectionCode));
	}

	/**
	 * Delete payment rejection code group based on filter
	 *
	 * @param filters PagingAndFiltering
	 */
	public int removeRejectionCodeGroup(PagingAndFiltering filters) {
		PaginationConfiguration configuration = new PaginationConfiguration(castFilters(filters.getFilters()));
		List<PaymentRejectionCodesGroup> groups = paymentRejectionCodesGroupService.list(configuration);
		if (groups == null || groups.isEmpty()) {
			throw new NotFoundException("No payment rejection code found");
		}
		try {
			groups.forEach(this::removeDependencies);
			paymentRejectionCodesGroupService.remove(groups);
			return groups.size();
		} catch (Exception exception) {
			throw new BusinessApiException(exception.getMessage());
		}
    }

	/**
	 * Update payment action sequence
	 *
	 * @param actionId action id
	 * @param sequenceAction sequence action
	 */
	public RejectionAction updateActionSequence(Long actionId, SequenceAction sequenceAction) {
		PaymentRejectionAction actionToUpdate = ofNullable(paymentRejectionActionService.findById(actionId))
				.orElseThrow(() -> new NotFoundException("Payment rejection action does not exists"));
		if(actionToUpdate.getPaymentRejectionCodesGroup() == null) {
			throw new BusinessApiException("No payment rejection group associated to the provided action");
		}
		List<PaymentRejectionAction> actions = actionToUpdate.getPaymentRejectionCodesGroup().getPaymentRejectionActions();
		if(actions == null || actions.isEmpty()) {
			throw new BadRequestException("Payment rejection group has no action");
		}
		if(actions.size() == 1) {
			throw new BadRequestException("Payment rejection group has only one action");
		}
		actions.sort(comparing(PaymentRejectionAction::getSequence));
		int currentSequence = actionToUpdate.getSequence();
		if(currentSequence == 0
				&& UP == sequenceAction.getSequenceAction()) {
			throw new BadRequestException("Update sequence can not be performed : no previous action found");
		}
		if(currentSequence == (actions.size() - 1)
				&& DOWN == sequenceAction.getSequenceAction()) {
			throw new BadRequestException("Update sequence can not be performed : no next action found");
		}
		PaymentRejectionAction actionToSwitch;
		if(UP == sequenceAction.getSequenceAction()) {
			actionToSwitch = actions.get(currentSequence - 1);
			actionToSwitch.setSequence(currentSequence);
			actionToUpdate.setSequence(currentSequence - 1);
		} else {
			actionToSwitch = actions.get(currentSequence + 1);
			actionToSwitch.setSequence(currentSequence);
			actionToUpdate.setSequence(currentSequence + 1);
		}
		paymentRejectionActionService.update(actionToUpdate);
		paymentRejectionActionService.update(actionToSwitch);
		return rejectionActionMapper.toResource(actionToUpdate);
	}

	/**
	 * Create rejection payment
	 *
	 * @param rejectionPayment rejection payment input
	 * @return RejectionPayment
	 */
	public RejectionPayment createRejectionPayment(RejectionPayment rejectionPayment) {
		if (rejectionPayment.getId() == null
				&& StringUtils.isBlank(rejectionPayment.getExternalPaymentId())) {
			throw new MissingParameterException("Id or externalId are required");
		}
		Payment payment = rejectionPayment.getId() != null
				? loadRejectionPaymentById(rejectionPayment) : loadRejectionPaymentByExternalId(rejectionPayment);
		if(payment.getRejectedPayment() != null) {
			throw new ForbiddenException(format("Payment[id=%d, reference=%s] has already been rejected by RejectedPayment[id=%d]",
					payment.getId(), payment.getReference(), payment.getRejectedPayment().getId()));
		}
		if (payment.getPaymentGateway() == null && isBlank(rejectionPayment.getPaymentGatewayCode())) {
			throw new BadRequestException("Payment has no gateway. Please provide a paymentGatewayCode");
		}
		PaymentRejectionCode paymentRejectionCode = rejectionCodeService.findByCode(rejectionPayment.getRejectionCode());
		ofNullable(paymentRejectionCode)
				.orElseThrow(() -> new NotFoundException("Provided rejection code not found"));
		PaymentGateway paymentGateway = null;
		if(rejectionPayment.getPaymentGatewayCode() != null) {
			paymentGateway = paymentGatewayService.findByCode(rejectionPayment.getPaymentGatewayCode());
		}
		if(paymentGateway == null && payment.getPaymentGateway() != null) {
			paymentGateway = payment.getPaymentGateway();
		}
		if(paymentGateway == null && rejectionPayment.getPaymentGatewayCode() != null) {
			throw new BadRequestException("Payment has no gateway. Please provide a valid paymentGateway");
		}
		if(paymentRejectionCode.getPaymentGateway() != null
				&& !paymentGateway.getId().equals(paymentRejectionCode.getPaymentGateway().getId())) {
			throw new BadRequestException("Rejection code " + rejectionPayment.getRejectionCode()
					+ " not found for gateway[code=" + rejectionPayment.getPaymentGatewayCode() + "]");
		}
		try {

			OCCTemplate occTemplate = paymentService.getOCCTemplateRejectPayment(payment);
			matchingCodeService.unmatchingByAOid(payment.getId());

			RejectedPayment rejectedPayment = from(rejectionPayment, payment, occTemplate);
			rejectedPayment.setPaymentGateway(paymentGateway);
			accountOperationService.handleAccountingPeriods(rejectedPayment);
			accountOperationService.create(rejectedPayment);
			payment.setRejectedPayment(rejectedPayment);
			if (!rejectionPayment.getSkipRejectionActions()) {
				paymentService.createRejectionActions(rejectedPayment);
			}
			if (rejectedPayment.getListAaccountOperationSupposedPaid() != null) {
				for (AccountOperation ao : rejectedPayment.getListAaccountOperationSupposedPaid()) {
					ao.setRejectedPayment(rejectedPayment);
				}
			}
			List<Long> aos = new ArrayList<>();
			aos.add(payment.getId());
			aos.add(rejectedPayment.getId());
			matchingCodeService.matchOperations(payment.getCustomerAccount().getId(),
					null, aos, null);
			paymentHistoryService.rejectPaymentHistory(payment.getReference(),
					rejectionPayment.getRejectionCode(), rejectionPayment.getComment());
			accountOperationService.update(payment);
			return RejectionPayment.from(rejectedPayment);
		} catch (Exception exception) {
			throw new BusinessApiException(exception);
		}
	}

	private RejectedPayment from(RejectionPayment rejectionPayment, Payment payment, OCCTemplate occTemplate) {
		RejectedPayment rejectedPayment = new RejectedPayment();
		CustomerAccount customerAccount = payment.getCustomerAccount();
		paymentService.calculateAmountsByTransactionCurrency(rejectedPayment,
				customerAccount, payment.getUnMatchingAmount(), null, new Date());
		rejectedPayment.setRejectedType(MANUAL);
		rejectedPayment.setBankReference(payment.getBankReference());
		rejectedPayment.setReference(payment.getReference());
		rejectedPayment.setRejectedDate(new Date());
		rejectedPayment.setTransactionDate(new Date());
		rejectedPayment.setDueDate(payment.getDueDate());
		rejectedPayment.setAccountingCode(occTemplate.getAccountingCode());
		rejectedPayment.setCode(occTemplate.getCode());
		rejectedPayment.setDescription(occTemplate.getDescription());
		rejectedPayment.setTransactionCategory(occTemplate.getOccCategory());
		rejectedPayment.setPaymentMethod(payment.getPaymentMethod());
		rejectedPayment.setMatchingAmount(ZERO);
		rejectedPayment.setUnMatchingAmount(payment.getUnMatchingAmount());
		rejectedPayment.setCustomerAccount(payment.getCustomerAccount());
		rejectedPayment.setAccountCodeClientSide(payment.getAccountCodeClientSide());
		rejectedPayment.setTaxAmount(payment.getTaxAmount());
		rejectedPayment.setAmountWithoutTax(payment.getAmountWithoutTax());
		rejectedPayment.setOrderNumber(payment.getOrderNumber());
		rejectedPayment.setMatchingStatus(O);
		rejectedPayment.setRejectedDescription(rejectionPayment.getComment());
		rejectedPayment.setRejectedCode(rejectionPayment.getRejectionCode());
		rejectedPayment.setListAaccountOperationSupposedPaid(paymentService.getAccountOperationThatWasPaid(payment));
		return rejectedPayment;
	}

	private Payment loadRejectionPaymentById(RejectionPayment rejectionPayment) {
		AccountOperation accountOperation
				= accountOperationService.findById(rejectionPayment.getId());
		if (accountOperation == null) {
			throw new NotFoundException("Payment[id=" + rejectionPayment.getId() + "] does not exist");
		}
		if (!(accountOperation instanceof Payment)) {
			throw new BadRequestException("AccountOperation[id=" + rejectionPayment.getId() + "] is not a payment");
		} else {
			return (Payment) accountOperation;
		}
	}

	private Payment loadRejectionPaymentByExternalId(RejectionPayment rejectionPayment) {
		if (!isBlank(rejectionPayment.getPaymentGatewayCode())) {
			List<Payment> payments = paymentService.
					findByExternalIdAndPaymentGateWay(rejectionPayment.getExternalPaymentId(), rejectionPayment.getPaymentGatewayCode());
			if (payments == null || payments.isEmpty()) {
				throw new NotFoundException("Payment[externalPaymentId=" + rejectionPayment.getExternalPaymentId()
						+ "}, paymentGateway=" + rejectionPayment.getPaymentGatewayCode() + "] does not exists");
			}
			return payments.get(0);
		} else {
			List<AccountOperation> accountOperations =
					accountOperationService.findByExternalId(rejectionPayment.getExternalPaymentId());
			if (accountOperations == null || accountOperations.isEmpty()) {
				throw new NotFoundException("Payment[externalPaymentId=" + rejectionPayment.getExternalPaymentId() + "] does not exist");
			}
			if (accountOperations.size() > 1) {
				throw new BadRequestException("Several payments found with externalPaymentId="
						+ rejectionPayment.getExternalPaymentId() + ". Please provide either internal id or paymentGatewayCode");
			}
			if(!(accountOperations.get(0) instanceof Payment)) {
				throw new BadRequestException("AccountOperation[externalPaymentId="
						+ rejectionPayment.getExternalPaymentId() + "] is not a payment");
			}
			return (Payment) accountOperations.get(0);
		}
	}
}
