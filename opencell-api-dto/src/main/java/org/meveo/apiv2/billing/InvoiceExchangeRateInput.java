package org.meveo.apiv2.billing;

import java.math.BigDecimal;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableInvoiceExchangeRateInput.class)
public interface InvoiceExchangeRateInput extends Resource {

	@Schema(description = "exchange rate")
    @NotNull
    BigDecimal getExchangeRate();

}