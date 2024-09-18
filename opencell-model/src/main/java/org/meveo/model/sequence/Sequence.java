package org.meveo.model.sequence;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessEntity;
import org.meveo.model.ExportIdentifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@ExportIdentifier({ "code" })
@Table(name = "generic_sequence")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @org.hibernate.annotations.Parameter(name = "sequence_name", value = "bill_seq_invoice_seq"), @Parameter(name = "increment_size", value = "1") })
@Inheritance(strategy = TABLE_PER_CLASS)
public class Sequence extends BusinessEntity {

    private static final long serialVersionUID = -1964277428044516329L;

    /**
     * SEQUENCE "Sequence" (a classical sequence from 0 to infinity), NUMERIC "Random number", ALPHA_UP "Random string uppercase", UUID "Universal Unique IDentifier", REGEXP "Regular expression" (random string from a
     * regexp)
     */
    @Column(name = "sequence_type")
    @Enumerated(STRING)
    private SequenceTypeEnum sequenceType;

    /**
     * Size of the generated number (padded with zeros for SEQUENCE and NUMERIC)
     */
    @Column(name = "sequence_size")
    private Integer sequenceSize;

    /**
     * Current value of the sequence. This field is read only and lock read. Updated only when a next sequence value is requested. used only if sequenceType=SEQUENCE
     */
    @Column(name = "current_number")
    private Long currentNumber = 0L;

    /**
     * Generate random string from a Regular expression used only if sequenceType=REGEXP
     */
    @Column(name = "sequence_pattern", length = 2000)
    @Size(max = 2000)
    private String sequencePattern;

    public Sequence() {
    }

    public Sequence(SequenceTypeEnum sequenceType, Integer sequenceSize, Long currentNumber, String sequencePattern) {
        this.sequenceType = sequenceType;
        this.sequenceSize = sequenceSize;
        this.currentNumber = currentNumber;
        this.sequencePattern = sequencePattern;
    }

    public Sequence(Integer sequenceSize, Long currentNumber) {
        this.sequenceSize = sequenceSize;
        this.currentNumber = currentNumber;
    }

    public Integer getSequenceSize() {
        return sequenceSize;
    }

    public void setSequenceSize(Integer sequenceSize) {
        this.sequenceSize = sequenceSize;
    }

    public SequenceTypeEnum getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(SequenceTypeEnum sequenceType) {
        this.sequenceType = sequenceType;
    }

    public Long getCurrentNumber() {
        return currentNumber;
    }

    public void setCurrentNumber(Long currentNumber) {
        this.currentNumber = currentNumber;
    }

    public String getSequencePattern() {
        return sequencePattern;
    }

    public void setSequencePattern(String sequencePattern) {
        this.sequencePattern = sequencePattern;
    }
}