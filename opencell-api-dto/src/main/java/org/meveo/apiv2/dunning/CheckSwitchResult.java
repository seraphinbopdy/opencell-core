package org.meveo.apiv2.dunning;

import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableCheckSwitchResult.class)
public interface CheckSwitchResult {

    @Schema(description = "Collection plan total")
    @Nullable
    Long getTotal();

    @Schema(description = "Collection plan list id")
    @Nullable
    List<Resource> getCollectionPlans();
}