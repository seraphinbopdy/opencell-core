package org.meveo.apiv2.export;

import java.util.Map;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(builder = ImmutableExportResponse.Builder.class)
public interface ExportResponse {

    String getExecutionId();

    @Nullable
    Map<String, Integer> getSummary();

    @Nullable
    Map<String, String> getFieldsNotImported();

    @Nullable
    String getExceptionMessage();

    @Nullable
    String getErrorMessageKey();

    @Nullable
    byte[] getFileContent();
}
