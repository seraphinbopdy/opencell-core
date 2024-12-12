/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */
package org.meveo.model.payments;

import static jakarta.persistence.EnumType.STRING;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;
import org.meveo.model.ObservableEntity;

/**
 * PreAuthorization
 *
 * @author anasseh
 */
@Entity
@ObservableEntity
@Table(name = "ar_pre_authorization")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "ar_pre_authorization_seq") })
@NamedQueries({
		@NamedQuery(name = "PreAuthorization.listToCancel", query = "Select pa  from PreAuthorization as pa  where pa.transactionDate<:dateToCancelIN and pa.status = 'AUTORISED' "),
		@NamedQuery(name = "PreAuthorization.PaToCapture", query = "Select pa  from PreAuthorization as pa  where pa.cardPaymentMethod.id=:cardPmIdIN and pa.status = 'AUTORISED' ")
})
public class PreAuthorization extends AuditableEntity {

    private static final long serialVersionUID = 1L;


    /**
     * Operation category toRefund/None
     */
    @Column(name = "status")
    @Enumerated(STRING)
    private PreAuthorizationStatusEnum status;

    /**
     * Reference
     */
    @Column(name = "reference", unique = true)
    @Size(max = 255)
    private String reference;

    /**
     * Amount with tax
     */
    @Column(name = "amount", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal amount;

    /**
     * Tax amount
     */
    @Column(name = "amount_captured", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal amountCaptured;
    
    /**
     * Associated Customer account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id")
    private CustomerAccount customerAccount;
    
    /**
     * Associated CardPaymentMethod
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_payment_method_id")
    private CardPaymentMethod cardPaymentMethod;
    
    /**
     * Associated PaymentGateway
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_gateway_id")
    private PaymentGateway paymentGateway;
    
    /**
     * Transaction date.
     */
    @Column(name = "transaction_date")
    private Date transactionDate;
    
    /**
     * Capture date.
     */
    @Column(name = "capture_date")
    private Date captureDate;
    
    /**
     * Cancel date.
     */
    @Column(name = "cancel_date")
    private Date cancelDate;
  
    @Column(name = "error_detail", length = 2000)
    private String errorDetail;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;

    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

   
    public CustomerAccount getCustomerAccount() {
        return customerAccount;
    }

    public void setCustomerAccount(CustomerAccount customerAccount) {
        this.customerAccount = customerAccount;
    }

    @Override
    public int hashCode() {
        return 961 + ("PreAuthorization" + id).hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof PreAuthorization)) {
            return false;
        }

        PreAuthorization other = (PreAuthorization) obj;
        if (id != null && other.getId() != null && id.equals(other.getId())) {
            return true;
        }
        if (reference == null) {
            if (other.reference != null)
                return false;
        } else if (!reference.equals(other.reference))
            return false;
        return true;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

	public PreAuthorizationStatusEnum getStatus() {
		return status;
	}

	public void setStatus(PreAuthorizationStatusEnum status) {
		this.status = status;
	}

	public BigDecimal getAmountCaptured() {
		return amountCaptured;
	}

	public void setAmountCaptured(BigDecimal amountCaptured) {
		this.amountCaptured = amountCaptured;
	}

	public CardPaymentMethod getCardPaymentMethod() {
		return cardPaymentMethod;
	}

	public void setCardPaymentMethod(CardPaymentMethod cardPaymentMethod) {
		this.cardPaymentMethod = cardPaymentMethod;
	}

	public Date getCaptureDate() {
		return captureDate;
	}

	public void setCaptureDate(Date captureDate) {
		this.captureDate = captureDate;
	}

	public Date getCancelDate() {
		return cancelDate;
	}

	public void setCancelDate(Date cancelDate) {
		this.cancelDate = cancelDate;
	}

	public PaymentGateway getPaymentGateway() {
		return paymentGateway;
	}

	public void setPaymentGateway(PaymentGateway paymentGateway) {
		this.paymentGateway = paymentGateway;
	}   
    
    
}
