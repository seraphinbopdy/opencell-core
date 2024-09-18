package org.meveo.apiv2.dunning;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningPolicyRule.class)
public interface DunningPolicyRule extends Resource {

    @Schema(description = "Rule joint")
    @Nullable
    String getRuleJoint();

    @Schema(description = "Dunning policy")
    @Nullable
    Resource getDunningPolicy();
}