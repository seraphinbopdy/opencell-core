package org.meveo.apiv2.payments;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableRejectionCodeClearInput.class)
public interface RejectionCodeClearInput {

    @Nullable
    Resource getPaymentGateway();

    @Nullable
    @Value.Default
    default Boolean getForce() {
        return Boolean.FALSE;
    }
}
