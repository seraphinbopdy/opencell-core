package org.meveo.service.payment;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.meveo.model.payments.OperationCategoryEnum.CREDIT;
import static org.meveo.model.payments.OperationCategoryEnum.DEBIT;
import static org.mockito.Mockito.*;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ValidationException;
import org.meveo.api.dto.CurrencyDto;
import org.meveo.api.dto.account.CustomerAccountDto;
import org.meveo.apiv2.payments.AccountOperationsDetails;
import org.meveo.apiv2.payments.ImmutableAccountOperationsDetails;
import org.meveo.apiv2.payments.ImmutableCustomerBalance;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.CustomerBalance;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.security.MeveoUser;
import org.meveo.service.admin.impl.TradingCurrencyService;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.CustomerBalanceService;
import org.meveo.service.payments.impl.OCCTemplateService;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CustomerBalanceServiceTest {

    @Spy
    @InjectMocks
    private CustomerBalanceService customerBalanceService;

    @Mock
    private OCCTemplateService occTemplateService;

    @Mock
    private ParamBeanFactory paramBeanFactory;

    @Mock
    private ParamBean paramBean;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerWrapper emWrapper;

    @Mock
    private MeveoUser meveoUser;

    @Mock
    private CustomerAccountService customerAccountService;

    @Mock
    private AccountOperationService accountOperationService;

    @Mock
    private TradingCurrencyService tradingCurrencyService;

    @Test
    public void shouldCreateCustomerBalance() {
        OCCTemplate template = new OCCTemplate();
        template.setId(1L);
        template.setOccCategory(CREDIT);
        template.setCode("OCC_CODE");
        OCCTemplate template2 = new OCCTemplate();
        template2.setId(2L);
        template2.setOccCategory(DEBIT);
        template2.setCode("OCC_CODE");
        CustomerBalance customerBalance = createCustomerBalance(true, asList(template, template2));

        when(emWrapper.getEntityManager()).thenReturn(entityManager);
        when(paramBeanFactory.getInstance()).thenReturn(paramBean);
        when(paramBean.getPropertyAsInteger("max.customer.balance", 6)).thenReturn(6);
        doReturn(5L).when(customerBalanceService).count();
        when(occTemplateService.findById(1L)).thenReturn(template);
        when(occTemplateService.findById(2L)).thenReturn(template2);
        doReturn(empty()).when(customerBalanceService).findDefaultCustomerBalance();

        customerBalanceService.create(customerBalance);
        verify(customerBalanceService, times(1)).create(customerBalance);
    }

    @Test
    public void shouldNotCreateCustomerBalanceIfLimitExceeded() {
        CustomerBalance customerBalance = createCustomerBalance(true, emptyList());

        when(paramBeanFactory.getInstance()).thenReturn(paramBean);
        when(paramBean.getPropertyAsInteger("max.customer.balance", 6)).thenReturn(6);
        doReturn(7L).when(customerBalanceService).count();
        doReturn(empty()).when(customerBalanceService).findDefaultCustomerBalance();

        BusinessException limitException = assertThrows(
                BusinessException.class, () -> customerBalanceService.create(customerBalance));

        assertThat(limitException.getMessage(), is(ErrorMessage.MAX_LIMIT_REACHED.errorMessage()));
    }

    @Test
    public void shouldNotCreateMoreThanOneCustomerBalance() {
        CustomerBalance customerBalance = createCustomerBalance(true, emptyList());

        when(paramBeanFactory.getInstance()).thenReturn(paramBean);
        when(paramBean.getPropertyAsInteger("max.customer.balance", 6)).thenReturn(6);
        doReturn(of(new CustomerBalance())).when(customerBalanceService).findDefaultCustomerBalance();

        BusinessException moreThanOneDefaultBalance = assertThrows(
                BusinessException.class, () -> customerBalanceService.create(customerBalance));

        assertThat(moreThanOneDefaultBalance.getMessage(), is(ErrorMessage.MORE_THAN_ONE_CB_FOUND.errorMessage()));
    }

    @Test
    public void shouldFailsIfOccTemplateNotFound() {
        CustomerBalance customerBalance = createCustomerBalance(true, emptyList());

        when(paramBeanFactory.getInstance()).thenReturn(paramBean);
        when(paramBean.getPropertyAsInteger("max.customer.balance", 6)).thenReturn(6);
        doReturn(5L).when(customerBalanceService).count();
        doReturn(empty()).when(customerBalanceService).findDefaultCustomerBalance();

        NotFoundException templateNotFound = assertThrows(
                NotFoundException.class, () -> customerBalanceService.create(customerBalance));

        assertThat(templateNotFound.getMessage(), is(ErrorMessage.OCC_TEMPLATE_NOT_FOUND.errorMessage()));
    }

    @Test
    public void shouldNotCreateCustomerBalanceWithEmptyCreditLine() {
        OCCTemplate template2 = new OCCTemplate();
        template2.setId(2L);
        template2.setCode("2L");
        template2.setOccCategory(DEBIT);
        CustomerBalance customerBalance = createCustomerBalance(true, asList(template2));

        when(paramBeanFactory.getInstance()).thenReturn(paramBean);
        when(paramBean.getPropertyAsInteger("max.customer.balance", 6)).thenReturn(6);
        doReturn(5L).when(customerBalanceService).count();
        doReturn(empty()).when(customerBalanceService).findDefaultCustomerBalance();
        when(occTemplateService.findById(2L)).thenReturn(template2);

        BusinessException creditLineEmpty = assertThrows(
                BusinessException.class, () -> customerBalanceService.create(customerBalance));

        assertThat(creditLineEmpty.getMessage(), is(ErrorMessage.EMPTY_CREDIT_LINE.errorMessage()));
    }

    @Test
    public void shouldNotCreateCustomerBalanceWithEmptyDebitLine() {
        OCCTemplate template2 = new OCCTemplate();
        template2.setId(2L);
        template2.setCode("2L");
        template2.setOccCategory(CREDIT);
        CustomerBalance customerBalance = createCustomerBalance(true, asList(template2));

        when(paramBeanFactory.getInstance()).thenReturn(paramBean);
        when(paramBean.getPropertyAsInteger("max.customer.balance", 6)).thenReturn(6);
        doReturn(5L).when(customerBalanceService).count();
        doReturn(empty()).when(customerBalanceService).findDefaultCustomerBalance();
        when(occTemplateService.findById(2L)).thenReturn(template2);

        BusinessException debitLineEmpty = assertThrows(
                BusinessException.class, () -> customerBalanceService.create(customerBalance));

        assertThat(debitLineEmpty.getMessage(), is(ErrorMessage.EMPTY_DEBIT_LINE.errorMessage()));
    }

    private CustomerBalance createCustomerBalance(boolean defaultBalance, List<OCCTemplate> templates) {
        CustomerBalance customerBalance = new CustomerBalance();
        customerBalance.setCode("CB_001");
        customerBalance.setDescription("CB_001");
        customerBalance.setDefaultBalance(defaultBalance);
        if(templates.isEmpty()) {
            OCCTemplate template = new OCCTemplate();
            template.setId(1L);
            template.setOccCategory(CREDIT);
            OCCTemplate template2 = new OCCTemplate();
            template2.setId(2L);
            template2.setOccCategory(DEBIT);
            customerBalance.setOccTemplates(asList(template, template2));
        } else {
            customerBalance.setOccTemplates(templates);
        }
        return customerBalance;
    }

    @Test
    public void shouldGetAccountOperations() {
        CustomerAccountDto customerAccountDto = new CustomerAccountDto();
        customerAccountDto.setId(1L);
        customerAccountDto.setCode("CUST_001");

        org.meveo.apiv2.payments.CustomerBalance customerBalanceDto = ImmutableCustomerBalance.builder().id(1L).build();
        customerAccountDto.setId(1L);
        customerAccountDto.setCode("CUST_001");

        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setId(1L);

        List<String> occToExcludes = List.of("INV_ADV");

        AccountOperationsDetails accountOperationsDetails = ImmutableAccountOperationsDetails.builder()
                .customerAccount(customerAccountDto)
                .customerBalance(customerBalanceDto)
                .transactionalCurrency(currencyDto)
                .excludeAOs(occToExcludes)
                .build();

        OCCTemplate template = new OCCTemplate();
        template.setId(1L);
        template.setOccCategory(CREDIT);
        template.setCode("ADJ_INV");
        OCCTemplate template2 = new OCCTemplate();
        template2.setId(2L);
        template2.setOccCategory(DEBIT);
        template2.setCode("REF_DDT");
        CustomerBalance customerBalance = createCustomerBalance(true, asList(template, template2));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setId(1L);
        TradingCurrency tradingCurrency = new TradingCurrency();
        tradingCurrency.setId(1L);

        //When
        when(customerAccountService.findById(1L)).thenReturn(customerAccount);
        when(tradingCurrencyService.findById(1L)).thenReturn(tradingCurrency);
        doReturn(customerBalance).when(customerBalanceService).findById(1L);

        ArgumentCaptor<Long> customerAccountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> currencyCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ArrayList<String>> linkedOccTemplates = ArgumentCaptor.forClass(ArrayList.class);
        ArgumentCaptor<ArrayList<String>> occTemplatesToExclude = ArgumentCaptor.forClass(ArrayList.class);

        customerBalanceService.getAccountOperations(accountOperationsDetails);

        //Then
        Mockito.verify(accountOperationService).getAccountOperations(customerAccountCaptor.capture(), currencyCaptor.capture(), linkedOccTemplates.capture(), occTemplatesToExclude.capture());
        Assertions.assertThat(customerAccountCaptor.getValue()).isNotNull();
        Assertions.assertThat(customerAccountCaptor.getValue()).isEqualTo(customerAccountDto.getId());
        Assertions.assertThat(currencyCaptor.getValue()).isNotNull();
        Assertions.assertThat(currencyCaptor.getValue()).isEqualTo(currencyDto.getId());
        Assertions.assertThat(linkedOccTemplates.getValue()).isEqualTo(List.of("ADJ_INV", "REF_DDT"));
        Assertions.assertThat(occTemplatesToExclude.getValue()).isEqualTo(List.of("INV_ADV"));
    }

    @Test
    public void shouldThrowCustomerBalanceNotFoundExceptionWhenGetAccountOperations() {
        //Given
        org.meveo.apiv2.payments.CustomerBalance customerBalanceDto = ImmutableCustomerBalance.builder().id(1L).build();
        AccountOperationsDetails accountOperationsDetails = ImmutableAccountOperationsDetails.builder()
                .customerBalance(customerBalanceDto)
                .build();

        //When
        doThrow(new BadRequestException("Customer Balance does not exist")).when(customerBalanceService).findById(1L);

        //Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> customerBalanceService.getAccountOperations(accountOperationsDetails));
        assertThat(exception.getMessage(), is("Customer Balance does not exist"));

    }

    @Test
    public void shouldThrowCustomerAccountNotFoundExceptionWhenGetAccountOperations() {
        //Given
        org.meveo.apiv2.payments.CustomerBalance customerBalanceDto = ImmutableCustomerBalance.builder().id(1L).build();
        CustomerAccountDto customerAccountDto = new CustomerAccountDto();
        customerAccountDto.setId(1L);
        customerAccountDto.setCode("CUST_001");
        AccountOperationsDetails accountOperationsDetails = ImmutableAccountOperationsDetails.builder()
                .customerBalance(customerBalanceDto)
                .customerAccount(customerAccountDto)
                .build();
        OCCTemplate template = new OCCTemplate();
        template.setId(1L);
        template.setOccCategory(CREDIT);
        template.setCode("ADJ_INV");
        OCCTemplate template2 = new OCCTemplate();
        template2.setId(2L);
        template2.setOccCategory(DEBIT);
        template2.setCode("REF_DDT");
        CustomerBalance customerBalance = createCustomerBalance(true, asList(template, template2));

        //When
        doReturn(customerBalance).when(customerBalanceService).findById(1L);
        doThrow(new BadRequestException("Customer Account does not exist")).when(customerAccountService).findById(1L);

        //Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> customerBalanceService.getAccountOperations(accountOperationsDetails));
        assertThat(exception.getMessage(), is("Customer Account does not exist"));

    }

    enum ErrorMessage {
        MAX_LIMIT_REACHED("Customer balance number reached limit, max balance allowed : 6"),
        MORE_THAN_ONE_CB_FOUND("One default balance already exists"),
        OCC_TEMPLATE_NOT_FOUND("Occ template with id 1 does not exists"),
        EMPTY_CREDIT_LINE("Credit line should not be empty"),
        EMPTY_DEBIT_LINE("Debit line should not be empty");

        ErrorMessage(String message) {
            this.message = message;
        }

        private String message;

        public String errorMessage() {
            return message;
        }
    }

    @Test(expected = ValidationException.class)
    public void shouldThrowValidationErrorIfFAEIsUsed() {
        OCCTemplate template = new OCCTemplate();
        template.setId(2L);
        template.setCode("INV_FAE");
        template.setOccCategory(CREDIT);
        CustomerBalance customerBalance = createCustomerBalance(true, singletonList(template));

        when(paramBeanFactory.getInstance()).thenReturn(paramBean);
        when(paramBean.getPropertyAsInteger("max.customer.balance", 6)).thenReturn(6);
        doReturn(5L).when(customerBalanceService).count();
        doReturn(empty()).when(customerBalanceService).findDefaultCustomerBalance();
        when(occTemplateService.findById(2L)).thenReturn(template);

        customerBalanceService.create(customerBalance);
    }
}