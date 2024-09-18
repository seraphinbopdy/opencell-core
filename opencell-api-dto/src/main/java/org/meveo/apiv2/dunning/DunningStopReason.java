package org.meveo.apiv2.dunning;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningStopReason.class)
public interface DunningStopReason extends Resource {

    @Schema(description = "The stop reason")
    @Nonnull
    String getStopReason();

    @Schema(description = "The stop reason's description")
    @Nullable
    String getDescription();

    @Schema(description = "The dunning settings associated to stop reason")
    @Nonnull
	Resource getDunningSettings();
}
