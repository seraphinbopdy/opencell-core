package org.meveo.model.securityDeposit;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HugeEntity implements Serializable {

    private static final long serialVersionUID = 3475716285714300643L;

    private String entityClass;

    private List<String> hugeLists;

    private List<String> mandatoryFilterFields;

    public String getEntityClass() {
        return entityClass;
    }

    public HugeEntity setEntityClass(String entityClass) {
        this.entityClass = entityClass;
        return this;
    }

    public List<String> getHugeLists() {
        return hugeLists;
    }

    public HugeEntity setHugeLists(List<String> hugeLists) {
        this.hugeLists = hugeLists;
        return this;
    }

    public List<String> getMandatoryFilterFields() {
        return mandatoryFilterFields;
    }

    public HugeEntity setMandatoryFilterFields(List<String> mandatoryFilterFields) {
        this.mandatoryFilterFields = mandatoryFilterFields;
        return this;
    }

    @Override
    public int hashCode() {
        return 961 + (this.getClass().getName() + entityClass).hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof HugeEntity)) { // Fails with proxed objects: getClass() != obj.getClass()){
            return false;
        }

        HugeEntity other = (HugeEntity) obj;

        String concatThis = entityClass + (hugeLists != null ? hugeLists.toString() : "") + (mandatoryFilterFields != null ? mandatoryFilterFields.toString() : "");
        String concatOther = other.getEntityClass() + (other.getHugeLists() != null ? other.getHugeLists().toString() : "") + (other.getMandatoryFilterFields() != null ? other.getMandatoryFilterFields().toString() : "");

        return concatThis.equals(concatOther);
    }
}
