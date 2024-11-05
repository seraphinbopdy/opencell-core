package org.meveo.apiv2.accountreceivable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import org.junit.runner.RunWith;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.NoAllOperationUnmatchedException;
import org.meveo.admin.exception.UnbalanceAmountException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.api.dto.account.CustomerAccountDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.apiv2.AcountReceivable.*;
import org.meveo.apiv2.AcountReceivable.CustomerAccount;
import org.meveo.model.MatchingReturnObject;
import org.meveo.model.PartialMatchingOccToSelect;
import org.meveo.model.admin.Currency;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.payments.*;
import org.meveo.service.payments.impl.*;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class AccountOperationApiServiceTest {

    @Mock
    private ResourceBundle resourceMessages;
    
    @InjectMocks
    private AccountOperationApiService accountOperationApiService;

    @Mock
    private AccountOperationService accountOperationService;
	
	@Mock
	private OCCTemplateService occTemplateService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CustomerAccountService customerAccountService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MatchingCodeService matchingCodeService;
    
    @Mock
    private PaymentPlanService paymentPlanService;
    
    @Mock
    List<PartialMatchingOccToSelect> partialMatchingOcc;

    @Before
    public void setUp() {
        org.meveo.model.payments.CustomerAccount customerAccount = new org.meveo.model.payments.CustomerAccount();
        customerAccount.setId(1L);
        customerAccount.setCode("1L");
        AccountOperation accountOperation = new AccountOperation();
        AccountOperation updatedAO = new AccountOperation();
        accountOperation.setId(1L);
        updatedAO.setId(1L);
        updatedAO.setCustomerAccount(customerAccount);
        Mockito.when(customerAccountService.findById(1L)).thenReturn(customerAccount);
        Mockito.when(accountOperationService.findById(1L)).thenReturn(accountOperation);
        Mockito.when(accountOperationService.update(accountOperation)).thenReturn(updatedAO);
    }

    @Test
    public void shouldAssignAccountOperationToCustomerAccount() {
        CustomerAccount customerAccount = ImmutableCustomerAccount.builder().id(1L).code("CODE").build();
        AccountOperation updatedAO =
                accountOperationApiService.assignAccountOperation(1L, customerAccount).get();
        assertEquals(1L, updatedAO.getCustomerAccount().getId().longValue());
    }

    @Test(expected = NotFoundException.class)
    public void shouldFailToAssignAccountOperationToCustomerAccountAccountOperationNotFound() {
        CustomerAccount customerAccount = ImmutableCustomerAccount.builder().id(1L).code("CODE").build();
        Mockito.when(accountOperationService.findById(1L)).thenReturn(null);
        accountOperationApiService.assignAccountOperation(1L, customerAccount).get();
    }

    @Test(expected = NotFoundException.class)
    public void shouldFailToAssignAccountOperationCustomerAccountNotFound() {
        CustomerAccount customerAccount = ImmutableCustomerAccount.builder().id(1L).code("CODE").build();
        Mockito.when(accountOperationService.findById(1L)).thenReturn(null);
        accountOperationApiService.assignAccountOperation(1L, customerAccount).get();
    }
    
    @Test
    public void shouldFailToDifferentCurrency() throws BusinessException, NoAllOperationUnmatchedException, UnbalanceAmountException, Exception {
        List<AccountOperationAndSequence> operationAndSequence = initOperationSequence();

        AccountOperation aoInvoice = init("I", 2L, new BigDecimal(9000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(9000), AccountOperationStatus.POSTED);
        AccountOperation aoP1 = init("P", 3L, new BigDecimal(2000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(2000), AccountOperationStatus.POSTED);
        AccountOperation aoP2 = init("P", 4L, new BigDecimal(3000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(3000), AccountOperationStatus.POSTED);
        TradingCurrency eTradingCurrency1 = new TradingCurrency();
        Currency eCurrency1 = new Currency();
        eCurrency1.setCurrencyCode("USD");//EUR
        eTradingCurrency1.setCurrency(eCurrency1);
        eTradingCurrency1.setId(1L);
        TradingCurrency eTradingCurrency2 = new TradingCurrency();
        eTradingCurrency2.setId(2L);
        Currency eCurrency2 = new Currency();
        eCurrency2.setCurrencyCode("EUR");

        aoP1.setTransactionalCurrency(eTradingCurrency1);
        aoP2.setTransactionalCurrency(eTradingCurrency2);
        aoInvoice.setTransactionalCurrency(eTradingCurrency1);
        aoInvoice.setTransactionCategory(OperationCategoryEnum.CREDIT);
        List<Long> aoIds = List.of(2L, 3L, 4L);
        List<AccountOperation> accountOperations = List.of(aoInvoice, aoP1, aoP2);

        Mockito.when(accountOperationService.findById(2L)).thenReturn(aoInvoice);
        Mockito.when(accountOperationService.findById(3L)).thenReturn(aoP1);
        Mockito.when(accountOperationService.findById(4L)).thenReturn(aoP2);

        Exception exception = assertThrows(BusinessApiException.class, () -> {
            accountOperationApiService.matchOperations(operationAndSequence);
        });
    }
        
    @Test(expected = BusinessApiException.class)
    public void shouldMatchOperationAndUseAmountFromDto() throws BusinessException, NoAllOperationUnmatchedException, UnbalanceAmountException, Exception {
        List<AccountOperationAndSequence> operationAndSequence = initOperationSequence();

        AccountOperation aoInvoice = init("I", 2L, new BigDecimal(9000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(9000), AccountOperationStatus.POSTED);
        AccountOperation aoP1 = init("P", 3L, new BigDecimal(2000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(2000), AccountOperationStatus.POSTED);
        AccountOperation aoP2 = init("P", 4L, new BigDecimal(3000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(3000), AccountOperationStatus.POSTED);
        

        Mockito.when(accountOperationService.findById(2L)).thenReturn(aoInvoice);
        Mockito.when(accountOperationService.findById(3L)).thenReturn(aoP1);
        Mockito.when(accountOperationService.findById(4L)).thenReturn(aoP2);
        Mockito.when(customerAccountService.findCustomerAccount(anyLong(), anyString())).thenReturn(aoInvoice.getCustomerAccount());

        MatchingReturnObject matchingReturnObject1 = new MatchingReturnObject();
        matchingReturnObject1.setOk(true);
        
        PartialMatchingOccToSelect PartialMatchingOccToSelect = new PartialMatchingOccToSelect();
        PartialMatchingOccToSelect.setAccountOperation(aoP1);
        PartialMatchingOccToSelect.setPartialMatchingAllowed(true);
        
        matchingReturnObject1.getPartialMatchingOcc().add(PartialMatchingOccToSelect);
        

        MatchingReturnObject matchingReturnObject2 = new MatchingReturnObject();
        matchingReturnObject2.setOk(true);
        
        PartialMatchingOccToSelect PartialMatchingOccToSelect2 = new PartialMatchingOccToSelect();
        PartialMatchingOccToSelect2.setAccountOperation(aoP2);
        PartialMatchingOccToSelect2.setPartialMatchingAllowed(true);
        
        matchingReturnObject2.getPartialMatchingOcc().add(PartialMatchingOccToSelect);
       
        Mockito.when(matchingCodeService.matchOperations(anyLong(), anyString(), anyList(), anyLong(), any(BigDecimal.class))).thenReturn(matchingReturnObject1).thenReturn(matchingReturnObject2);

        accountOperationApiService.matchOperations(operationAndSequence);
    }
    

    @Test
    public void shouldReturnBusinessExceptionForZeroAmountMatchOperationAndUseAmountFromDto() throws BusinessException, NoAllOperationUnmatchedException, UnbalanceAmountException, Exception {
        List<AccountOperationAndSequence> operationAndSequence = List.of(ImmutableAccountOperationAndSequence.builder().id(1L).sequence(0).build(),
            ImmutableAccountOperationAndSequence.builder().id(2L).sequence(1).amountToMatch(BigDecimal.ZERO).build());

        AccountOperation aoInvoice = init("I", 2L, new BigDecimal(9000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(9000), AccountOperationStatus.POSTED);
        AccountOperation aoP1 = init("P", 3L, new BigDecimal(2000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(2000), AccountOperationStatus.POSTED);

        Mockito.when(accountOperationService.findById(2L)).thenReturn(aoInvoice);
        Mockito.when(accountOperationService.findById(1L)).thenReturn(aoP1);

        Exception exception = assertThrows(BusinessApiException.class, () -> {
            accountOperationApiService.matchOperations(operationAndSequence);
        });
        
        assertTrue(exception.getMessage().contains("The amount to match must be greater than 0"));
    }
    

    @Test
    public void shouldReturnBusinessExceptionForAmountEqualToAccountOprtation() throws BusinessException, NoAllOperationUnmatchedException, UnbalanceAmountException, Exception {
        List<AccountOperationAndSequence> operationAndSequence = List.of(ImmutableAccountOperationAndSequence.builder().id(1L).sequence(0).build(),
            ImmutableAccountOperationAndSequence.builder().id(2L).sequence(1).amountToMatch(new BigDecimal(9001)).build());

        AccountOperation aoInvoice = init("I", 2L, new BigDecimal(9000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(9000), AccountOperationStatus.POSTED);
        AccountOperation aoP1 = init("P", 3L, new BigDecimal(2000), BigDecimal.ZERO, MatchingStatusEnum.O, new BigDecimal(2000), AccountOperationStatus.POSTED);

        Mockito.when(accountOperationService.findById(2L)).thenReturn(aoInvoice);
        Mockito.when(accountOperationService.findById(1L)).thenReturn(aoP1);
        Mockito.when(customerAccountService.findCustomerAccount(anyLong(), anyString())).thenReturn(aoInvoice.getCustomerAccount());
        
        Exception exception = assertThrows(BusinessApiException.class, () -> {
            accountOperationApiService.matchOperations(operationAndSequence);
        });
        
        assertTrue(exception.getMessage().contains("The amount to match must be less than : "));
    }
	
	@Test
	public void shouldReturnEntityDoesNotExistsExceptionForTransferAmountsCoustmerAccountNotFoundById() {
		//given
		CustomerAccount customerAccountDto = ImmutableCustomerAccount.builder().id(100L).build();
		AmountToTransferDto amountToTransferDto = ImmutableAmountToTransferDto.builder().amount(new BigDecimal(100.0d)).customerAccount(customerAccountDto).build();
		AmountsTransferDto amountsTransferDto = ImmutableAmountsTransferDto.builder().amountsToTransfer(List.of(amountToTransferDto)).build();
		//when
		Mockito.when(customerAccountService.findById(100L)).thenReturn(null);
		assertThrows(EntityDoesNotExistsException.class, () -> {
			accountOperationApiService.transferAmounts(1L, amountsTransferDto);
		});
	}
	
	@Test
	public void shouldReturnEntityDoesNotExistsExceptionForTransferAmountsCoustmerAccountNotFoundByCode() {
		//given
		CustomerAccount customerAccountDto = ImmutableCustomerAccount.builder().code("CODE").build();
		AmountToTransferDto amountToTransferDto = ImmutableAmountToTransferDto.builder().amount(new BigDecimal(100.0d)).customerAccount(customerAccountDto).build();
		AmountsTransferDto amountsTransferDto = ImmutableAmountsTransferDto.builder().amountsToTransfer(List.of(amountToTransferDto)).build();
		//when
		Mockito.when(customerAccountService.findByCode("CODE")).thenReturn(null);
		assertThrows(EntityDoesNotExistsException.class, () -> {
			accountOperationApiService.transferAmounts(1L, amountsTransferDto);
		});
	}
	
	@Test
	public void shouldReturnBadRequestExceptionAmountsToTransferGreaterThanUnmatchedAmount() {
		//given
		CustomerAccount customerAccountDto = ImmutableCustomerAccount.builder().code("CODE").build();
		AmountToTransferDto amountToTransferDto = ImmutableAmountToTransferDto.builder().amount(new BigDecimal(100.0d)).customerAccount(customerAccountDto).build();
		AmountsTransferDto amountsTransferDto = ImmutableAmountsTransferDto.builder().amountsToTransfer(List.of(amountToTransferDto)).build();
		AccountOperation accountOperation = new AccountOperation();
		accountOperation.setUnMatchingAmount(new BigDecimal(50));
		
		TradingCurrency tradingCurrency = new TradingCurrency();
		tradingCurrency.setId(1L);
		Currency currency = new Currency();
		currency.setCurrencyCode("USD");
		tradingCurrency.setCurrency(currency);
		
		var customerAccountForAccount = new org.meveo.model.payments.CustomerAccount();
		customerAccountForAccount.setTradingCurrency(tradingCurrency);
		accountOperation.setCustomerAccount(customerAccountForAccount);
		var customerAccount = new org.meveo.model.payments.CustomerAccount();
		customerAccount.setTradingCurrency(tradingCurrency);
		//when
		Mockito.when(customerAccountService.findByCode("CODE")).thenReturn(customerAccount);
		Mockito.when(accountOperationService.findById(2L, List.of("customerAccount"))).thenReturn(accountOperation);
		assertThrows(BadRequestException.class, () -> {
			accountOperationApiService.transferAmounts(2L, amountsTransferDto);
		});
	}
	
	@Test
	public void shouldPassOnAmountsToTransferGreaterThanUnmatchedAmount() {
		//given
		CustomerAccount customerAccountDto = ImmutableCustomerAccount.builder().code("CODE").build();
		AmountToTransferDto amountToTransferDto = ImmutableAmountToTransferDto.builder().amount(new BigDecimal(100.0d)).customerAccount(customerAccountDto).build();
		AmountsTransferDto amountsTransferDto = ImmutableAmountsTransferDto.builder().amountsToTransfer(List.of(amountToTransferDto)).build();
		AccountOperation accountOperation = new AccountOperation();
		accountOperation.setUnMatchingAmount(new BigDecimal(50));
		
		TradingCurrency tradingCurrency = new TradingCurrency();
		tradingCurrency.setId(1L);
		Currency currency = new Currency();
		currency.setCurrencyCode("USD");
		tradingCurrency.setCurrency(currency);
		
		var customerAccountForAccount = new org.meveo.model.payments.CustomerAccount();
		customerAccountForAccount.setTradingCurrency(tradingCurrency);
		accountOperation.setCustomerAccount(customerAccountForAccount);
		accountOperation.setUnMatchingAmount(new BigDecimal(250));
		var customerAccount = new org.meveo.model.payments.CustomerAccount();
		customerAccount.setTradingCurrency(tradingCurrency);
		//when
		Mockito.when(customerAccountService.findByCode("CODE")).thenReturn(customerAccount);
		
		assertThrows(EntityDoesNotExistsException.class, () -> {
			accountOperationApiService.transferAmounts(2L, amountsTransferDto);
		});
	}
	
	@Test
	public void shouldReturnEntityDoesNotExistsExceptionTransferAmountsForCustomerAccounts(){
		//given
		CustomerAccount customerAccountDto = ImmutableCustomerAccount.builder().code("CODE").build();
		AmountToTransferDto amountToTransferDto = ImmutableAmountToTransferDto.builder().amount(new BigDecimal(20.0d)).customerAccount(customerAccountDto).build();
		AmountsTransferDto amountsTransferDto = ImmutableAmountsTransferDto.builder().amountsToTransfer(List.of(amountToTransferDto)).build();
		AccountOperation accountOperation = new AccountOperation();
		accountOperation.setUnMatchingAmount(new BigDecimal(50));
		
		TradingCurrency tradingCurrency = new TradingCurrency();
		tradingCurrency.setId(1L);
		Currency currency = new Currency();
		currency.setCurrencyCode("USD");
		tradingCurrency.setCurrency(currency);
		
		var customerAccountForAccount = new org.meveo.model.payments.CustomerAccount();
		customerAccountForAccount.setTradingCurrency(tradingCurrency);
		accountOperation.setCustomerAccount(customerAccountForAccount);
		var customerAccount = new org.meveo.model.payments.CustomerAccount();
		customerAccount.setTradingCurrency(tradingCurrency);
		//when
		Mockito.when(customerAccountService.findByCode("CODE")).thenReturn(customerAccount);
		assertThrows(EntityDoesNotExistsException.class, () -> {
			accountOperationApiService.transferAmounts(2L, amountsTransferDto);
		});
	}
	@Test
	public void shouldTransferAmountsForCustomerAccountsForCreditAO() throws Exception {
		//given
		CustomerAccount customerAccountDto = ImmutableCustomerAccount.builder().code("CODE").build();
		AmountToTransferDto amountToTransferDto = ImmutableAmountToTransferDto.builder().amount(new BigDecimal(20.0d)).customerAccount(customerAccountDto).build();
		AmountsTransferDto amountsTransferDto = ImmutableAmountsTransferDto.builder().amountsToTransfer(List.of(amountToTransferDto)).build();
		AccountOperation accountOperation = new AccountOperation();
		accountOperation.setId(1L);
		accountOperation.setUnMatchingAmount(new BigDecimal(50));
		
		TradingCurrency tradingCurrency = new TradingCurrency();
		tradingCurrency.setId(1L);
		Currency currency = new Currency();
		currency.setCurrencyCode("USD");
		tradingCurrency.setCurrency(currency);
		
		var customerAccountForAccount = new org.meveo.model.payments.CustomerAccount();
		customerAccountForAccount.setId(35L);
		customerAccountForAccount.setTradingCurrency(tradingCurrency);
		accountOperation.setCustomerAccount(customerAccountForAccount);
		accountOperation.setTransactionCategory(OperationCategoryEnum.CREDIT);
		var customerAccount = new org.meveo.model.payments.CustomerAccount();
		customerAccount.setTradingCurrency(tradingCurrency);
		
		OCCTemplate occTemplateCredit = new OCCTemplate();
		occTemplateCredit.setCode("CRD_TRS");
		occTemplateCredit.setOccCategory(OperationCategoryEnum.CREDIT);
		occTemplateCredit.setDescription("Credit transfer");
		
		
		OCCTemplate occTemplateDebit = new OCCTemplate();
		occTemplateDebit.setCode("DBT_TRS");
		occTemplateDebit.setOccCategory(OperationCategoryEnum.DEBIT);
		occTemplateDebit.setDescription("Debit transfer");
		// after creating a new account operation, set a random id to it
		AccountOperation newAccountOperation = new AccountOperation();
		newAccountOperation.setId(Math.round(Math.random() * 1000));
		//when
		Mockito.when(customerAccountService.findByCode("CODE")).thenReturn(customerAccount);
		Mockito.when(accountOperationService.findById(2L,List.of("customerAccount"))).thenReturn(accountOperation);
		Mockito.when(occTemplateService.findByCode("CRD_TRS", List.of("accountingCode"))).thenReturn(occTemplateCredit);
		Mockito.when(occTemplateService.findByCode("DBT_TRS", List.of("accountingCode"))).thenReturn(occTemplateDebit);
		Mockito.when(matchingCodeService.matchOperations(customerAccount.getId(), null, null, null)).thenReturn(null);
		
		accountOperationApiService.transferAmounts(2L, amountsTransferDto);
	}
	@Test
	public void shouldReturnBadRequestExceptionForTransferAmountsCoustmerAccountAccountRequired() {
		//given
		AmountsTransferDto amountsTransferDto = ImmutableAmountsTransferDto.builder().amountsToTransfer(null).build();
		//when
		assertThrows(BadRequestException.class, () -> {
			accountOperationApiService.transferAmounts(1L, amountsTransferDto);
		});
	}

    private List<AccountOperationAndSequence> initOperationSequence() {
        List<AccountOperationAndSequence> sequence = new ArrayList<>();
        AccountOperationAndSequence p1 = ImmutableAccountOperationAndSequence.builder().id(2L).sequence(0).build();
        AccountOperationAndSequence p2 = ImmutableAccountOperationAndSequence.builder().id(3L).sequence(1).amountToMatch(new BigDecimal(1000)).build();
        AccountOperationAndSequence p3 = ImmutableAccountOperationAndSequence.builder().id(4L).sequence(2).amountToMatch(new BigDecimal(2000)).build();
        sequence.add(p1);
        sequence.add(p2);
        sequence.add(p3);
        return sequence;
    }
    
    private AccountOperation init(String typeOperation, Long idAp, BigDecimal amount, BigDecimal matchingAmount, MatchingStatusEnum matchingStatus, BigDecimal unMatchingAmount, AccountOperationStatus statusAop) {
        AccountOperation ao = new AccountOperation();
        ao.setType(typeOperation);
        ao.setId(idAp);
        ao.setCode("AO_CODE");
        ao.setAmount(amount);
        ao.setMatchingAmount(matchingAmount);
        ao.setMatchingStatus(matchingStatus);
        ao.setUnMatchingAmount(unMatchingAmount);
        ao.setStatus(statusAop);
        ao.setCustomerAccount(new org.meveo.model.payments.CustomerAccount());
        ao.getCustomerAccount().setId(1L);
        ao.getCustomerAccount().setCode("CODE");
        return ao;
    }
	
	

}