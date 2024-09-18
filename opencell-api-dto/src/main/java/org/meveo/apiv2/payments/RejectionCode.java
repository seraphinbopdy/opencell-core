package org.meveo.apiv2.payments;

import java.util.Map;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableRejectionCode.class)
public interface RejectionCode extends Resource {

    @Nullable
    String getDescription();

    @Nullable
    Map<String, String> getDescriptionI18n();

    @Nullable
    Resource getPaymentGateway();
}
