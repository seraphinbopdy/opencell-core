package org.meveo.apiv2.billing;

import static java.lang.Boolean.FALSE;
import static org.meveo.apiv2.billing.TransactionMode.PROCESS_ALL;
import static org.meveo.model.billing.RatedTransactionAction.REOPEN;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.billing.RatedTransactionAction;

import javax.annotation.Nullable;
import java.util.Map;

@Immutable
@Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableInvoiceCancellationInput.class)
public interface InvoiceCancellationInput extends Resource {

    @Nullable
    @Schema(description = "Invoice cancellation filter")
    Map<String, Object> getFilters();

    @Nullable
    @Value.Default
    @Schema(description = "Fails on validated invoice")
    default Boolean getFailOnValidatedInvoice() {
        return FALSE;
    }

    @Nullable
    @Value.Default
    @Schema(description = "Fails on cancelled invoice")
    default Boolean getFailOnCanceledInvoice() {
        return FALSE;
    }

    @Nullable
    @Value.Default
    @Schema(description = "Transaction mode")
    default TransactionMode getMode() {
        return PROCESS_ALL;
    }

    @Nullable
    @Value.Default
    @Schema(description = "Rated transaction action to perform")
    default RatedTransactionAction getRatedTransactionAction() {
        return REOPEN;
    }
}
