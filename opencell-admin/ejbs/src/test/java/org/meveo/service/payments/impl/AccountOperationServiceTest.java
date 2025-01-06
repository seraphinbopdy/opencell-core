package org.meveo.service.payments.impl;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.crm.Provider;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.CashPaymentMethod;
import org.meveo.model.payments.CheckPaymentMethod;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.DDPaymentMethod;
import org.meveo.model.payments.PaymentActionEnum;
import org.meveo.model.payments.PaymentMethodEnum;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AccountOperationServiceTest {
    @Spy
    @InjectMocks
    private AccountOperationService accountOperationService;

    @Mock
    private Provider appProvider;

    @Before
    public void init() {

        when(appProvider.isPaymentDeferral()).thenReturn(true);
        when(appProvider.getMaximumDelay()).thenReturn(3);
        when(appProvider.getMaximumDeferralPerInvoice()).thenReturn(5);
    }

    @Test()
    public void selectedPaymentMethodDoesNotBelongToTheAccountOperationCustomerAccount() {
        AccountOperation accountOperation = new AccountOperation();
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setPaymentMethods(Arrays.asList(new CheckPaymentMethod(), new CashPaymentMethod()));
        accountOperation.setCustomerAccount(customerAccount);
        Date paymentDate = Date.from(LocalDate.now().plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

        try {
            accountOperationService.createDeferralPayments(accountOperation, PaymentMethodEnum.CARD, paymentDate);
        } catch (BusinessException e) {
            assertEquals("The selected payment method does not belong to the account operation customer account", e.getMessage());
            return;
        }
        fail("Exception is expected");
    }

    @Test
    public void paymentExceedsAppProviderMaxDelay() {
        AccountOperation accountOperation = new AccountOperation();
        Date paymentDate = Date.from(LocalDate.now().plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        Date collectionDate = DateUtils.addDays(paymentDate, -2);
        accountOperation.setCollectionDate(collectionDate);

        when(appProvider.getMaximumDelay()).thenReturn(1);

        try {
            accountOperationService.createDeferralPayments(accountOperation, null, paymentDate);
        } catch (BusinessException e) {
            assertEquals("The payment date should not exceed the current collection date by more than 1", e.getMessage());
            return;
        }
        fail("Exception is expected");
    }

    @Test
    public void paymentExceedsAppProviderTheConfiguredMaximumDeferralPerInvoice() {
        AccountOperation accountOperation = new AccountOperation();
        accountOperation.setPaymentDeferralCount(10);

        Date paymentDate = Date.from(LocalDate.now().plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        Date collectionDate = DateUtils.addDays(paymentDate, -2);
        accountOperation.setCollectionDate(collectionDate);
        try {
            accountOperationService.createDeferralPayments(accountOperation, null, paymentDate);
        } catch (BusinessException e) {
            assertEquals("The payment deferral count (10) should not exceeds the configured maximum deferral per invoice (5).", e.getMessage());
            return;
        }
        fail("Exception is expected");
    }

    @Test()
    public void thePaymentDatePlusThreeDaysMustNotBeSaturdayOrSunday() {
        AccountOperation accountOperation = new AccountOperation();
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setPaymentMethods(Arrays.asList(new DDPaymentMethod(), new CashPaymentMethod()));
        accountOperation.setCustomerAccount(customerAccount);
        accountOperation.setPaymentDeferralCount(0);
        when(appProvider.getMaximumDeferralPerInvoice()).thenReturn(1);
        Date paymentDate = Date.from(LocalDate.of(1989, 8, 20).minusDays(3).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        Date collectionDate = DateUtils.addDays(paymentDate, -2);
        accountOperation.setCollectionDate(collectionDate);

        try {
            accountOperationService.createDeferralPayments(accountOperation, PaymentMethodEnum.DIRECTDEBIT, paymentDate);
        } catch (BusinessException e) {
            assertEquals("The payment date plus three days must not be a saturday or sunday.", e.getMessage());
            return;
        }
        fail("Exception is expected");
    }

    @Test
    public void successfullyCreateDeferralPayments() {

        Date paymentDate = Date.from(LocalDate.of(1989, 9, 17).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        Date collectionDate = Date.from(LocalDate.of(1989, 8, 20).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

        AccountOperation accountOperation = new AccountOperation();
        doReturn(accountOperation).when(accountOperationService).update(accountOperation);
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setPaymentMethods(Arrays.asList(new DDPaymentMethod(), new CashPaymentMethod()));
        accountOperation.setCustomerAccount(customerAccount);
        accountOperation.setPaymentDeferralCount(0);
        accountOperation.setCollectionDate(collectionDate);
        when(appProvider.isPaymentDeferral()).thenReturn(true);
        when(appProvider.getMaximumDelay()).thenReturn(50);
        when(appProvider.getMaximumDeferralPerInvoice()).thenReturn(1);
        accountOperationService.createDeferralPayments(accountOperation, PaymentMethodEnum.DIRECTDEBIT, paymentDate);
        assertEquals(accountOperation.getPaymentMethod(), PaymentMethodEnum.DIRECTDEBIT);
        assertEquals(accountOperation.getPaymentDeferralCount(), Integer.valueOf(1));
        assertEquals(accountOperation.getCollectionDate(), paymentDate);
        assertEquals(accountOperation.getPaymentAction(), PaymentActionEnum.PENDING_PAYMENT);
    }
}