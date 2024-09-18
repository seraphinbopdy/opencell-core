package org.meveo.apiv2.dunning;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.dunning.PayRetryFrequencyUnitEnum;
import org.meveo.model.payments.PaymentMethodEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningPaymentRetry.class)
public interface DunningPaymentRetry extends Resource {

    @Schema(description = "The payment method")
    @Nonnull
    PaymentMethodEnum getPaymentMethod();

    @Schema(description = "The payment service provider")
    @Nullable
    String getPsp();

    @Schema(description = "The number of payment retries")
    @Nonnull
    Integer getNumPayRetries();

    @Schema(description = "The unit's frequency of retry")
    @Nonnull
    PayRetryFrequencyUnitEnum getPayRetryFrequencyUnit();

    @Schema(description = "The retry's frequency")
    @Nonnull
    Integer getPayRetryFrequency();

    @Schema(description = "The dunning settings associated to stop reason")
    @Nonnull
	Resource getDunningSettings();
}
