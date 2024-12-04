package org.meveo.apiv2.accounting;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAuxiliaryAccount.class)
public interface AuxiliaryAccount extends Resource {

    @Schema(description = "Customer account")
    @Nullable
    Resource getCustomerAccount();

    @Schema(description = "Auxiliary account code")
    @Nullable
    String getAuxiliaryAccountCode();

    @Schema(description = "Auxiliary account label")
    @Nullable
    String getAuxiliaryAccountLabel();
}