package org.meveo.apiv2.payments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableRejectionGroup.class)
public interface RejectionGroup extends Resource {

    @Nullable
    @Schema(description = "Payment rejection action description")
    String getDescription();

    @Nullable
    @Schema(description = "Payment gateway")
    Resource getPaymentGateway();

    @Nullable
    @Schema(description = "Associated payment rejection codes")
    List<Resource> getRejectionCodes();

    @Nullable
    @Schema(description = "Associated payment rejection actions")
    List<Resource> getRejectionActions();
}
