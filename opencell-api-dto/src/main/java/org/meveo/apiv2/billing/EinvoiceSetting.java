package org.meveo.apiv2.billing;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.billing.CustomizationIDEnum;
import org.meveo.model.billing.VatDateCodeEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableEinvoiceSetting.class)
public interface EinvoiceSetting extends Resource {
	
	@Nullable
	@Schema(description = "forcing xml generation job to be changing after invoicing job")
	Boolean getForceXmlGeneration();
	
	@Nullable
	@Schema(description = "forcing pdf generation job to be changing after xml generation job")
	Boolean getForcePDFGeneration();
	
	@Nullable
	@Schema(description = "forcing pdf generation ubl to be changing after pdf generation job")
	Boolean getForceUBLGeneration();
	
	@Nullable
	@Schema(description = "the code of invoicing job")
	String getInvoicingJob();
	
	@Nullable
	@Schema(description = "the code of pdf generation job")
	String getPdfGenerationJob();
	
	@Nullable
	@Schema(description = "the code of ubl generation job")
	String getUblGenerationJob();
	
	@Nullable
	@Schema(description = "the code of xml generation job")
	String getXmlGenerationJob();
	
	@Nullable
	@Schema(description = "the vat date code")
	VatDateCodeEnum getVatDateCode();

	@Nullable
	@Schema(description = "customization ID")
	String getCustomizationID();
	
}
