package org.meveo.apiv2.dunning;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.payments.DunningCollectionPlanStatusEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningCollectionPlanStatus.class)
public interface DunningCollectionPlanStatus extends Resource {

    @Schema(description = "dunning setting id")
    @NotNull
    Resource getDunningSettings();

    @Schema(description = "indicate the status used in the collection")
    @NotNull
    DunningCollectionPlanStatusEnum getStatus();

    @Schema(description = "indicate description for the collection")
    @Nullable
    String getDescription();

    @Schema(description = "indicate color code for the status")
    String getColorCode();
}
