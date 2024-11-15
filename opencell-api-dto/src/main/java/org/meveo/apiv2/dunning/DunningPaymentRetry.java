package org.meveo.apiv2.dunning;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.dunning.PayRetryFrequencyUnitEnum;
import org.meveo.model.payments.PaymentMethodEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunningPaymentRetry.class)
public interface DunningPaymentRetry extends Resource {

    @Schema(description = "The payment method")
    @NotNull
    PaymentMethodEnum getPaymentMethod();

    @Schema(description = "The payment service provider")
    @Nullable
    String getPsp();

    @Schema(description = "The number of payment retries")
    @NotNull
    Integer getNumPayRetries();

    @Schema(description = "The unit's frequency of retry")
    @NotNull
    PayRetryFrequencyUnitEnum getPayRetryFrequencyUnit();

    @Schema(description = "The retry's frequency")
    @NotNull
    Integer getPayRetryFrequency();

    @Schema(description = "The dunning settings associated to stop reason")
    @NotNull
	Resource getDunningSettings();
}
