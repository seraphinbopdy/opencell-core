package org.meveo.apiv2.payments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.apiv2.models.Resource;

import javax.annotation.Nullable;

import static java.lang.Boolean.FALSE;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableRejectionCodeDeleteInput.class)
public interface RejectionCodeDeleteInput extends Resource {

    @Nullable
    @Value.Default
    default Boolean getForce() {
        return FALSE;
    }

    @Nullable
    PagingAndFiltering getFilters();
}
