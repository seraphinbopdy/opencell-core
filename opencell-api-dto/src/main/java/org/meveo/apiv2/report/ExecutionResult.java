package org.meveo.apiv2.report;

import java.util.List;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableReportQuery.class)
public interface ExecutionResult extends Resource {

    @Schema(description = "Report query execution results")
    List<Object> getExecutionResults();

    @Schema(description = "Execution results count")
    long getTotal();
}
