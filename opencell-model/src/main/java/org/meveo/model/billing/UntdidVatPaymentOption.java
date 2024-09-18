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
@Table(name = "untdid_2475_vat_payment_option")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "untdid_2475_vat_payment_option_seq"), @Parameter(name = "increment_size", value = "1") })
public class UntdidVatPaymentOption extends AuditableEntity {

    private static final long serialVersionUID = 6328943927385071967L;

    @Column(name = "code_2005", length = 10)
    private String code2005;

    @Column(name = "value_2005", length = 500)
    private String value2005;

    @Column(name = "code_2475", length = 10)
    private String code2475;

    @Column(name = "value_2475", length = 500)
    private String value2475;

    public String getCode2005() {
        return code2005;
    }

    public void setCode2005(String code2005) {
        this.code2005 = code2005;
    }

    public String getValue2005() {
        return value2005;
    }

    public void setValue2005(String value2005) {
        this.value2005 = value2005;
    }

    public String getCode2475() {
        return code2475;
    }

    public void setCode2475(String code2475) {
        this.code2475 = code2475;
    }

    public String getValue2475() {
        return value2475;
    }

    public void setValue2475(String value2475) {
        this.value2475 = value2475;
    }
}
