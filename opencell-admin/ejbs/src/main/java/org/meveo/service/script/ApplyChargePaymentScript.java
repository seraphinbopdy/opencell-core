package org.meveo.service.script;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.meveo.apiv2.accounts.ApplyOneShotChargeListModeEnum.ROLLBACK_ON_ERROR;
import static org.meveo.commons.utils.StringUtils.isNotBlank;
import static org.meveo.model.catalog.OneShotChargeTemplateTypeEnum.OTHER;
import static org.meveo.service.base.ValueExpressionWrapper.evaluateExpression;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.account.ApplyOneShotChargeInstanceRequestDto;
import org.meveo.apiv2.accounts.ApplyOneShotChargeListInput;
import org.meveo.apiv2.accounts.ImmutableApplyOneShotChargeListInput;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.Payment;
import org.meveo.model.payments.PaymentHistory;
import org.meveo.model.payments.PaymentRejectionActionReport;
import org.meveo.model.payments.RecordedInvoice;
import org.meveo.model.payments.RejectedPayment;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.catalog.impl.OneShotChargeTemplateService;
import org.meveo.service.payments.impl.PaymentHistoryService;
import org.meveo.service.payments.impl.PaymentRejectionActionReportService;
import org.meveo.service.payments.impl.PaymentService;
import org.meveo.service.payments.impl.RejectedPaymentService;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ApplyChargePaymentScript extends Script {

    private final OneShotChargeTemplateService oneShotChargeTemplateService
            = getServiceInterface(OneShotChargeTemplateService.class.getSimpleName());
    private final PaymentService paymentService
            = getServiceInterface(PaymentService.class.getSimpleName());
    private final RejectedPaymentService rejectedPaymentService
            = getServiceInterface(RejectedPaymentService.class.getSimpleName());
    private final SubscriptionService subscriptionService
            = getServiceInterface(SubscriptionService.class.getSimpleName());
    private final PaymentRejectionActionReportService paymentRejectionActionReportService
            = getServiceInterface(PaymentRejectionActionReportService.class.getSimpleName());
    private final PaymentHistoryService paymentHistoryService
            = getServiceInterface(PaymentHistoryService.class.getSimpleName());

    @Override
    public void execute(Map<String, Object> context) throws BusinessException {
        log.info("Execute apply charge script");
        OneShotChargeTemplate oneShotCharge =
                oneShotChargeTemplateService.refreshOrRetrieve((OneShotChargeTemplate) context.get("chargeTemplate"));
        Object paymentReject = ofNullable(context.get("rejectedPayment"))
                .orElseThrow(() -> new BusinessException("No payment reject provided"));
        RejectedPayment rejectedPayment = rejectedPaymentService.findById((Long) paymentReject);
        PaymentRejectionActionReport actionReport =
                ofNullable(paymentRejectionActionReportService.findByCode(rejectedPayment.getRejectedCode()))
                        .orElseThrow(() -> new BusinessException("No action report found"));
        ofNullable(oneShotCharge).orElseThrow(()
                ->  new BusinessException("One-shot other charge does’t exist for payment rejection action "
                + actionReport.getAction().getId() + " [gateway="+ rejectedPayment.getPaymentGateway().getCode()
                + ", rejection code="+ actionReport.getCode() +"]"));
        if (oneShotCharge.getOneShotChargeTemplateType() != OTHER) {
            throw new BusinessException("Charge [code=" + oneShotCharge.getCode()
                    + "] is not a one-shot charge of type ‘other' "
                    + "for payment rejection action " +  actionReport.getAction().getId()
                    + " [gateway="+ rejectedPayment.getPaymentGateway().getCode()
                    + ", rejection code=" + actionReport.getCode() + "]");
        }
        BigDecimal amountOverride = context.get("amountOverride") != null ? (BigDecimal) context.get("amountOverride") : null;
        String descriptionOverride = (String) context.get("descriptionOverride");
        if(amountOverride != null
                && (oneShotCharge.getAmountEditable() != null && !oneShotCharge.getAmountEditable())) {
            throw new BusinessException("Charge [code="+ oneShotCharge.getCode()
                    +"] does’t allow amount override for payment rejection action " + actionReport.getAction().getId()
                    + " [gateway="+ rejectedPayment.getPaymentGateway().getCode()
                    + ", rejection code=" + actionReport.getCode() + "]");
        }
        Payment payment = paymentService.findByRejectPayment(rejectedPayment.getId());
        String subscriptionSearchScope = (String) context.get("subscriptionSearchScope");

        List<Subscription> subscriptions = loadSubscriptions(subscriptionSearchScope, payment);
        if (subscriptions == null || subscriptions.isEmpty()) {
            throw new BusinessException("No subscription matches the criteria "
                    + "for payment rejection action " + actionReport.getAction().getId() + " [gateway="
                    + rejectedPayment.getPaymentGateway().getCode()
                    + ", rejection code=" + actionReport.getCode() + "] "
                    + "and rejected payment [id=" + rejectedPayment.getId()
                    + ", reference=" + rejectedPayment.getReference() + "]");
        }
        String subscriptionFilter = (String) context.get("subscriptionFilter");
        String subscriptionSelection = (String) context.get("subscriptionSelection");
        String subscriptionOperator = (String) context.get("subscriptionOperator");
        if (isNotBlank(subscriptionFilter)) {
            String result = evaluateExpression(subscriptionFilter, String.class,
                    payment, rejectedPayment, rejectedPayment.getInvoices(),
                    rejectedPayment.getCustomerAccount().getBillingAccounts(),
                    rejectedPayment.getCustomerAccount(),
                    subscriptions);
            log.info("subscription filter evaluated, result {}", result);
            subscriptions = applyFilter(subscriptionOperator, subscriptions, result);
        }
        Subscription selectedSubscription = null;
        if (!subscriptions.isEmpty() && subscriptions.size() > 1) {
            subscriptions.sort(comparing(Subscription::getSubscriptionDate));
            if (subscriptionSelection.equals("newest")) {
                selectedSubscription = subscriptions.get(subscriptions.size() - 1);
            } else {
                selectedSubscription = subscriptions.get(0);
            }
        } else if (subscriptions.size() == 1) {
            selectedSubscription = subscriptions.get(0);
        }
        ofNullable(selectedSubscription)
                .orElseThrow(() ->  new BusinessException("No subscription matches the criteria "
                        + "for payment rejection action " + actionReport.getAction().getId() + " [gateway="
                        + rejectedPayment.getPaymentGateway().getCode()
                        + ", rejection code=" + actionReport.getCode() + "] "
                        + "and rejected payment [id=" + rejectedPayment.getId()
                        + ", reference=" + rejectedPayment.getReference() + "]"));
        Boolean generateRTs = ofNullable((Boolean) context.get("generateRTs")).orElse(false);

        ApplyOneShotChargeListInput applyOneShotChargeListInput =
                buildApplyChargeInput(generateRTs, selectedSubscription, oneShotCharge,
                        amountOverride, descriptionOverride, rejectedPayment, payment, actionReport);

        subscriptionService.applyOneShotChargeList(applyOneShotChargeListInput);
        log.info("Charge applied to subscription{}", selectedSubscription.getCode());
        context.put(REJECTION_ACTION_REPORT, "Charge [code=" + oneShotCharge.getCode()
                +"] has been applied to subscription [code=" + selectedSubscription.getCode() +"]");
        context.put(REJECTION_ACTION_RESULT, true);
        log.info("Apply charge script successfully executed");
    }

    private List<Subscription> loadSubscriptions(String subscriptionSearchScope, Payment payment) {
        List<Subscription> subscriptions;
        if (subscriptionSearchScope.equalsIgnoreCase("customer account")
                || subscriptionSearchScope.equalsIgnoreCase("billing account")) {
            if(payment.getCustomerAccount() == null || (payment.getCustomerAccount() != null
                    && payment.getCustomerAccount().getBillingAccounts() == null
                    && subscriptionSearchScope.equalsIgnoreCase("billing account"))) {
                return emptyList();
            }
            subscriptions = payment.getCustomerAccount().getBillingAccounts()
                    .stream()
                    .map(BillingAccount::getUsersAccounts)
                    .flatMap(Collection::stream)
                    .map(UserAccount::getSubscriptions)
                    .flatMap(Collection::stream)
                    .collect(toList());
        } else {
            Optional<PaymentHistory> paymentHistory = paymentHistoryService.findByPaymentId(payment.getId());
            if(paymentHistory.isEmpty()) {
                return emptyList();
            }
            if(paymentHistory.get().getListAoPaid() == null || paymentHistory.get().getListAoPaid().isEmpty()) {
                return emptyList();
            }
            List<Invoice> invoices = paymentHistory.get().getListAoPaid()
                    .stream()
                    .filter(accountOperation -> accountOperation instanceof RecordedInvoice)
                    .map(AccountOperation::getInvoices)
                    .findFirst()
                    .orElse(emptyList());
            subscriptions = invoices
                    .stream()
                    .map(Invoice::getSubscriptions)
                    .flatMap(Collection::stream)
                    .collect(toList());
        }
        return subscriptions;
    }

    private List<Subscription> applyFilter(String subscriptionOperator,
                                           List<Subscription> subscriptions, String result) {
        switch (subscriptionOperator) {
            case "equals":
                subscriptions = subscriptions.stream()
                        .filter(subscription -> subscription.getCode().equals(result))
                        .collect(toList());
                break;
            case "begins with":
                subscriptions = subscriptions.stream()
                        .filter(subscription -> subscription.getCode().startsWith(result))
                        .collect(toList());
                break;
            case "contains":
                subscriptions = subscriptions.stream()
                        .filter(subscription -> subscription.getCode().contains(result))
                        .collect(toList());
                break;
            case "ends with":
                return subscriptions.stream()
                        .filter(subscription -> subscription.getCode().endsWith(result))
                        .collect(toList());
        }
        return subscriptions;
    }

    private ApplyOneShotChargeListInput buildApplyChargeInput(Boolean generateRTs,
                                                              Subscription selectedSubscription,
                                                              OneShotChargeTemplate oneShotChargeTemplate,
                                                              BigDecimal amount,
                                                              String description,
                                                              RejectedPayment rejectedPayment,
                                                              Payment payment,
                                                              PaymentRejectionActionReport actionReport) {
        ApplyOneShotChargeInstanceRequestDto applyOneShotChargeInstanceRequestDto = new ApplyOneShotChargeInstanceRequestDto();
        applyOneShotChargeInstanceRequestDto.setSubscription(selectedSubscription.getCode());
        applyOneShotChargeInstanceRequestDto.setOneShotCharge(oneShotChargeTemplate.getCode());
        applyOneShotChargeInstanceRequestDto.setAmountWithoutTax(amount);
        applyOneShotChargeInstanceRequestDto.setAmountWithTax(amount);
        applyOneShotChargeInstanceRequestDto.setQuantity(new BigDecimal(1));
        applyOneShotChargeInstanceRequestDto.setOperationDate(rejectedPayment.getRejectedDate());
        applyOneShotChargeInstanceRequestDto.setDescription(description);
        applyOneShotChargeInstanceRequestDto.setCriteria1("payment.reference="
                + payment.getReference() + "," + "rejectedPayment.id=" + rejectedPayment.getId());
        applyOneShotChargeInstanceRequestDto
                .setCriteria2(isNotBlank(rejectedPayment.getRejectedDescription())
                        ? "rejectionDescription=" + rejectedPayment.getRejectedDescription() : "");
        if (actionReport != null && actionReport.getAction() != null) {
            applyOneShotChargeInstanceRequestDto.setCriteria3("rejectionAction.id=" + actionReport.getAction().getId());
        }
        return ImmutableApplyOneShotChargeListInput
                .builder()
                .isVirtual(false)
                .isGenerateRTs(generateRTs)
                .isReturnWalletOperationDetails(false)
                .isRateTriggeredEdr(false)
                .isReturnCounters(false)
                .isReturnWalletOperations(false)
                .mode(ROLLBACK_ON_ERROR)
                .maxDepth(0)
                .chargesToApply(List.of(applyOneShotChargeInstanceRequestDto))
                .build();
    }
}
