package org.meveo.apiv2.payments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import javax.annotation.Nullable;
import java.util.Map;

@Value.Immutable
@Value.Style(jdkOnly = true)
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
