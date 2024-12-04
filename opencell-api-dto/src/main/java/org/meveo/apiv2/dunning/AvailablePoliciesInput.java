package org.meveo.apiv2.dunning;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAvailablePoliciesInput.class)
public interface AvailablePoliciesInput {

    @Schema(description = "Billing account resource")
    @Nullable
    Resource getBillingAccount();

    @Schema(description = "Collection plan resource")
    @Nullable
    Resource getCollectionPlan();
}