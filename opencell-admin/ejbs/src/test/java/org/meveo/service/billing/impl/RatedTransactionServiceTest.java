package org.meveo.service.billing.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.runner.RunWith;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.AccountingCode;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.DiscountPlanInstance;
import org.meveo.model.billing.DiscountPlanInstanceStatusEnum;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.InvoiceType;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RatedTransactionStatusEnum;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TradingLanguage;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletInstance;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.catalog.DiscountPlanItem;
import org.meveo.model.catalog.DiscountPlanItemTypeEnum;
import org.meveo.model.catalog.DiscountPlanStatusEnum;
import org.meveo.model.catalog.DiscountPlanTypeEnum;
import org.meveo.model.cpq.contract.BillingRule;
import org.meveo.model.cpq.contract.Contract;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.tax.TaxClass;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RatedTransactionServiceTest {
    @Spy
    @InjectMocks
    private RatedTransactionService ratedTransactionService;
    
    public List<RatedTransaction> initData() {
        Seller seller = new Seller();

        CustomerAccount ca = new CustomerAccount();
        ca.setId(1001L);
        BillingAccount ba = new BillingAccount();
        ba.setCustomerAccount(ca);
        ba.setId(1002L);

        TradingLanguage tradingLanguage = new TradingLanguage();
        tradingLanguage.setLanguageCode("en");
        tradingLanguage.setId(3L);

        ba.setTradingLanguage(tradingLanguage);

        List<RatedTransaction> rts = new ArrayList<RatedTransaction>();

        UserAccount ua1 = new UserAccount();
        WalletInstance wallet1 = new WalletInstance();
        wallet1.setId(1005L);
        wallet1.setCode("wallet1");
        ua1.setCode("ua1");
        ua1.setWallet(wallet1);
        ua1.setBillingAccount(ba);
        ua1.setId(6L);

        UserAccount ua2 = new UserAccount();

        Subscription subscription1 = new Subscription();
        subscription1.setCode("subsc1");
        subscription1.setUserAccount(ua1);
        subscription1.setId(1009L);

        DiscountPlan discountPlan = new DiscountPlan();
        discountPlan.setDiscountPlanType(DiscountPlanTypeEnum.PROMO_CODE);
        discountPlan.setStatus(DiscountPlanStatusEnum.IN_USE);

        DiscountPlanItem dpi = new DiscountPlanItem();
        dpi.setDiscountPlan(discountPlan);
        dpi.setDiscountPlanItemType(DiscountPlanItemTypeEnum.FIXED);
        dpi.setDiscountValue(BigDecimal.TEN);
        discountPlan.setDiscountPlanItems(List.of(dpi));

        DiscountPlanInstance discountPlanInstance = new DiscountPlanInstance();
        discountPlanInstance.setDiscountPlan(discountPlan);
        discountPlanInstance.setSubscription(subscription1);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(Calendar.MONTH, -2);
        discountPlanInstance.setStartDate(cal.getTime());
        cal.add(Calendar.MONTH, 8);
        discountPlanInstance.setEndDate(cal.getTime());
        discountPlanInstance.setStatus(DiscountPlanInstanceStatusEnum.ACTIVE);

        subscription1.setDiscountPlanInstances(List.of(discountPlanInstance));

        InvoiceCategory cat1 = new InvoiceCategory();
        cat1.setCode("cat1");
        cat1.setId(1011L);
        AccountingCode accountingCode = new AccountingCode();
        accountingCode.setId(19L);        
        
        InvoiceSubCategory subCat11 = new InvoiceSubCategory();
        subCat11.setInvoiceCategory(cat1);
        subCat11.setCode("subCat11");
        subCat11.setAccountingCode(accountingCode);
        subCat11.setId(1013L);
        
        Tax tax = new Tax();
        tax.setId(1017L);
        tax.setCode("tax1");
        tax.setPercent(new BigDecimal(15));
        
        TaxClass taxClass = new TaxClass();
        taxClass.setId(1018L);
        
        InvoiceType invoiceType = new InvoiceType();
        invoiceType.setId(1004L);
        
        RatedTransaction rt = new RatedTransaction(new Date(), new BigDecimal(15), new BigDecimal(16), new BigDecimal(1), new BigDecimal(2), new BigDecimal(15), new BigDecimal(16), new BigDecimal(1),
            RatedTransactionStatusEnum.OPEN, ua1.getWallet(), ba, ua1, subCat11, null, null, null, null, null, subscription1, null, null, null, null, null, "rt111", "RT111", new Date(), new Date(), seller, tax,
            tax.getPercent(), null, taxClass, accountingCode, null, null, null);
        rt.setId(1020L);
        
        rts.add(rt);
        
        return rts;
    }
    
    void testOk() {
        List<RatedTransaction> rts = initData();
        RatedTransaction rt = rts.get(0);
        Contract c1 = new Contract();
        c1.setBillingAccount(rt.getBillingAccount());
        c1.setId(1028L);
        
        BillingRule br1 = new BillingRule();
        br1.setContract(c1);
        br1.setPriority(1);
        br1.setInvoicedBACodeEL("#{rt.getUserAccount().getBillingAccount().getCode()}");
        br1.setCriteriaEL("#{rt.getUserAccount() != null}");
        BillingAccount billingAccountAvant = rt.getBillingAccount();
        ratedTransactionService.applyInvoicingRules(rts);        
        BillingAccount originBillingAccountTest = rt.getOriginBillingAccount();
        BillingAccount billingAccountApres = rt.getBillingAccount();
        assertEquals(originBillingAccountTest, billingAccountAvant);
        assertNotEquals(billingAccountApres, billingAccountAvant);
    }
    
    void testOnlyOneRuleRedirect() {
        List<RatedTransaction> rts = initData();
        RatedTransaction rt = rts.get(0);
        Contract c1 = new Contract();
        c1.setBillingAccount(rt.getBillingAccount());
        c1.setId(1028L);
        
        BillingRule br1 = new BillingRule();
        br1.setContract(c1);
        br1.setPriority(1);
        br1.setInvoicedBACodeEL("#{rt.getUserAccount().getBillingAccount().getCode()}");
        br1.setCriteriaEL("#{rt.getUserAccount() != null}");
        
        BillingRule br2 = new BillingRule();
        br2.setContract(c1);
        br2.setPriority(1);
        br2.setInvoicedBACodeEL("");
        br2.setCriteriaEL("");
        
        BillingAccount billingAccountAvant = rt.getBillingAccount();
        ratedTransactionService.applyInvoicingRules(rts);        
        BillingAccount originBillingAccountTest = rt.getOriginBillingAccount();
        BillingAccount billingAccountApres = rt.getBillingAccount();
        assertEquals(originBillingAccountTest, billingAccountAvant);
        assertNotEquals(billingAccountApres, billingAccountAvant);
    }
    
    void testNotEvaluateInvoicedBACodeEL() {
        List<RatedTransaction> rts = initData();
        RatedTransaction rt = rts.get(0);
        Contract c1 = new Contract();
        c1.setBillingAccount(rt.getBillingAccount());
        c1.setId(1028L);
        
        BillingRule br1 = new BillingRule();
        br1.setContract(c1);
        br1.setPriority(1);
        br1.setInvoicedBACodeEL("");
        br1.setCriteriaEL("#{rt.getUserAccount() != null}");
        
        BillingRule br2 = new BillingRule();
        br2.setContract(c1);
        br2.setPriority(1);
        br2.setInvoicedBACodeEL("");
        br2.setCriteriaEL("");
        
        ratedTransactionService.applyInvoicingRules(rts);        
        RatedTransactionStatusEnum statusTest = rt.getStatus();
        String rejectReasonTest = rt.getRejectReason();
        assertTrue(statusTest.equals(RatedTransactionStatusEnum.REJECTED));
        assertTrue(rejectReasonTest.contains("Error evaluating invoicedBillingAccountCodeEL"));
    }
    
    void testNotEvaluateCriteriaEL() {
        List<RatedTransaction> rts = initData();
        RatedTransaction rt = rts.get(0);
        Contract c1 = new Contract();
        c1.setBillingAccount(rt.getBillingAccount());
        c1.setId(1028L);
        
        BillingRule br1 = new BillingRule();
        br1.setContract(c1);
        br1.setPriority(1);
        br1.setInvoicedBACodeEL("#{rt.getUserAccount().getBillingAccount().getCode()}");
        br1.setCriteriaEL("");
        
        BillingRule br2 = new BillingRule();
        br2.setContract(c1);
        br2.setPriority(1);
        br2.setInvoicedBACodeEL("");
        br2.setCriteriaEL("");
        
        ratedTransactionService.applyInvoicingRules(rts);
        RatedTransactionStatusEnum statusTest = rt.getStatus();
        String rejectReasonTest = rt.getRejectReason();
        assertTrue(statusTest.equals(RatedTransactionStatusEnum.REJECTED));
        assertTrue(rejectReasonTest.contains("Error evaluating criteriaEL"));
    }
}
