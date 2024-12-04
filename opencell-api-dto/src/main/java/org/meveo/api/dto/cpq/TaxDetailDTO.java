package org.meveo.api.dto.cpq;

import java.math.BigDecimal;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

@XmlAccessorType(XmlAccessType.FIELD)
public class TaxDetailDTO {

    private BigDecimal totalAmountWithoutTax = BigDecimal.ZERO;
    private BigDecimal totalAmountWithTax = BigDecimal.ZERO;
    private BigDecimal totalAmountTax = BigDecimal.ZERO;
    private String vatex;
    @XmlElementWrapper(name = "taxes")
    @XmlElement(name = "tax")
    private List<TaxDTO> taxes;

    public List<TaxDTO> getTaxes() {
        return taxes;
    }

    public void setTaxes(List<TaxDTO> taxes) {
        this.taxes = taxes;
    }

    public String getVatex() {
        return vatex;
    }

    public void setVatex(String vatex) {
        this.vatex = vatex;
    }

    public BigDecimal getTotalAmountWithoutTax() {
        return totalAmountWithoutTax;
    }

    public void setTotalAmountWithoutTax(BigDecimal totalAmountWithoutTax) {
        this.totalAmountWithoutTax = totalAmountWithoutTax;
    }

    public BigDecimal getTotalAmountWithTax() {
        return totalAmountWithTax;
    }

    public void setTotalAmountWithTax(BigDecimal totalAmountWithTax) {
        this.totalAmountWithTax = totalAmountWithTax;
    }

    public BigDecimal getTotalAmountTax() {
        return totalAmountTax;
    }

    public void setTotalAmountTax(BigDecimal totalAmountTax) {
        this.totalAmountTax = totalAmountTax;
    }
}
