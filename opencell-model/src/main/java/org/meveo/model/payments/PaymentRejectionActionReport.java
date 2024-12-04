package org.meveo.model.payments;

import java.util.Date;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessCFEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ModuleItem;
import org.meveo.model.scripts.ScriptInstance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@ModuleItem
@CustomFieldEntity(cftCodePrefix = "PaymentRejectionActionReport")
@Table(name = "ar_payment_rejection_action_report")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "ar_payment_rejection_action_report_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({
        @NamedQuery(name = "PaymentRejectionActionReport.removeActionReferenceToPendingAndInProgressReports", query = "UPDATE PaymentRejectionActionReport SET action = null, status = 'CANCELED', report=:report WHERE code=:rejectionCode AND status IN ('PENDING', 'RUNNING')"),
        @NamedQuery(name = "PaymentRejectionActionReport.removeActionReference", query = "UPDATE PaymentRejectionActionReport rar SET rar.action = null WHERE rar.code=:rejectionCode") })
public class PaymentRejectionActionReport extends BusinessCFEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_payment_id")
    private RejectedPayment rejectedPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_rejection_action_id")
    private PaymentRejectionAction action;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PaymentRejectionActionStatus status;

    @Column(name = "report")
    private String report;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date")
    private Date endDate;

    /**
     * Script instance associated to payment rejection report.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_script_id")
    private ScriptInstance actionScript;

    public RejectedPayment getRejectedPayment() {
        return rejectedPayment;
    }

    public void setRejectedPayment(RejectedPayment rejectedPayment) {
        this.rejectedPayment = rejectedPayment;
    }

    public PaymentRejectionAction getAction() {
        return action;
    }

    public void setAction(PaymentRejectionAction action) {
        this.action = action;
    }

    public PaymentRejectionActionStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentRejectionActionStatus status) {
        this.status = status;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public ScriptInstance getActionScript() {
        return actionScript;
    }

    public void setActionScript(ScriptInstance actionScript) {
        this.actionScript = actionScript;
    }
}
