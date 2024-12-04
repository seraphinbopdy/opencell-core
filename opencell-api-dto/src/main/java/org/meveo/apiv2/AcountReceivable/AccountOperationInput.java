package org.meveo.apiv2.AcountReceivable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAccountOperationInput.class)
public interface AccountOperationInput {

    @Schema(description = "AccountOperation Id")
    @NotNull
    Long getId();

}