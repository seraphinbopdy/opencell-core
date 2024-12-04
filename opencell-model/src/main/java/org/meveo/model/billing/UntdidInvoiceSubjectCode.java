package org.meveo.model.billing;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;
import org.meveo.model.ObservableEntity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@ObservableEntity
@Cacheable
@Table(name = "untdid_4451_invoice_subject_code")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "untdid_4451_invoice_subject_code_seq"), @Parameter(name = "increment_size", value = "1") })
public class UntdidInvoiceSubjectCode extends AuditableEntity {

    private static final long serialVersionUID = 8577308821246643756L;

    @Column(name = "code_name", length = 500)
    private String codeName;

    @Column(name = "code", length = 10)
    private String code;

    public String getCodeName() {
        return codeName;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
