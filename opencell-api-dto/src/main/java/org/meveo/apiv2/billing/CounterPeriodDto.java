package org.meveo.apiv2.billing;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.model.catalog.CounterTypeEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableCounterPeriodDto.class)
public interface CounterPeriodDto {

    @Nullable
    String getCode();

    @Nullable
    CounterTypeEnum getCounterType();

    @Nullable
    BigDecimal getLevel();

    @Nullable
    Date getStartDate();

    @Nullable
    Date getEndDate();

    @Nullable
    BigDecimal getValue();

    @Nullable
    Map<String, BigDecimal> getAccumulatedValues();


}