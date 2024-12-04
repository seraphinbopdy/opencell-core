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
@Table(name = "untdid_4461_payment_means")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "untdid_4461_payment_means_seq"), @Parameter(name = "increment_size", value = "1") })
public class UntdidPaymentMeans extends AuditableEntity {

    private static final long serialVersionUID = -1024979001287985755L;

    @Column(name = "code", length = 500)
    @Size(max = 20)
    private String code;

    @Column(name = "code_name", length = 500)
    @Size(max = 20)
    private String codeName;

    @Column(name = "usage_in_en16931", length = 500)
    @Size(max = 20)
    private String usageEN16931;

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

    public String getUsageEN16931() {
        return usageEN16931;
    }

    public void setUsageEN16931(String usageEN16931) {
        this.usageEN16931 = usageEN16931;
    }

}
