package org.meveo.apiv2.finance;

import java.util.List;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.meveo.apiv2.models.Resource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Immutable
@Value.Style(jdkOnly = true, jakarta = true)
public interface TrialBalancesResult extends Resource {

    Long getTotal();

    Long getLimit();

    Long getOffset();

    @Nullable
    @Schema(description = "The trial balance report period")
    String getPeriod();

    @Nullable
    @Schema(description = "Trial balances")
    List<TrialBalance> getBalances();
}