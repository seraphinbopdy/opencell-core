package org.meveo.apiv2.accounts;

import java.util.Set;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.billing.CounterPeriodDto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableCounterInstanceDto.class)
public interface CounterInstanceDto {

    @Nullable
    String getCounterTemplateCode();

    @Nullable
    String getCustomerAccountCode();

    @Nullable
    String getBillingAccountCode();

    @Nullable
    String getUserAccountCode();

    @Nullable
    String getSubscriptionCode();

    @Nullable
    String getProductCode();

    @Nullable
    String getChargeInstanceCode();

    @Schema(description = "Counter Periods")
    @Nullable
    Set<CounterPeriodDto> getCounterPeriods();

}