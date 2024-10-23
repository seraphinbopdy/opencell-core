package org.meveo.model.billing;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.meveo.model.AuditableEntity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "electronic_invoice_settings")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
		@Parameter(name = "sequence_name", value = "electronic_invoice_settings_seq"), })
public class ElectronicInvoiceSetting  extends AuditableEntity {
	
	@Type(type = "numeric_boolean")
	@Column(name = "force_xml_generation")
	private boolean forceXmlGeneration = false;
	
	@Type(type = "numeric_boolean")
	@Column(name = "force_pdf_generation")
	private boolean forcePDFGeneration = false;
	
	@Type(type = "numeric_boolean")
	@Column(name = "force_ubl_generation")
	private boolean forceUBLGeneration = false;
	
	@Column(name = "invoicing_job")
	private String invoicingJob;
	
	@Column(name = "pdf_generation_job")
	private String pdfGenerationJob;
	
	@Column(name = "ubl_generation_job")
	private String ublGenerationJob;
	
	@Column(name = "xml_generation_job")
	private String xmlGenerationJob;
	
	@Column(name = "vat_date_code")
	@Convert(converter = VatDateCodeEnumConverter.class)
	private VatDateCodeEnum vatDateCode = VatDateCodeEnum.PAID_TO_DATE;

	@Column(name = "customization_id")
	//@Convert(converter = CustomizationIdConverter.class)
	private String customizationID = CustomizationIDEnum.URN_CEN_EU.getValue();
	
	
	public boolean isForceXmlGeneration() {
		return forceXmlGeneration;
	}
	
	public void setForceXmlGeneration(boolean forceXmlGeneration) {
		this.forceXmlGeneration = forceXmlGeneration;
	}
	
	public boolean isForcePDFGeneration() {
		return forcePDFGeneration;
	}
	
	public void setForcePDFGeneration(boolean forcePDFGeneration) {
		this.forcePDFGeneration = forcePDFGeneration;
	}
	
	public boolean isForceUBLGeneration() {
		return forceUBLGeneration;
	}
	
	public void setForceUBLGeneration(boolean forceUBLGeneration) {
		this.forceUBLGeneration = forceUBLGeneration;
	}
	
	public String getInvoicingJob() {
		return invoicingJob;
	}
	
	public void setInvoicingJob(String invoicingJob) {
		this.invoicingJob = invoicingJob;
	}
	
	public String getPdfGenerationJob() {
		return pdfGenerationJob;
	}
	
	public void setPdfGenerationJob(String pdfGenerationJob) {
		this.pdfGenerationJob = pdfGenerationJob;
	}
	
	public String getUblGenerationJob() {
		return ublGenerationJob;
	}
	
	public void setUblGenerationJob(String ublGenerationJob) {
		this.ublGenerationJob = ublGenerationJob;
	}
	
	public String getXmlGenerationJob() {
		return xmlGenerationJob;
	}
	
	public void setXmlGenerationJob(String xmlGenerationJob) {
		this.xmlGenerationJob = xmlGenerationJob;
	}
	
	public VatDateCodeEnum getVatDateCode() {
		return vatDateCode;
	}
	
	public void setVatDateCode(VatDateCodeEnum vatDateCode) {
		this.vatDateCode = vatDateCode;
	}

	public String getCustomizationID() {
		return customizationID;
	}

	public void setCustomizationID(String customizationID) {
		this.customizationID = customizationID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		ElectronicInvoiceSetting that = (ElectronicInvoiceSetting) o;
		return forceXmlGeneration == that.forceXmlGeneration && forcePDFGeneration == that.forcePDFGeneration && forceUBLGeneration == that.forceUBLGeneration && Objects.equals(invoicingJob, that.invoicingJob) && Objects.equals(pdfGenerationJob, that.pdfGenerationJob) && Objects.equals(ublGenerationJob, that.ublGenerationJob) && Objects.equals(xmlGenerationJob, that.xmlGenerationJob) && vatDateCode == that.vatDateCode && customizationID == that.customizationID;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), forceXmlGeneration, forcePDFGeneration, forceUBLGeneration, invoicingJob, pdfGenerationJob, ublGenerationJob, xmlGenerationJob, vatDateCode, customizationID);
	}
}
