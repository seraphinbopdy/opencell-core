package org.meveo.apiv2.AcountReceivable;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableMatchingAccountOperation.class)
public interface MatchingAccountOperation {

    @Schema(description = "List of AccountOperation and Sequence for matching")
    @NotEmpty
    List<AccountOperationAndSequence> getAccountOperations();
}