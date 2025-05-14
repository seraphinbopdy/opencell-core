package org.meveo.apiv2.billing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableQuarantineOutput.class)
public interface QuarantineOutput extends Resource {

	@Schema(description = "Quarantine output statistics")
	Statistic getStatistics();

	@Schema(description = "List of quarantined invoice ids")
	List<Long> getInvoicesQuarantined();

	@Schema(description = "List of invoices that could not be quarantined")
	List<Failure> getInvoicesNotQuarantined();
}