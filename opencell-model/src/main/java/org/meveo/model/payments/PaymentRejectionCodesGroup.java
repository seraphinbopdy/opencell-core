package org.meveo.model.payments;

import java.util.List;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessCFEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ModuleItem;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@ModuleItem
@CustomFieldEntity(cftCodePrefix = "PaymentRejectionCodesGroup")
@Table(name = "ar_payment_rejection_codes_group")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "ar_payment_rejection_codes_group_seq"), @Parameter(name = "increment_size", value = "1") })
public class PaymentRejectionCodesGroup extends BusinessCFEntity {

    /**
     * PaymentGateway associated to the rejection code.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_gateway_id")
    private PaymentGateway paymentGateway;

    /**
     * Associated rejection codes.
     */
    @OneToMany(mappedBy = "paymentRejectionCodesGroup", fetch = FetchType.LAZY)
    private List<PaymentRejectionCode> paymentRejectionCodes;

    /**
     * Associated rejection actions.
     */
    @OneToMany(mappedBy = "paymentRejectionCodesGroup", fetch = FetchType.LAZY)
    private List<PaymentRejectionAction> paymentRejectionActions;

    public PaymentGateway getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public List<PaymentRejectionCode> getPaymentRejectionCodes() {
        return paymentRejectionCodes;
    }

    public void setPaymentRejectionCodes(List<PaymentRejectionCode> paymentRejectionCodes) {
        this.paymentRejectionCodes = paymentRejectionCodes;
    }

    public List<PaymentRejectionAction> getPaymentRejectionActions() {
        return paymentRejectionActions;
    }

    public void setPaymentRejectionActions(List<PaymentRejectionAction> paymentRejectionActions) {
        this.paymentRejectionActions = paymentRejectionActions;
    }
}
