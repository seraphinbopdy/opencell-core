package org.meveo.model.payments;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.SqlTypes;
import org.meveo.model.BusinessCFEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ModuleItem;
import org.meveo.model.scripts.ScriptInstance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@ModuleItem
@CustomFieldEntity(cftCodePrefix = "PaymentRejectionAction")
@Table(name = "ar_payment_rejection_action")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "ar_payment_rejection_action_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({
        @NamedQuery(name = "PaymentRejectionAction.getActionsByRejectionCode", query = "select distinct (pra) from PaymentRejectionAction pra where pra.paymentRejectionCodesGroup in (select rc.paymentRejectionCodesGroup from PaymentRejectionCode rc where rc.code = :code and (:paymentGatewayId is null or rc.paymentGateway.id = :paymentGatewayId))") })
public class PaymentRejectionAction extends BusinessCFEntity {

    /**
     * Action sequence
     */
    @Column(name = "sequence")
    private int sequence;

    /**
     * Script instance associated to rejection action.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_instance_id")
    private ScriptInstance script;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_rejection_codes_group_id")
    private PaymentRejectionCodesGroup paymentRejectionCodesGroup;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "script_parameters", columnDefinition = "jsonb")
    private Map<String, String> scriptParameters;

    @OneToMany(mappedBy = "action", fetch = FetchType.LAZY)
    private List<PaymentRejectionActionReport> rejectionActionReports;

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public ScriptInstance getScript() {
        return script;
    }

    public void setScript(ScriptInstance script) {
        this.script = script;
    }

    public PaymentRejectionCodesGroup getPaymentRejectionCodesGroup() {
        return paymentRejectionCodesGroup;
    }

    public void setPaymentRejectionCodesGroup(PaymentRejectionCodesGroup paymentRejectionCodesGroup) {
        this.paymentRejectionCodesGroup = paymentRejectionCodesGroup;
    }

    public Map<String, String> getScriptParameters() {
        return scriptParameters;
    }

    public void setScriptParameters(Map<String, String> scriptParameters) {
        this.scriptParameters = scriptParameters;
    }

    public List<PaymentRejectionActionReport> getRejectionActionReports() {
        return rejectionActionReports;
    }

    public void setRejectionActionReports(List<PaymentRejectionActionReport> rejectionActionReports) {
        this.rejectionActionReports = rejectionActionReports;
    }
}
