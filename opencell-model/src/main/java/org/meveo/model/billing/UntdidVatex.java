package org.meveo.model.billing;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ExportIdentifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@CustomFieldEntity(cftCodePrefix = "UntdidVatex")
@ExportIdentifier({ "code" })
@Entity
@Table(name = "untdid_vatex")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "untdid_vatex_seq"), @Parameter(name = "increment_size", value = "1") })
public class UntdidVatex extends AuditableEntity {

    private static final long serialVersionUID = -7645366420182144384L;

    @Column(name = "code", length = 255)
    @Size(max = 500)
    private String code;

    @Column(name = "code_name", length = 500)
    @Size(max = 500)
    private String codeName;

    @Column(name = "remark", length = 500)
    @Size(max = 500)
    private String remark;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeName() {
        return codeName;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
