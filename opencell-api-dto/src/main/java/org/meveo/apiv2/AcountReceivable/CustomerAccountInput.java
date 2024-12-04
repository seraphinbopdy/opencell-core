package org.meveo.apiv2.AcountReceivable;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableCustomerAccountInput.class)
public interface CustomerAccountInput {

    @Schema(description = "Customer account")
    @Nullable
    CustomerAccount getCustomerAccount();
}