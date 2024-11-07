package org.meveo.service.script;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.meveo.model.payments.PaymentStatusEnum.REJECTED;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.model.billing.Invoice;
import org.meveo.model.payments.Payment;
import org.meveo.model.payments.PaymentHistory;
import org.meveo.model.payments.RecordedInvoice;
import org.meveo.model.payments.RejectedPayment;
import org.meveo.service.payments.impl.PaymentHistoryService;
import org.meveo.service.payments.impl.PaymentService;
import org.meveo.service.payments.impl.RecordedInvoiceService;
import org.meveo.service.payments.impl.RejectedPaymentService;

public class EnterLitigationScript extends Script {

    private final RejectedPaymentService rejectedPaymentService
            = getServiceInterface(RejectedPaymentService.class.getSimpleName());
    private final RecordedInvoiceService recordedInvoiceService
            = getServiceInterface(RecordedInvoiceService.class.getSimpleName());
    private final PaymentHistoryService paymentHistoryService
            = getServiceInterface(PaymentHistoryService.class.getSimpleName());
    private final PaymentService paymentService
            = getServiceInterface(PaymentService.class.getSimpleName());

    @Override
    public void execute(Map<String, Object> context) throws BusinessException {
        log.info("Execute enter litigation script");

        Object paymentRejectId = ofNullable(context.get("rejectedPayment"))
                .orElseThrow(() -> new BusinessException("No payment reject provided"));
        RejectedPayment rejectedPayment = rejectedPaymentService.findById((Long) paymentRejectId);
        String litigationReason = (String) ofNullable(context.get("litigationReason"))
                .orElse(rejectedPayment.getRejectedDescription());
        Payment associatedPayment = paymentService.findByRejectPayment(rejectedPayment.getId());

        PaymentHistory paymentHistory =
                paymentHistoryService.findPaymentHistoryByPaymentIdAndPaymentStatus(associatedPayment.getId(), REJECTED);
        List<RecordedInvoice> recordedInvoices = paymentHistory.getListAoPaid().stream()
                .filter(accountOperation -> accountOperation instanceof RecordedInvoice)
                .map(accountOperation -> (RecordedInvoice) accountOperation)
                .collect(toList());

        log.info("{} invoice will be sent to litigation", recordedInvoices.size());
        recordedInvoices.forEach(recordedInvoice -> sendToLitigation(recordedInvoice, litigationReason));

        String invoiceNumbers = recordedInvoices.stream()
                                         .map(RecordedInvoice::getInvoice)
                                         .map(Invoice::getInvoiceNumber)
                                         .collect(Collectors.joining(", "));

        context.put(REJECTION_ACTION_RESULT, true);
        context.put(REJECTION_ACTION_REPORT, "Invoices [" + invoiceNumbers + "] have entered litigation.");
        log.info("Enter litigation script successfully executed");
    }

    private void sendToLitigation(RecordedInvoice recordedInvoice, String reason) {
        try {
            recordedInvoiceService.setLitigation(recordedInvoice, reason);
        } catch (BusinessException exception) {
            throw new BusinessApiException(exception.getMessage());
        }
    }
}
