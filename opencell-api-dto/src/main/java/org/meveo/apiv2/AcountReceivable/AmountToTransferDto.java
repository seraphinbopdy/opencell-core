package org.meveo.apiv2.AcountReceivable;

import java.math.BigDecimal;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAmountToTransferDto.class)
public interface AmountToTransferDto {
	
	@Schema(description = "Customer account to transfer amount to")
	CustomerAccount getCustomerAccount();
	@Schema(description = "Amount to transfer")
	BigDecimal getAmount();
}
