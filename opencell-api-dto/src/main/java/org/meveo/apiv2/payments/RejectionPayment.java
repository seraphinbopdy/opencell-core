package org.meveo.apiv2.payments;

import static java.lang.Boolean.FALSE;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.payments.RejectedPayment;

import javax.annotation.Nullable;
import java.util.Date;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableRejectionPayment.class)
public interface RejectionPayment extends Resource {

    @Nullable
    @Schema(description = "Payment rejection external id")
    String getExternalPaymentId();

    @Nullable
    @Schema(description = "Payment rejection gateway code")
    String getPaymentGatewayCode();

    @Nullable
    @Schema(description = "Rejection payment date")
    Date getRejectionDate();

    @Nullable
    @Schema(description = "Rejection payment code")
    String getRejectionCode();

    @Nullable
    @Schema(description = "Rejection Payment comment")
    String getComment();

    @Default
    @Schema(description = "Skip rejection actions creation")
    default Boolean getSkipRejectionActions() {
        return FALSE;
    }

    static RejectionPayment from(RejectedPayment rejectedPayment) {
        ImmutableRejectionPayment.Builder builder = ImmutableRejectionPayment.builder()
                .id(rejectedPayment.getId())
                .code(rejectedPayment.getRejectedCode())
                .comment(rejectedPayment.getComment())
                .externalPaymentId(rejectedPayment.getBankReference())
                .rejectionDate(rejectedPayment.getRejectedDate());
        ofNullable(rejectedPayment.getCode()).ifPresent(builder::code);
        return builder.build();
    }
}
