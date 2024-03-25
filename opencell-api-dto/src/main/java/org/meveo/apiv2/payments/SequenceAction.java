package org.meveo.apiv2.payments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableSequenceAction.class)
public interface SequenceAction extends Resource {

    @Nullable
    @Schema(description = "Payment rejection action action")
    SequenceActionType getSequenceAction();
}
