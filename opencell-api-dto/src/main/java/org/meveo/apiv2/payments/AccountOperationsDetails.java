package org.meveo.apiv2.payments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.meveo.api.dto.CurrencyDto;
import org.meveo.api.dto.account.CustomerAccountDto;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
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
