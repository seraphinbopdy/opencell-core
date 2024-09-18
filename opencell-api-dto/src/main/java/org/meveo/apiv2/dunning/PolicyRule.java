package org.meveo.apiv2.dunning;

import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.dunning.PolicyConditionRuleJointEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutablePolicyRule.class)
public interface PolicyRule extends Resource {

    @Schema(description = "Policy rule's rule join")
    @Nullable
    PolicyConditionRuleJointEnum getRuleJoint();

    @Schema(description = "List of policy rule lines")
    @Nullable
    List<DunningPolicyRuleLine> getRuleLines();
}