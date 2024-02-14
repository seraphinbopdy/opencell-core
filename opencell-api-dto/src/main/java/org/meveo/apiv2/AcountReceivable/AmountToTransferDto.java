package org.meveo.apiv2.AcountReceivable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.api.dto.account.CustomerAccountDto;

import java.math.BigDecimal;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(builder = ImmutableAmountToTransferDto.class)
public interface AmountToTransferDto {
	
	@Schema(description = "Customer account to transfer amount to")
	CustomerAccount getCustomerAccount();
	@Schema(description = "Amount to transfer")
	BigDecimal getAmount();
}
