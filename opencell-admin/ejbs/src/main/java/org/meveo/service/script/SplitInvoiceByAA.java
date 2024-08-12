package org.meveo.service.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.InvoiceLine;
import org.meveo.model.billing.InvoiceLinesGroup;
import org.meveo.model.billing.InvoiceType;
import org.meveo.model.payments.PaymentMethod;

public class SplitInvoiceByAA extends org.meveo.service.script.Script {

    public void execute(Map<String, Object> context) throws BusinessException {

        List<InvoiceLine> invoiceLines = (List<InvoiceLine>) context.get("invoiceLines");
        if(invoiceLines==null) {
            return;
        }
        
        Map<String, InvoiceLinesGroup> groupedByAccountingArticleGroups = 
                invoiceLines.stream().collect(Collectors.groupingBy(
                                invoiceLine -> invoiceLine.getInvoiceKey() + "_" + invoiceLine.getAccountingArticle().getId(),
                                Collectors.collectingAndThen(
                                    Collectors.toList(), invoiceLineList -> buildILG(invoiceLineList, context)
                                )
                            ));
            context.put(Script.RESULT_VALUE, new ArrayList(groupedByAccountingArticleGroups.values()));
    }

    private InvoiceLinesGroup buildILG(List<InvoiceLine> invoiceLineList, Map<String, Object> context) {
        InvoiceType invoiceType = (InvoiceType) context.get("invoiceType");
        InvoiceLine firstInvoiceLine = invoiceLineList.get(0);
        BillingAccount billingAccount = firstInvoiceLine.getBillingAccount();
        Seller seller = billingAccount.getSeller();
        InvoiceLinesGroup result = new InvoiceLinesGroup(billingAccount , billingAccount.getBillingCycle(), seller, invoiceType,
                false, firstInvoiceLine.getInvoiceKey()+ "_" + firstInvoiceLine.getAccountingArticle().getId(),(PaymentMethod) context.get("paymentMethod"), null);
        result.setInvoiceLines(invoiceLineList);
        return result;
    }
}