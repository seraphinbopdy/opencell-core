package org.meveo.api.dto.payment;

import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.generic.GenericFieldDetails;
import org.meveo.apiv2.payments.AccountOperationsDetails;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableCustomerBalanceExportDto.class)
public interface CustomerBalanceExportDto extends AccountOperationsDetails {
    
    @Nullable
    @Value.Default default List<GenericFieldDetails> getGenericFieldDetails(){ return Collections.emptyList();}
}
