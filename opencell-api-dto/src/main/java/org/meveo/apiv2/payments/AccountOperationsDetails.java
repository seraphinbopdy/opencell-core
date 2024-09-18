package org.meveo.apiv2.payments;

import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.api.dto.CurrencyDto;
import org.meveo.api.dto.account.CustomerAccountDto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(builder = ImmutableAccountOperationsDetails.Builder.class)
public interface AccountOperationsDetails {
    @Nullable
    CustomerAccountDto customerAccount();
    @Nullable
    CustomerBalance customerBalance();
    @Nullable
    CurrencyDto transactionalCurrency();
    @Nullable
    List<String> excludeAOs();
}
