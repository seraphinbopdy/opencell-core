package org.meveo.apiv2.dunning;

import java.util.List;

import org.immutables.value.Value;
import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningPauseReason.class)
public interface DunningPauseReason extends Resource {

    @Schema(description = "The pause reason")
    @NotNull
    String getPauseReason();

    @Schema(description = "The pause reason's description")
    @Nullable
    String getDescription();

    @Nullable
    @Schema(description = "The pause reason's description in different languages")
    List<LanguageDescriptionDto> getDescriptionI18n();

    @Schema(description = "The dunning settings associated to pause reason")
    @NotNull
	Resource getDunningSettings();
}
