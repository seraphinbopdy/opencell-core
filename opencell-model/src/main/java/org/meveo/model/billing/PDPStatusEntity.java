package org.meveo.model.billing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pdp_status")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "pdp_status_seq"), @Parameter(name = "increment_size", value = "1") })
public class PDPStatusEntity extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "transmitted_format", nullable = false)
    private TransmittedFormatEnum transmittedFormatEnum;

    @Column(name = "origin", nullable = false)
    private String origin;

    @Column(name = "return_code")
    private Integer returnCode;

    @Column(name = "label")
    private String label;

    @Column(name = "invoice_identifier")
    private String invoiceIdentifier;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @JoinColumn(name = "invoice_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Invoice invoice;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PDPStatusEnum status;

    @Column(name = "deposit_date")
    private Date depositDate;

    @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinTable(name = "pdp_status_and_history", joinColumns = @JoinColumn(name = "pdpstatus_id"), inverseJoinColumns = @JoinColumn(name = "history_id"))
    private List<PDPStatusHistory> pdpStatusHistories = new ArrayList<>();

    public TransmittedFormatEnum getTransmittedFormatEnum() {
        return transmittedFormatEnum;
    }

    public void setTransmittedFormatEnum(TransmittedFormatEnum transmittedFormatEnum) {
        this.transmittedFormatEnum = transmittedFormatEnum;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Integer getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(Integer returnCode) {
        this.returnCode = returnCode;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getInvoiceIdentifier() {
        return invoiceIdentifier;
    }

    public void setInvoiceIdentifier(String invoiceIdentifier) {
        this.invoiceIdentifier = invoiceIdentifier;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public PDPStatusEnum getStatus() {
        return status;
    }

    public void setStatus(PDPStatusEnum status) {
        this.status = status;
    }

    public Date getDepositDate() {
        return depositDate;
    }

    public void setDepositDate(Date depositDate) {
        this.depositDate = depositDate;
    }

    public List<PDPStatusHistory> getPdpStatusHistories() {
        return pdpStatusHistories;
    }

    public void setPdpStatusHistories(List<PDPStatusHistory> pdpStatusHistories) {
        this.pdpStatusHistories = pdpStatusHistories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PDPStatusHistory))
            return false;
        if (!super.equals(o))
            return false;
        PDPStatusEntity that = (PDPStatusEntity) o;
        return getTransmittedFormatEnum() == that.getTransmittedFormatEnum() && Objects.equals(getOrigin(), that.getOrigin()) && Objects.equals(getReturnCode(), that.getReturnCode())
                && Objects.equals(getLabel(), that.getLabel()) && Objects.equals(getInvoiceIdentifier(), that.getInvoiceIdentifier()) && Objects.equals(getInvoiceNumber(), that.getInvoiceNumber())
                && Objects.equals(getInvoice(), that.getInvoice()) && getStatus() == that.getStatus() && Objects.equals(getDepositDate(), that.getDepositDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTransmittedFormatEnum(), getOrigin(), getReturnCode(), getLabel(), getInvoiceIdentifier(), getInvoiceNumber(), getInvoice(), getStatus(), getDepositDate());
    }

}
