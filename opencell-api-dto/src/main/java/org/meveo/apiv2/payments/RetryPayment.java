package org.meveo.apiv2.payments;

import java.util.Date;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableRetryPayment.class)
public interface RetryPayment extends Resource {

    @Nullable
    @Schema(description = "Retry payment collection date")
    Date getCollectionDate();
}
