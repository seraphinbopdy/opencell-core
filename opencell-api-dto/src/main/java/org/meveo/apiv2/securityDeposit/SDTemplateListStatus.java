package org.meveo.apiv2.securityDeposit;

import java.util.List;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSDTemplateListStatus.class)
public interface SDTemplateListStatus extends Resource {

    @Schema()
    @JsonAlias("SDTemplateList")
    @NotNull List<Resource> getSecurityDepositTemplates();

    @Schema()
    @NotNull String getStatus();




}
