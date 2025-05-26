package org.meveo.apiv2.billing;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableInvoicePatchInput.class)
public interface InvoicePatchInput extends Resource {

	@Nullable
    @Schema(description = "The custom fields associated to the invoice")
    CustomFieldsDto getCustomFields();

    @Schema(description = "The comment for the invoice")
    @Nullable
    String getComment();

    @Schema(description = "The list of purchase order numbers")
    @javax.annotation.Nullable
    List<String> getPurchaseOrders();
}