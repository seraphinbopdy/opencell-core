package org.meveo.model.billing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.type.SqlTypes;
import org.meveo.model.BusinessEntity;
import org.meveo.model.cpq.enums.OperatorEnum;
import org.meveo.model.scripts.ScriptInstance;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "billing_invoice_validation_rule", uniqueConstraints = @UniqueConstraint(columnNames = { "code" }))
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "billing_invoice_validation_rule_seq"), @Parameter(name = "increment_size", value = "1") })
public class InvoiceValidationRule extends BusinessEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_type_id", nullable = false)
    @NotNull
    private InvoiceType invoiceType;

    @Column(name = "priority")
    @NotNull
    private Integer priority;

    @Column(name = "valid_from")
    private Date validFrom;

    @Column(name = "valid_to")
    private Date validTo;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private ValidationRuleTypeEnum type;

    @Column(name = "fail_status")
    @Enumerated(EnumType.STRING)
    private InvoiceValidationStatusEnum failStatus = InvoiceValidationStatusEnum.REJECTED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "script_instance_id")
    private ScriptInstance validationScript;

    @Column(name = "validation_el")
    private String validationEL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_values", columnDefinition = "jsonb")
    private Map<String, String> ruleValues;

    @Column(name = "evaluation_mode")
    @Enumerated(EnumType.STRING)
    @NotNull
    private EvaluationModeEnum evaluationMode = EvaluationModeEnum.VALIDATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator")
    private OperatorEnum operator = OperatorEnum.OR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private InvoiceValidationRule parentRule;

    @OneToMany(mappedBy = "parentRule", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceValidationRule> subRules = new ArrayList<>();

    @Transient
    private boolean toReorder;

    public InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(InvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public ValidationRuleTypeEnum getType() {
        return type;
    }

    public void setType(ValidationRuleTypeEnum type) {
        this.type = type;
    }

    public ScriptInstance getValidationScript() {
        return validationScript;
    }

    public void setValidationScript(ScriptInstance validationScript) {
        this.validationScript = validationScript;
    }

    public String getValidationEL() {
        return validationEL;
    }

    public void setValidationEL(String validationEL) {
        this.validationEL = validationEL;
    }

    public InvoiceValidationStatusEnum getFailStatus() {
        return failStatus;
    }

    public void setFailStatus(InvoiceValidationStatusEnum failStatus) {
        this.failStatus = failStatus;
    }

    public Map<String, String> getRuleValues() {
        return ruleValues;
    }

    public void setRuleValues(Map<String, String> ruleValues) {
        this.ruleValues = ruleValues;
    }

    public boolean isToReorder() {
        return toReorder;
    }

    public void setToReorder(boolean toReorder) {
        this.toReorder = toReorder;
    }

    public EvaluationModeEnum getEvaluationMode() {
        return evaluationMode;
    }

    public void setEvaluationMode(EvaluationModeEnum evaluationMode) {
        this.evaluationMode = evaluationMode;
    }

    public OperatorEnum getOperator() {
        return operator;
    }

    public void setOperator(OperatorEnum operator) {
        this.operator = operator;
    }

    public InvoiceValidationRule getParentRule() {
        return parentRule;
    }

    public void setParentRule(InvoiceValidationRule parentRule) {
        this.parentRule = parentRule;
    }

    public List<InvoiceValidationRule> getSubRules() {
        return subRules;
    }

    public void setSubRules(List<InvoiceValidationRule> subRules) {
        this.subRules = subRules;
    }

}
