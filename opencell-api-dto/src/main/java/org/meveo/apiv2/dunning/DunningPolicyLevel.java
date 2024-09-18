package org.meveo.apiv2.dunning;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningPolicyLevel.class)
public interface DunningPolicyLevel extends Resource {

    @Schema(description = "Dunning level id")
    @Nullable
    Long getDunningLevelId();

    @Schema(description = "Collection plan status Id")
    @Nullable
    Long getCollectionPlanStatusId();
}