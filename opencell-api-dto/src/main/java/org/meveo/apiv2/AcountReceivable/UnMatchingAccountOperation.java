package org.meveo.apiv2.AcountReceivable;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableUnMatchingAccountOperation.class)
public interface UnMatchingAccountOperation {

    @Schema(description = "List of AccountOperation for matching")
    @NotEmpty
    List<UnMatchingAccountOperationDetail> getAccountOperations();
}