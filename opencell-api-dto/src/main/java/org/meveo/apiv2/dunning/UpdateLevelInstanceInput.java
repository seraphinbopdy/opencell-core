package org.meveo.apiv2.dunning;

import java.util.Date;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.model.dunning.DunningLevelInstanceStatusEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableUpdateLevelInstanceInput.class)
public interface UpdateLevelInstanceInput {

    @Nullable
    Integer getDaysOverdue();

    @Nullable
    DunningLevelInstanceStatusEnum getLevelStatus();
    
    @Nullable
    List<DunningActionInstanceInput> getActions();

    @Nullable
    Date getExecutionDate();

    @Nullable
    @Schema(description = "custom field associated to dunning action")
    CustomFieldsDto getCustomFields();
}
