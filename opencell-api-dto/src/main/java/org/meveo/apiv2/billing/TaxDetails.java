package org.meveo.apiv2.billing;

import java.math.BigDecimal;
import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableTaxDetails.class)
public interface TaxDetails extends Resource {

    @Schema(description = "Main tax")
    Tax getTax();

    @Schema(description = "Tax amount")
    BigDecimal getTaxAmount();

    @Schema(description = "Converted tax amount")
    @Nullable
    BigDecimal getConvertedTaxAmount();

    @Schema(description = "Sub taxes")
    @Nullable
    List<TaxDetails> getSubTaxes();
}