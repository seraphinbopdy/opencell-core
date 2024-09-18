package org.meveo.apiv2.dunning;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableMassSwitchResult.class)
public interface MassSwitchResult {

    @Schema(description = "Collection plan list total")
    @Nullable
    Long getTotal();

    @Schema(description = "Collection plan list eligible for switch")
    @Nullable
    CheckSwitchResult getCanBeSwitched();

    @Schema(description = "Collection plan list not eligible for switch")
    @Nullable
    CheckSwitchResult getCanNotBeSwitched();
}