package org.meveo.apiv2.audit;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAuditDataConfigurationDto.class)
public interface AuditDataConfigurationDto extends Resource {

    @Schema(description = "entity type/class to track")
    @NotNull
    String getEntityClass();

    @Nullable
    @Schema(description = "Entity fields to track, a comma separated list of fields")
    String getFields();

    @Nullable
    @Schema(description = "Actions to track - A comma separated list of actions: CREATE, UPDATE, DELETE")
    String getActions();
}