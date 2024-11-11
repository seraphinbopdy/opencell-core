package org.meveo.apiv2.dunning;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.apiv2.models.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableDunningPauseReason.class)
public interface DunningPauseReason extends Resource {

    @Schema(description = "The pause reason")
    @Nonnull
    String getPauseReason();

    @Schema(description = "The pause reason's description")
    @Nullable
    String getDescription();

    @Nullable
    @Schema(description = "The pause reason's description in different languages")
    List<LanguageDescriptionDto> getDescriptionI18n();

    @Schema(description = "The dunning settings associated to pause reason")
    @Nonnull
	Resource getDunningSettings();
}
