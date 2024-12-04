package org.meveo.apiv2.billing;

import java.math.BigDecimal;

import jakarta.annotation.Nullable;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableGenerateInvoiceResult.class)
public interface GenerateInvoiceResult extends Invoice, Resource {

    @Schema(description = "Temporary InvoiceNumber")
    String getTemporaryInvoiceNumber();

    @Schema(description = "Invoice type code")
    String getInvoiceTypeCode();

    @Schema(description = "Amount")
    BigDecimal getAmount();

    @Nullable
    @Schema(description = "Account operation ID")
    Long getAccountOperationId();
}