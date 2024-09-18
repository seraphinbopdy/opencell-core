package org.meveo.apiv2.payments;

import static java.lang.Boolean.FALSE;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableRejectionCodeDeleteInput.class)
public interface RejectionCodeDeleteInput extends Resource {

    @Nullable
    @Value.Default
    default Boolean getForce() {
        return FALSE;
    }

    @Nullable
    @JsonUnwrapped
    PagingAndFiltering getFilters();
}
