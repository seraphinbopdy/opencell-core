package org.meveo.service.script;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.meveo.model.payments.PaymentMethodEnum.CARD;
import static org.meveo.model.payments.PaymentStatusEnum.REJECTED;
import static org.meveo.model.shared.DateUtils.addDaysToDate;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.Invoice;
import org.meveo.model.payments.*;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.payments.impl.PaymentGatewayService;
import org.meveo.service.payments.impl.PaymentHistoryService;
import org.meveo.service.payments.impl.PaymentService;
import org.meveo.service.payments.impl.RecordedInvoiceService;
import org.meveo.service.payments.impl.RejectedPaymentService;

import java.util.List;
import java.util.Map;

public class RetryRejectionPaymentScript extends Script {

    private final RejectedPaymentService rejectedPaymentService
            = getServiceInterface(RejectedPaymentService.class.getSimpleName());
    private final InvoiceService invoiceService
            = getServiceInterface(InvoiceService.class.getSimpleName());
    private final RecordedInvoiceService recordedInvoiceService
            = getServiceInterface(RecordedInvoiceService.class.getSimpleName());
    private final PaymentHistoryService paymentHistoryService
            = getServiceInterface(PaymentHistoryService.class.getSimpleName());
    private final PaymentService paymentService
            = getServiceInterface(PaymentService.class.getSimpleName());
    private final PaymentGatewayService paymentGatewayService
            = getServiceInterface(PaymentGatewayService.class.getSimpleName());

    @Override
    public void execute(Map<String, Object> context) throws BusinessException {
        log.info("Execute retry payment script");

        Object paymentReject = ofNullable(context.get("rejectedPayment"))
                .orElseThrow(() -> new BusinessException("No payment reject provided"));
        RejectedPayment rejectedPayment = rejectedPaymentService.findById((Long) paymentReject);
        Payment payment = paymentService.findByRejectPayment(rejectedPayment.getId());
        int maxRetries = context.get("maxRetries") != null ? (Integer) context.get("maxRetries") : 1;
        PaymentHistory paymentHistory = ofNullable(paymentHistoryService
                .findPaymentHistoryByPaymentIdAndPaymentStatus(payment.getId(), REJECTED))
                .orElseThrow(() -> new BusinessException("No payment history found for payment id = " + payment.getId()));
        RecordedInvoice recordedInvoice = getRecordedInvoice(paymentHistory);
        final long paymentRequests = ofNullable(recordedInvoice.getPaymentRequests()).orElse(0L);
        try {
            if (paymentRequests > maxRetries) {
                boolean litigationAfterRetry = context.get("litigationAfterRetry") != null
                        ? (Boolean) context.get("litigationAfterRetry") : false;
                if (litigationAfterRetry) {
                    if (recordedInvoice.getInvoices() != null && !recordedInvoice.getInvoices().isEmpty()) {
                        log.info("Send linked invoices to litigation after reaching max retries");
                        List<Long> invoices = recordedInvoice.getInvoices()
                                .stream()
                                .map(Invoice::getId)
                                .collect(toList());
                        recordedInvoice.setLitigationReason("Maximum payment retries reached.");
                        recordedInvoice.setMatchingStatus(MatchingStatusEnum.I);
                        invoiceService.getEntityManager()
                                .createNamedQuery("Invoice.sendToLitigation")
                                .setParameter("ids", invoices)
                                .executeUpdate();
                        recordedInvoiceService.update(recordedInvoice);
                    }
                }
            } else if (paymentRequests == 1) {
                createPaymentRequest(payment, paymentHistory);
                recordedInvoice = recordedInvoiceService.refreshOrRetrieve(recordedInvoice);
                int firstDelay = context.get("firstRetryDelay") != null ? (Integer) context.get("firstRetryDelay") : 0;
                recordedInvoice.setCollectionDate(addDaysToDate(rejectedPayment.getRejectedDate(), firstDelay));
                recordedInvoiceService.update(recordedInvoice);
            } else {
                createPaymentRequest(payment, paymentHistory);
                recordedInvoice = recordedInvoiceService.refreshOrRetrieve(recordedInvoice);
                int nextRetriesDelay
                        = context.get("nextRetriesDelay") != null ? (Integer) context.get("nextRetriesDelay") : 0;
                recordedInvoice.setCollectionDate(addDaysToDate(rejectedPayment.getRejectedDate(), nextRetriesDelay));
                recordedInvoiceService.update(recordedInvoice);
            }
            context.put(REJECTION_ACTION_RESULT, true);
        } catch (Exception exception) {
            log.error("Error executing retry payment script", exception);
            throw new BusinessException(exception);
        }
    }

    private RecordedInvoice getRecordedInvoice(PaymentHistory paymentHistory) {
        return paymentHistory.getListAoPaid().stream()
                .filter(accountOperation -> accountOperation instanceof RecordedInvoice)
                .map(accountOperation -> (RecordedInvoice) accountOperation)
                .findFirst()
                .orElseThrow(() -> new BusinessException("No recorded invoice"));
    }

    private void createPaymentRequest(Payment payment, PaymentHistory paymentHistory) throws Exception {
        CustomerAccount customerAccount = payment.getCustomerAccount();
        PaymentMethod preferredPaymentMethod = getPreferredPaymentMethod(customerAccount, payment.getPaymentMethod());
        PaymentGateway paymentGateway =
                paymentGatewayService.getPaymentGateway(customerAccount, preferredPaymentMethod, null);
        List<Long> aosToPay = paymentHistory.getListAoPaid().stream().map(AccountOperation::getId).collect(toList());

        if (Boolean.TRUE.equals(payment.getIsManualPayment())) {
            paymentService.createManualPaymentFromRejectedPayment(payment, payment.getCollectionDate(), aosToPay);
        } else {
            if (CARD.equals(payment.getPaymentMethod()) && CARD.equals(preferredPaymentMethod.getPaymentType())) {
                CardPaymentMethod paymentMethod = (CardPaymentMethod) preferredPaymentMethod;
                paymentService.doPayment(customerAccount, paymentHistory.getAmountCts(), aosToPay,
                        TRUE, TRUE, paymentGateway, paymentMethod.getHiddenCardNumber(),
                        paymentMethod.getCardNumber(), paymentMethod.getHiddenCardNumber(),
                        paymentMethod.getExpirationMonthAndYear(), paymentMethod.getCardType(),
                        TRUE, preferredPaymentMethod.getPaymentType(), true, null);
            } else {
                paymentService.doPayment(customerAccount, paymentHistory.getAmountCts(), aosToPay,
                        TRUE, TRUE, paymentGateway, null, null, null,
                        null, null, TRUE, preferredPaymentMethod.getPaymentType(), true, null);
            }
        }

        log.info("Payment request successfully created");
    }

    private PaymentMethod getPreferredPaymentMethod(CustomerAccount customerAccount, PaymentMethodEnum paymentMethod) {
        return customerAccount
                .getPaymentMethods()
                .stream()
                .filter(pm -> paymentMethod.equals(pm.getPaymentType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No payment method found for customer account"
                        + customerAccount.getCode()));
    }
}