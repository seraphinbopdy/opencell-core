/**
 * 
 */
package org.meveo.service.payments.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.jpa.JpaAmpNewTx;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.CardPaymentMethod;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.DDPaymentMethod;
import org.meveo.model.payments.OperationCategoryEnum;
import org.meveo.model.payments.Payment;
import org.meveo.model.payments.PaymentErrorTypeEnum;
import org.meveo.model.payments.PaymentHistory;
import org.meveo.model.payments.PaymentMethod;
import org.meveo.model.payments.PaymentStatusEnum;
import org.meveo.model.payments.Refund;
import org.meveo.service.base.PersistenceService;

import static org.meveo.model.payments.PaymentStatusEnum.REJECTED;

/**
 * @author anasseh
 * @lastModifiedVersion 5.0.2
 */
@Stateless
public class PaymentHistoryService extends PersistenceService<PaymentHistory> {
	
    /** The account operation service. */
    @Inject
    private AccountOperationService accountOperationService;

	private static final String DEFAULT_CUSTOMER_CODE = "DEFAULT_CUSTOMER_CODE";

    @JpaAmpNewTx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addHistoryInNewTransaction(CustomerAccount customerAccount, Payment payment,Refund refund, Long amountCts, PaymentStatusEnum status, String errorCode, String errorMessage, String externalPaymentId,
            PaymentErrorTypeEnum errorType, OperationCategoryEnum operationCategory, String paymentGatewayCode, PaymentMethod paymentMethod,List<Long> aoIdsToPay) throws BusinessException {
		// DUPLICATED during process issue https://opencellsoft.atlassian.net/browse/INTRD-8946
		// To void side effect by using new trasction for caller service
		List<AccountOperation> aoToPay = new ArrayList<AccountOperation>();
		if(aoIdsToPay != null) {
			for(Long aoId : aoIdsToPay) {
				aoToPay.add(accountOperationService.findById(aoId));
			}
		}
		addHistoryAOs(customerAccount, payment, refund, amountCts, status, errorCode, errorMessage, externalPaymentId, errorType, operationCategory, paymentGatewayCode, paymentMethod, aoToPay);
    }

    public void addHistory(CustomerAccount customerAccount, Payment payment,Refund refund, Long amountCts, PaymentStatusEnum status, String errorCode, String errorMessage, String externalPaymentId,
            PaymentErrorTypeEnum errorType, OperationCategoryEnum operationCategory, String paymentGatewayCode, PaymentMethod paymentMethod,List<Long> aoIdsToPay) throws BusinessException {
    	List<AccountOperation> aoToPay = new ArrayList<AccountOperation>();
    	if(aoIdsToPay != null) {
        	for(Long aoId : aoIdsToPay) {
        		aoToPay.add(accountOperationService.findById(aoId));
            }
    	}
    	addHistoryAOs(customerAccount, payment, refund, amountCts, status, errorCode, errorMessage, externalPaymentId, errorType, operationCategory, paymentGatewayCode, paymentMethod, aoToPay);
    }

	public void addHistoryAOs(CustomerAccount customerAccount, Payment payment, Refund refund, Long amountCts, PaymentStatusEnum status, String errorCode, String errorMessage,
			String externalPaymentId, PaymentErrorTypeEnum errorType, OperationCategoryEnum operationCategory, String paymentGatewayCode, PaymentMethod paymentMethod, List<AccountOperation> aoToPay)
			throws BusinessException {
		PaymentHistory paymentHistory = new PaymentHistory();
		if(customerAccount != null) {
			paymentHistory.setCustomerAccountCode(customerAccount.getCode());
			paymentHistory.setCustomerAccountName(customerAccount.getName() == null ? null : customerAccount.getName().getFullName());
			if (customerAccount.getCustomer().getSeller() != null) {
				paymentHistory.setSellerCode(customerAccount.getCustomer().getSeller().getCode());
			}
			paymentHistory.setCustomerCode(customerAccount.getCustomer().getCode());
		} else {
			paymentHistory.setCustomerAccountCode(DEFAULT_CUSTOMER_CODE);
		}
		paymentHistory.setPayment(payment);
		paymentHistory.setRefund(refund);
		paymentHistory.setOperationDate(new Date());
		paymentHistory.setAmountCts(amountCts);
		paymentHistory.setErrorCode(errorCode);
		paymentHistory.setErrorMessage(errorMessage);
		paymentHistory.setErrorType(errorType);
		paymentHistory.setExternalPaymentId(externalPaymentId);
		paymentHistory.setOperationCategory(payment != null ? OperationCategoryEnum.CREDIT : OperationCategoryEnum.DEBIT );
		paymentHistory.setSyncStatus(status);
		paymentHistory.setPaymentGatewayCode(paymentGatewayCode);
		paymentHistory.setLastUpdateDate(paymentHistory.getOperationDate());
		if (payment != null) {
			if (payment.getPaymentMethod() != null && payment.getPaymentMethod().isSimple()) {
				paymentHistory.setPaymentMethodType(payment.getPaymentMethod());
				paymentHistory.setPaymentMethodName(payment.getPaymentInfo());
			}
		} else if (refund != null) {
			if (refund.getPaymentMethod() != null && refund.getPaymentMethod().isSimple()) {
				paymentHistory.setPaymentMethodType(refund.getPaymentMethod());
				paymentHistory.setPaymentMethodName(refund.getPaymentInfo());
			}
		}
		if (paymentMethod != null) {
			paymentHistory.setPaymentMethodType(paymentMethod.getPaymentType());
			String pmVal = null;
			if (paymentMethod instanceof DDPaymentMethod) {
				pmVal = ((DDPaymentMethod) paymentMethod).getMandateIdentification();
			}
			if (paymentMethod instanceof CardPaymentMethod) {
				pmVal = ((CardPaymentMethod) paymentMethod).getHiddenCardNumber();
			}
			paymentHistory.setPaymentMethodName(pmVal);
		}

		for (AccountOperation ao : aoToPay) {
			if(ao!=null) {
			if (ao.getPaymentHistories() == null) {
				ao.setPaymentHistories(new ArrayList<>());
			}
			ao.getPaymentHistories().add(paymentHistory);

			if (paymentHistory.getListAoPaid() == null) {
				paymentHistory.setListAoPaid(new ArrayList<>());
			}
			paymentHistory.getListAoPaid().add(ao);
		}
		}
		super.create(paymentHistory);
	}

    public PaymentHistory findHistoryByPaymentId(String paymentId) {
        try {
            QueryBuilder qb = new QueryBuilder(PaymentHistory.class, "a");
            qb.addCriterion("externalPaymentId", "=", paymentId, false);
			List<PaymentHistory> paymentHistories = (List<PaymentHistory>) qb.getQuery(getEntityManager()).getResultList();
            return paymentHistories != null && !paymentHistories.isEmpty() ? paymentHistories.get(0) : null;
        } catch (NoResultException ne) {
            return null;
        }
    }

	public PaymentHistory rejectPaymentHistory(String paymentReference, String rejectionCode, String rejectionComment) {
		PaymentHistory paymentHistory = findHistoryByPaymentId(paymentReference);
		if (paymentHistory != null) {
			paymentHistory.setAsyncStatus(REJECTED);
			paymentHistory.setLastUpdateDate(new Date());
			paymentHistory.setErrorCode(rejectionCode);
			paymentHistory.setErrorMessage(rejectionComment);
			update(paymentHistory);
		}
		return paymentHistory;
	}

	/**
	 * @param paymentId Payment id
	 * @param paymentStatus Payment status
	 * @return PaymentHistory
	 */
	public PaymentHistory findPaymentHistoryByPaymentIdAndPaymentStatus(Long paymentId, PaymentStatusEnum paymentStatus) {
		try {
			QueryBuilder qb = new QueryBuilder(PaymentHistory.class, "a");
			qb.addCriterion("payment.id", "=", paymentId, false);
			qb.addCriterion("status", "=", paymentStatus, false);
			List<PaymentHistory> paymentHistories = (List<PaymentHistory>) qb.getQuery(getEntityManager()).getResultList();
			return paymentHistories != null && !paymentHistories.isEmpty() ? paymentHistories.get(0) : null;
		} catch (NoResultException ne) {
			return null;
		}
	}
}
