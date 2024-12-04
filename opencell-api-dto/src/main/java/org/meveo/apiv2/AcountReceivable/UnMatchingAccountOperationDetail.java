package org.meveo.apiv2.AcountReceivable;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableUnMatchingAccountOperationDetail.class)
public interface UnMatchingAccountOperationDetail {

    @Schema(description = "AccountOperation Id")
    @NotNull
    Long getId();

    @Schema(description = "MatchingAmount Id")
    List<Long> getMatchingAmountIds();
}