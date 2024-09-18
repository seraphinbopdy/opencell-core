package org.meveo.apiv2.dunning;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAvailablePoliciesList.class)
public interface AvailablePoliciesList {

    @Schema(description = "Available policies")
    List<DunningPolicy> getAvailablePolicies();

    @Schema(description = "Total available policies")
    Integer getTotal();
}