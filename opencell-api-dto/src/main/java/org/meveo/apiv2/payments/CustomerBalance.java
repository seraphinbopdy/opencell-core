package org.meveo.apiv2.payments;

import static java.lang.Boolean.FALSE;

import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(builder = ImmutableCustomerBalance.Builder.class)
public interface CustomerBalance extends Resource {

    @Nullable
    String getCode();

    @Nullable
    String getLabel();

    @Nullable
    @Value.Default
    default Boolean getDefaultBalance() {
        return FALSE;
    }

    @Nullable
    String getBalanceEL();

    @Nullable
    List<Resource> getOccTemplates();
}