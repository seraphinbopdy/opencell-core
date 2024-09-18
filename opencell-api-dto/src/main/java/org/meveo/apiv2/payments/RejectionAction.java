package org.meveo.apiv2.payments;

import java.util.Map;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableRejectionAction.class)
public interface RejectionAction extends Resource {

    @Nullable
    @Schema(description = "Payment rejection action description")
    String getDescription();

    @Nullable
    @Schema(description = "Payment rejection action sequence")
    Integer getSequence();

    @Nullable
    @Schema(description = "Payment rejection action associated script")
    Resource getScriptInstance();

    @Nullable
    @Schema(description = "Payment rejection action script parameters")
    Map<String,String> getScriptParameters();

    @Nullable
    @Schema(description = "Payment rejection code group")
    Resource getRejectionCodeGroup();
}
