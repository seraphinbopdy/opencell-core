package org.meveo.apiv2.securityDeposit;

import java.math.BigDecimal;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.securityDeposit.SecurityTemplateStatusEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSecurityDepositTemplate.class)
public interface SecurityDepositTemplate extends Resource {

    @Schema(description = "The Template Name")
    @NotNull String getTemplateName();

    @Schema(description = "The Currency")
    @Nullable
    Resource getCurrency();

    @Schema(description = "The Allow Validity Date")
    boolean getAllowValidityDate();

    @Schema(description = "The Allow Validity Period")
    boolean getAllowValidityPeriod();

    @Schema(description = "The Min Amount")
    @Nullable
    BigDecimal getMinAmount();

    @Schema(description = "The Max Amount")
    @Nullable
    BigDecimal getMaxAmount();

    @Schema(description = "The Status")
    @NotNull
    SecurityTemplateStatusEnum getStatus();

    @Schema(description = "The Number Of Instantiation")
    @Nullable
    Integer getNumberOfInstantiation();

}
