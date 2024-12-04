package org.meveo.model.cpq;

import java.util.Objects;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "cpq_product_version_attributes")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "cpq_product_version_attribute_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({
        @NamedQuery(name = "ProductVersionAttribute.findByAttributeAndProductVersion", query = "select pva FROM ProductVersionAttribute pva where pva.attribute.id =:attributeId and pva.productVersion.id =:productVersionId") })
public class ProductVersionAttribute extends AttributeBaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -5934892816847168643L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_version_id", nullable = false)
    @NotNull
    private ProductVersion productVersion;

    public ProductVersionAttribute() {
        super();
    }

    public ProductVersionAttribute(ProductVersionAttribute copy, ProductVersion productVersion) {
        super(copy);
        this.productVersion = productVersion;
    }

    /**
     * @return the productVersion
     */
    public ProductVersion getProductVersion() {
        return productVersion;
    }

    /**
     * @param productVersion the productVersion to set
     */
    public void setProductVersion(ProductVersion productVersion) {
        this.productVersion = productVersion;
    }

    /**
     * @return the sequence
     */
    public Integer getSequence() {
        return sequence;
    }

    /**
     * @param sequence the sequence to set
     */
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductVersionAttribute)) {
            return false;
        }
        ProductVersionAttribute that = (ProductVersionAttribute) o;
        return isMandatory() == that.isMandatory() && isDisplay() == that.isDisplay() && Objects.equals(getProductVersion(), that.getProductVersion()) && Objects.equals(getSequence(), that.getSequence())
                && Objects.equals(getAttribute(), that.getAttribute()) && Objects.equals(getMandatoryWithEl(), that.getMandatoryWithEl()) && Objects.equals(getReadOnly(), that.getReadOnly())
                && Objects.equals(getDefaultValue(), that.getDefaultValue()) && getValidationType() == that.getValidationType() && Objects.equals(getValidationPattern(), that.getValidationPattern())
                && Objects.equals(getValidationLabel(), that.getValidationLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getProductVersion(), getSequence(), getAttribute(), getMandatoryWithEl(), isMandatory(), isDisplay(), getReadOnly(), getDefaultValue(), getValidationType(),
            getValidationPattern(), getValidationLabel());
    }
}
