package org.meveo.apiv2.payments;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableClearingResponse.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface ClearingResponse {

    String getStatus();

    String getMassage();

    @Nullable
    String getAssociatedPaymentGatewayCode();

    Integer getClearedCodesCount();
}
