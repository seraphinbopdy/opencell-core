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
@JsonDeserialize(as = ImmutableDunningStopReason.class)
public interface DunningStopReason extends Resource {

    @Schema(description = "The stop reason")
    @Nonnull
    String getStopReason();

    @Schema(description = "The stop reason's description")
    @Nullable
    String getDescription();

    @Nullable
    @Schema(description = "The pause reason's description in different languages")
    List<LanguageDescriptionDto> getDescriptionI18n();

    @Schema(description = "The dunning settings associated to stop reason")
    @Nonnull
	Resource getDunningSettings();
}
