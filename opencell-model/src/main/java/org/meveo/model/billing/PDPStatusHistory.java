package org.meveo.model.billing;

import java.util.Date;
import java.util.Objects;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "pdp_status_history")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "pdp_status_history_seq"), @Parameter(name = "increment_size", value = "1") })
public class PDPStatusHistory extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "pdp_status", nullable = false)
    private PDPStatusEnum pdpStatus;

    @Column(name = "event_date", nullable = false)
    private Date eventDate;

    @Column(name = "origin", nullable = false)
    private String origin;

    public PDPStatusEnum getPdpStatus() {
        return pdpStatus;
    }

    public void setPdpStatus(PDPStatusEnum pdpStatus) {
        this.pdpStatus = pdpStatus;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PDPStatusEntity))
            return false;
        if (!super.equals(o))
            return false;
        PDPStatusHistory pdpStatusEntity1 = (PDPStatusHistory) o;
        return getPdpStatus() == pdpStatusEntity1.getPdpStatus() && Objects.equals(getEventDate(), pdpStatusEntity1.getEventDate()) && Objects.equals(getOrigin(), pdpStatusEntity1.getOrigin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getPdpStatus(), getEventDate(), getOrigin());
    }
}
