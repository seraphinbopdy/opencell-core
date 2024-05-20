package org.meveo.api.payment;

import static org.junit.Assert.assertEquals;
import static org.meveo.model.payments.AccountOperationStatus.EXPORTED;
import static org.meveo.model.payments.AccountOperationStatus.POSTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.account.CustomerAccountDto;
import org.meveo.api.dto.payment.ImmutableCustomerBalanceExportDto;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.apiv2.generic.ImmutableGenericFieldDetails;
import org.meveo.apiv2.generic.core.GenericHelper;
import org.meveo.apiv2.generic.services.GenericApiLoadService;
import org.meveo.apiv2.payments.ImmutableAccountOperationsDetails;
import org.meveo.apiv2.payments.ImmutableAccountOperationsResult;
import org.meveo.apiv2.payments.ImmutableCustomerBalance;
import org.meveo.apiv2.settings.globalSettings.service.AdvancedSettingsApiService;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.ResourceUtils;
import org.meveo.model.Auditable;
import org.meveo.model.admin.Currency;
import org.meveo.model.admin.User;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.AccountOperationStatus;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.security.MeveoUser;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.CustomerBalanceService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AccountOperationApiTest {

    @InjectMocks
    private AccountOperationApi accountOperationApi;

    @Mock
    private AccountOperationService accountOperationService;

    @Mock
    private UserService userService;

    @Mock
    private MeveoUser currentUser;
    
    @Mock
    private AdvancedSettingsApiService advancedSettingsApiService;
    
    @Mock
    private CustomerBalanceService customerBalanceService;

    @Mock
    private CustomerAccountService customerAccountService;
    
    @Mock
    private GenericApiLoadService genericApiLoadService;

    private static final String CURRENT_USER_NAME = "opencell admin";

    private AccountOperation accountOperation;
//    private User user;
    private final Date newAccountingDate = new Date();

    @Before
    public void setUp() {
        accountOperation = createAccountOperation(POSTED, new Date());
//        user = createUserWithRoles(true);

        when(accountOperationService.findById(any())).thenReturn(accountOperation);
//        when(currentUser.getUserName()).thenReturn(CURRENT_USER_NAME);
//        when(userService.findByUsername(any(), anyBoolean())).thenReturn(user);
        when(accountOperationService.update(any())).thenReturn(accountOperation);
    }

    @Test
    public void shouldUpdateAccountingDate() {
        AccountOperation accountOperation = accountOperationApi.updateAccountingDate(1l, newAccountingDate);

        assertEquals((Long) 1l, accountOperation.getId());
        assertEquals(newAccountingDate, accountOperation.getAccountingDate());
    }

    @Test(expected = EntityDoesNotExistsException.class)
    public void testAccountingDateWhenAoNotFound() {
        when(accountOperationService.findById(any())).thenReturn(null);

        AccountOperation updatedAccountOperation = accountOperationApi.updateAccountingDate(2l, newAccountingDate);
        assertEquals(newAccountingDate, updatedAccountOperation.getAccountingDate());
    }

    @Test(expected = BusinessException.class)
    public void shouldNotUpdateAccountingDateWhenAOIsExported() {
        AccountOperation accountOperation = createAccountOperation(EXPORTED, new Date());

        when(accountOperationService.findById(any())).thenReturn(accountOperation);

        AccountOperation updatedAccountOperation = accountOperationApi.updateAccountingDate(1l, newAccountingDate);
        assertEquals(newAccountingDate, updatedAccountOperation.getAccountingDate());
    }

//    @Test(expected = BusinessException.class)
//    public void shouldNotUpdateAccountingDateWhenUserHasNotFinanceManagementPermission() {
//        AccountOperation accountOperation = createAccountOperation(POSTED, new Date());
//        User user = createUserWithRoles(false);
//
//        when(accountOperationService.findById(any())).thenReturn(accountOperation);
//        when(userService.findByUsername(any(), anyBoolean())).thenReturn(user);
//
//        AccountOperation updatedAccountOperation = accountOperationApi.updateAccountingDate(1l, newAccountingDate);
//        assertEquals(newAccountingDate, updatedAccountOperation.getAccountingDate());
//    }

    private AccountOperation createAccountOperation(AccountOperationStatus status, Date accountingDate) {
        Auditable auditable = new Auditable();
        auditable.setUpdater("opencell.admin");
        auditable.setCreator("opencell.admin");
        AccountOperation accountOperation = new AccountOperation();
        accountOperation.setStatus(status);
        accountOperation.setId(1l);
        accountOperation.setCode("ACCOUNT_OPERATION_CODE");
        accountOperation.setDescription("ACCOUNT OPERATION CODE");
        accountOperation.setAuditable(auditable);
        accountOperation.setAccountingDate(accountingDate);
        return accountOperation;
    }

    private User createUserWithRoles(Boolean withFinanceManagementPermission) {
        User user = new User();
        user.setId(1l);
        user.setCode("opencell_admin_code");
        user.setUserName(CURRENT_USER_NAME);
        user.setDescription("opencell admin");
        
        return user;
    }

    @Test
    public void shouldCreateExport() {

        CustomerAccountDto ca = new CustomerAccountDto();
        ca.setCode("CA_CODE");

        ImmutableCustomerBalanceExportDto dto = ImmutableCustomerBalanceExportDto.builder()
                                                                                   .customerAccount(ca)
                                                                                   .customerBalance(ImmutableCustomerBalance.builder()
                                                                                                                            .id(1l)
                                                                                                                            .build())
                                                                                   .genericFieldDetails(List.of(ImmutableGenericFieldDetails.builder()
                                                                                                                                            .name("reference")
                                                                                                                                            .header("reference")
                                                                                                                                            .build()))
                                                                                   .build();

        when(advancedSettingsApiService.findByCode(anyString())).thenReturn(Optional.empty());
        when(customerBalanceService.getAccountOperations(dto)).thenReturn(ImmutableAccountOperationsResult.builder()
                                                                                                          .accountOperationIds(List.of(1L))
                                                                                                          .balance(BigDecimal.valueOf(100))
                                                                                                          .totalCredit(BigDecimal.valueOf(200))
                                                                                                          .totalDebit(BigDecimal.valueOf(300))
                                                                                                          .build());

        CustomerAccount caEntity = new CustomerAccount();
        caEntity.setCode("CA_CODE");
        TradingCurrency tradingCurrency = new TradingCurrency();
        Currency currency = new Currency();
        currency.setCurrencyCode("USD");
        tradingCurrency.setCurrency(currency);
        caEntity.setTradingCurrency(tradingCurrency);
        when(customerAccountService.findByCode("CA_CODE")).thenReturn(caEntity);
        
        try(var mock = mockStatic(GenericHelper.class)) {
            mock.when(GenericHelper::getDefaultLimit).thenReturn(100L);
            accountOperationApi.exportCustomerBalance("EXCEL", dto);
        }
            
        ArgumentCaptor<PaginationConfiguration> mapperCaptor = ArgumentCaptor.forClass(PaginationConfiguration.class);
        verify(genericApiLoadService).export(eq(AccountOperation.class), mapperCaptor.capture(), isNull(), eq(dto.getGenericFieldDetails()), eq("EXCEL"), eq(AccountOperation.class.getSimpleName()), isNotNull(), isNull(), isNull(), isNull(), isNotNull());
    }


}
