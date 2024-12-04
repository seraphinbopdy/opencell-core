package org.meveo.apiv2.AcountReceivable;

import java.util.Date;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(builder = ImmutableDeferralPayments.Builder.class)
public interface DeferralPayments {
    @Nullable
    Long getAccountOperationId();

    @Nullable
    String getPaymentMethod();

    @Nullable
    Date getPaymentDate();
}
