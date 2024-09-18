package org.meveo.apiv2.dunning;

import java.util.List;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningPolicyRules.class)
public interface DunningPolicyRules extends Resource {

    @Schema(description = "List of policy rules")
    List<PolicyRule> getPolicyRules();
}