package org.meveo.apiv2.billing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableCancellationOutput.class)
public interface CancellationOutput extends Resource {
    @Schema(description = "Quarantine output statistics")
    Statistic getStatistics();

    @Schema(description = "List of cancelled invoice ids")
    List<Long> getInvoicesCancelled();

    @Schema(description = "List of invoices that could not be cancelled")
    List<Failure> getInvoicesNotCancelled();
}