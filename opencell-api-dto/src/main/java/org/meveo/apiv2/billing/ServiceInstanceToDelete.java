package org.meveo.apiv2.billing;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableServiceInstanceToDelete.class)
public interface ServiceInstanceToDelete {

    @Schema(description = "List of Service instance to delete")
    @NotEmpty
    List<Long> getIds();
}