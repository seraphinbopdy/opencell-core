package org.meveo.apiv2.payments;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableRejectionCodesExportResult.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface RejectionCodesExportResult {

    @Nullable
    @Schema(description = "Export file full path")
    String getFileFullPath();

    @Nullable
    @Schema(description = "Exported rejection codes count")
    Integer getExportSize();

    @Nullable
    @Schema(description = "Exported file encoded")
    String getEncodedFile();
}
