package org.meveo.apiv2.billing;

import java.util.Collections;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableWalletOperationRerate.class)
public interface WalletOperationRerate extends Resource {

    @Nullable
    @Value.Default
    default Map<String, Object> getFilters() {
        return Collections.emptyMap();
    }

}
