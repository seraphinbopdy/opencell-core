package org.meveo.apiv2.finance;

import java.math.BigDecimal;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableTrialBalance.class)
public interface TrialBalance extends Resource {

    @Schema(description = "Accounting code")
    String getAccountingCode();

    @Nullable
    @Schema(description = "Accounting label")
    String getAccountingLabel();

    @Nullable
    @Schema(description = "Initial balance")
    BigDecimal getInitialBalance();

    @Nullable
    @Schema(description = "Current credit balance")
    BigDecimal getCurrentCreditBalance();

    @Nullable
    @Schema(description = "Current debit balance")
    BigDecimal getCurrentDebitBalance();

    @Nullable
    @Schema(description = "Closing balance")
    BigDecimal getClosingBalance();
}