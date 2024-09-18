package org.meveo.model.payments;

import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.SqlTypes;
import org.meveo.model.BusinessCFEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ModuleItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@ModuleItem
@CustomFieldEntity(cftCodePrefix = "PaymentRejectionCode")
@Table(name = "ar_payment_rejection_code")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "ar_payment_rejection_code_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "PaymentRejectionCode.findByCodeAndPaymentGateway", query = "SELECT rc from PaymentRejectionCode rc where rc.code = :code and rc.paymentGateway.id = :paymentGatewayId"),
        @NamedQuery(name = "PaymentRejectionCode.clearAllByPaymentGateway", query = "DELETE from PaymentRejectionCode rc where rc.paymentGateway.id = :paymentGatewayId"),
        @NamedQuery(name = "PaymentRejectionCode.clearAll", query = "DELETE from PaymentRejectionCode"),
        @NamedQuery(name = "PaymentRejectionCode.findByPaymentGateway", query = "SELECT rc from PaymentRejectionCode rc where rc.paymentGateway.id = :paymentGatewayId") })
public class PaymentRejectionCode extends BusinessCFEntity {

    /**
     * PaymentGateway associated to the rejection code.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_gateway_id", nullable = false, unique = true)
    private PaymentGateway paymentGateway;

    /**
     * Translated descriptions in JSON format with language code as a key and translated description as a value
     **/
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description_i18n", columnDefinition = "jsonb")
    private Map<String, String> descriptionI18n;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_rejection_codes_group_id")
    private PaymentRejectionCodesGroup paymentRejectionCodesGroup;

    public PaymentGateway getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public Map<String, String> getDescriptionI18n() {
        return descriptionI18n;
    }

    public void setDescriptionI18n(Map<String, String> descriptionI18n) {
        this.descriptionI18n = descriptionI18n;
    }

    public PaymentRejectionCodesGroup getPaymentRejectionCodesGroup() {
        return paymentRejectionCodesGroup;
    }

    public void setPaymentRejectionCodesGroup(PaymentRejectionCodesGroup paymentRejectionCodesGroup) {
        this.paymentRejectionCodesGroup = paymentRejectionCodesGroup;
    }
}
