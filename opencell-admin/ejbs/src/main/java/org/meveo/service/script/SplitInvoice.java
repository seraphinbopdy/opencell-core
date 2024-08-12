package org.meveo.service.script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.MethodCallingUtils;
import org.meveo.commons.utils.PersistenceUtils;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingCycle;
import org.meveo.model.billing.InvoiceLine;
import org.meveo.model.billing.InvoiceLinesGroup;
import org.meveo.model.billing.InvoiceType;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.admin.impl.SellerService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.InvoiceLineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public class SplitInvoice extends org.meveo.service.script.Script {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final MethodCallingUtils methodCallingUtils = getServiceInterface(MethodCallingUtils.class.getSimpleName());
    SellerService sellerService = getServiceInterface("SellerService");
    InvoiceLineService invoiceLineService = getServiceInterface("InvoiceLineService");
    BillingAccountService billingAccountService = getServiceInterface("BillingAccountService");
    private static final List<String> PRESTATION_ARTICLE_CODES = List.of(
            "AR_3257_PRESTATION_RECURRENT",
            "AR_3257_PRESTATION_PONCTUEL"
    );
    private static final List<String> RECUURENT_AND_ONESHOT_INV_SUBCAT_CODES = List.of(
            "ISCAT_REC",
            "ISCAT_OST",
            "ISCAT_IND",
            "ICAT_REMISE2",
            "ICAT_REMISE3",
            "ICAT_REMISE4",
            "ICAT_REMISE1"
    );
    private static final String BC_AFFRANCHIGO ="BC_AFFRANCHIGO" ;
    public void execute(Map<String, Object> context) throws BusinessException {
        final String KEY_DEFAULT_BILL = "DEFAULT_BILL";
        final String KEY_APART_BILL = "APART_BILL";
        final String KEY_ASAP_BILL = "ASAP_BILL";
        log.debug("EXECUTE context {}", context);
        List<InvoiceLine> allIL = (List<InvoiceLine>) context.get("invoiceLines");
        InvoiceType invoiceType = (InvoiceType) context.get("invoiceType");
        PaymentMethod paymentMethod = (PaymentMethod) context.get("paymentMethod");
        BillingAccount billingAccount = billingAccountService
                .refreshOrRetrieve((BillingAccount) context.get("CONTEXT_ENTITY"));
        Seller seller = billingAccount.getCustomerAccount().getCustomer().getSeller() != null
                ? billingAccount.getCustomerAccount().getCustomer().getSeller()
                : sellerService.findByCode("MAIN_SELLER");
        BillingCycle billingCycle = billingAccount.getBillingCycle();
        Map<String, InvoiceLinesGroup> invoices = new HashMap<String, InvoiceLinesGroup>();
        log.debug("SplitInvoice/1-billingAccount.getNextInvoiceDate()={}", billingAccount.getNextInvoiceDate());
        boolean dedicatedInvoiceFlag;
        boolean invoicingAsapFlag;
        EntityManager em = invoiceLineService.getEntityManager();
        for (InvoiceLine il : allIL) {
            il = PersistenceUtils.initializeAndUnproxy(il);
            il = invoiceLineService.findById(il.getId(), Arrays.asList("tax"));
            String invoicingGroupKey = KEY_DEFAULT_BILL;
            dedicatedInvoiceFlag = Boolean.FALSE;
            invoicingAsapFlag = Boolean.FALSE;
            if (isInvoiceTypeCom(invoiceType) && isIlReccurentOrOneShot(il)) {
                // BILLING-2645: traiter un cas specifique : les IL frais dde prestation auront toujours une Qunatite =1 et le PU =Montant HT
                if (billingCycle.getCode().equals(BC_AFFRANCHIGO) && isArticlePrestation(il.getAccountingArticle().getCode()) ){
                    InvoiceLine finalIl = il;
                    methodCallingUtils.callMethodInNewTx(() -> {
                        finalIl.setQuantity(BigDecimal.ONE);
                        finalIl.setUnitPrice(finalIl.getAmountWithoutTax());
                        invoiceLineService.update(finalIl);
                    } );
                }
                // traiter le cas non usage : oneShot ou recurrent ou le param2 != falg|flag
                RatedTransaction rt = il.getRatedTransactions().get(0);
                invoicingGroupKey = buildKeyInvoice( rt, Optional.of(il.getValueDate()), KEY_DEFAULT_BILL);
            } else if (il.getRatedTransactions() !=null && !il.getRatedTransactions().isEmpty()) {
                RatedTransaction rt = il.getRatedTransactions().get(0);
                if (null != rt) {
                    if (null != rt.getParameter2()) {
                        //dedicatedInvoiceFlag| invoicingASAP
                        String[] rtParam2Flags = rt.getParameter2().split("\\|");
                        try {
                            dedicatedInvoiceFlag = "true".equalsIgnoreCase(rtParam2Flags[0]);
                            invoicingAsapFlag = "true".equalsIgnoreCase(rtParam2Flags[1]);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            throw new BusinessException("Parameter2 malformed for RatedTransaction id : " + rt.getId());
                        }
                        log.debug("dedicatedInvoiceFlag {}", dedicatedInvoiceFlag);
                        log.debug("invoicingASAPFlag {}", invoicingAsapFlag);
                    }
                    // dedicatedFlag==true and still to be billed ==> to be billed apart
                    if (dedicatedInvoiceFlag) {
                        invoicingGroupKey = buildKeyInvoiceForApart(rt, KEY_APART_BILL, rt.getUuid());
                    } else if (invoicingAsapFlag) {
                        invoicingGroupKey = buildKeyInvoice(rt, Optional.empty(), KEY_ASAP_BILL);
                    } else {
                        // on time (default) + flag "dedicated" == false
                        invoicingGroupKey = buildKeyInvoice(rt,Optional.empty(), KEY_DEFAULT_BILL);
                    }
                    log.debug("SplitInvoice/3-invoicingGroupKey={}", invoicingGroupKey);
                }
            }
            if (!invoices.containsKey(invoicingGroupKey)) {
                log.debug("New invoice lines group {}", invoicingGroupKey);
                InvoiceLinesGroup ilGroup = new InvoiceLinesGroup(billingAccount, billingCycle, seller,
                        invoiceType, false/* prepaid */, invoicingGroupKey, paymentMethod, null);
                invoices.put(invoicingGroupKey, ilGroup);
            }
            log.debug("add(il) : {}", il);
            log.debug("ilGroup1 : {}", invoices.get(invoicingGroupKey).getInvoiceLines());
            em.detach(il);
            // Add il into the invoicingGroup
            invoices.get(invoicingGroupKey).getInvoiceLines().add(il);
            invoices.entrySet().stream().forEach(invoice -> log.info("BillingAccount[{}], InvoiceType[{}], InvoicingGroup[{}] => {} ILs",
                    invoice.getValue().getBillingAccount().getCode(), invoiceType.getCode(), invoice.getKey(),
                    invoice.getValue().getInvoiceLines().size()));
            log.debug("invoices.values() : {}", invoices.values().size());
            // Returns the list to the invoicing process
            ArrayList ar = new ArrayList(invoices.values());
            log.debug("ArrayList : {}", ar);
            context.put(Script.RESULT_VALUE, ar);
            log.debug("context.put RESULT_VALUE : {}", context);
        }
    }
    private boolean isIlReccurentOrOneShot(InvoiceLine il ) {
        if(il == null) {
            log.warn("LCR : null");
        } else {
            log.warn("LCR : {}", il.getAccountingArticle());
            log.warn("LCR : {}", il.getAccountingArticle().getInvoiceSubCategory());
            log.warn("LCR : {}", il.getAccountingArticle().getInvoiceSubCategory().getCode());
        }
        return RECUURENT_AND_ONESHOT_INV_SUBCAT_CODES.contains(il.getAccountingArticle().getInvoiceSubCategory().getCode());
    }
    private boolean isArticlePrestation(String invoiceLineArticle) {
        return PRESTATION_ARTICLE_CODES.contains(invoiceLineArticle);
    }
    private boolean isInvoiceTypeCom(InvoiceType invoiceType) {
        return invoiceType !=null && invoiceType.getCode().equals("COM") ;
    }
    private String buildKeyInvoice(RatedTransaction rt ,Optional<Date> ilUsageDate, String key) {
        Date usageDate ;
        if (rt==null && ilUsageDate.isPresent()) {
            usageDate = ilUsageDate.get();
        } else {
            usageDate = rt.getInvoicingDate()!= null ? rt.getInvoicingDate() : rt.getUsageDate();
        }
        StringBuilder strBuilder = new StringBuilder();
        if (usageDate == null) {
            usageDate = new Date();
        }
        return strBuilder
                .append(key)
                .append("_")
                .append(DateUtils.formatDateWithPattern(Date.from(usageDate.toInstant()), "MM-yyyy")).toString();
    }
    private String buildKeyInvoiceForApart(RatedTransaction rt, String key, String uuid) {
        return buildKeyInvoice(rt,Optional.empty(), key)+uuid;
    }
}
