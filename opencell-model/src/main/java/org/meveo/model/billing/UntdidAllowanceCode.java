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
@Table(name = "untdid_5189_allowance_code")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "untdid_5189_allowance_code_seq"), @Parameter(name = "increment_size", value = "1") })
public class UntdidAllowanceCode extends AuditableEntity {

    private static final long serialVersionUID = -6011686926063877705L;

    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
