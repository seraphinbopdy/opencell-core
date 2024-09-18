package org.meveo.model.billing;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;
import org.meveo.model.ObservableEntity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@ObservableEntity
@Cacheable
@Table(name = "iso_icd")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "iso_icd_seq"), @Parameter(name = "increment_size", value = "1") })
public class IsoIcd extends AuditableEntity {

    private static final long serialVersionUID = -8492067649913788802L;

    @Column(name = "code", length = 5)
    @Size(max = 10)
    private String code;

    @Column(name = "scheme_name", length = 500)
    @Size(max = 500)
    private String schemeName;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }
}
