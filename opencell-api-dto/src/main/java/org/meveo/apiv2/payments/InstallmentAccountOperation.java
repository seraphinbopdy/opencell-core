package org.meveo.apiv2.payments;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableInstallmentAccountOperation.class)
public interface InstallmentAccountOperation {

    @Schema(description = "AccountOperation Id")
    @NotNull
    Long getId();
}