package org.meveo.apiv2.dunning;

import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningMassSwitchInput.class)
public interface DunningMassSwitchInput {

    @Schema(description = "Collection plan list to check")
    @Nullable
    List<Resource> getCollectionPlans();

    @Schema(description = "Dunning policy to use for check")
    @Nullable
    Resource getPolicy();
}