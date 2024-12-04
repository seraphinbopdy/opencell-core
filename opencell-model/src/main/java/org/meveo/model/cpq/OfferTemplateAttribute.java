package org.meveo.model.cpq;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.catalog.OfferTemplate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "offer_template_attribute")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "offer_template_attribute_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "OfferTemplateAttribute.findByAttributeAndOfferTemplate", query = "select ota FROM OfferTemplateAttribute ota where ota.attribute.id =:attributeId and ota.offerTemplate.id =:offerTemplateId") })
public class OfferTemplateAttribute extends AttributeBaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -5934892816847168643L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_template_id", nullable = false)
    @NotNull
    private OfferTemplate offerTemplate;

    public OfferTemplateAttribute() {
        super();
    }

    /**
     * @return the offerTemplate
     */
    public OfferTemplate getOfferTemplate() {
        return offerTemplate;
    }

    /**
     * @param offerTemplate the offerTemplate to set
     */
    public void setOfferTemplate(OfferTemplate offerTemplate) {
        this.offerTemplate = offerTemplate;
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

}
