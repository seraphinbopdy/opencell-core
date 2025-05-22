package org.meveo.apiv2.billing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableStatistic.class)
public interface Statistic extends Resource {

    @Schema(description = "Total records processed by the API")
    Long getTotal();

    @Schema(description = "Success count")
    Long getSuccess();

    @Schema(description = "Failures count")
    Long getFail();

    @Schema(description = "API limit record to process")
    Long getLimit();
}