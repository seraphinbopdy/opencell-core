package org.meveo.apiv2.payments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.meveo.api.dto.payment.AccountOperationDto;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(builder = ImmutableAccountOperationsResult.Builder.class)
public interface AccountOperationsResult {
    @Nullable
    BigDecimal totalCredit();
    @Nullable
    BigDecimal totalDebit();
    @Nullable
    BigDecimal balance();
    @Nullable
    List<AccountOperationDto> accountOperations();
}
