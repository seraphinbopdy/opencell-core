package org.meveo.api.dto.payment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.meveo.apiv2.generic.GenericFieldDetails;
import org.meveo.apiv2.payments.AccountOperationsDetails;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableCustomerBalanceExportDto.class)
public interface CustomerBalanceExportDto extends AccountOperationsDetails {
    
    @Nullable
    @Value.Default default List<GenericFieldDetails> getGenericFieldDetails(){ return Collections.emptyList();}
}
