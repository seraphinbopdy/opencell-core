package org.meveo.apiv2.AcountReceivable;

import java.math.BigDecimal;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAccountOperationAndSequence.class)
public interface AccountOperationAndSequence {

    @Schema(description = "Matching sequence")
    @NotNull
    Integer getSequence();

    @Schema(description = "AccountOperation Id")
    @NotNull
    Long getId();
    

    @Schema(description = "amount to match")
    @NotNull
    BigDecimal getAmountToMatch();
}