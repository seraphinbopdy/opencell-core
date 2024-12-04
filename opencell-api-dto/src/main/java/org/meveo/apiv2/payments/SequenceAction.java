package org.meveo.apiv2.payments;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSequenceAction.class)
public interface SequenceAction extends Resource {

    @Nullable
    @Schema(description = "Payment rejection action action")
    SequenceActionType getSequenceAction();
}
