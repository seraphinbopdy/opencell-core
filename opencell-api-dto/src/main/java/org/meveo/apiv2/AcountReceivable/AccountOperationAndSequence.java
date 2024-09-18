package org.meveo.apiv2.AcountReceivable;

import java.math.BigDecimal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAccountOperationAndSequence.class)
public interface AccountOperationAndSequence {

    @Schema(description = "Matching sequence")
    @Nonnull
    Integer getSequence();

    @Schema(description = "AccountOperation Id")
    @Nonnull
    Long getId();
    

    @Schema(description = "amount to match")
    @Nullable
    BigDecimal getAmountToMatch();
}