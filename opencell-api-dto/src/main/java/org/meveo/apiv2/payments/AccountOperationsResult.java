package org.meveo.apiv2.payments;

import java.math.BigDecimal;
import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(builder = ImmutableAccountOperationsResult.Builder.class)
public interface AccountOperationsResult {
    @Nullable
    BigDecimal totalCredit();
    @Nullable
    BigDecimal totalDebit();
    @Nullable
    BigDecimal balance();
    @Nullable
    List<Long> accountOperationIds();
}
