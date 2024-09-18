package org.meveo.apiv2.settings;

import static java.lang.Boolean.FALSE;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;
import org.meveo.model.settings.MaximumValidityUnitEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableOpenOrderSettingInput.class)
public interface OpenOrderSettingInput extends Resource {

    @Nullable
    @Value.Default
    default Boolean getUseOpenOrders() {
        return FALSE;
    }

    @Nullable
    @Value.Default
    default Boolean getApplyMaximumValidity() {
        return FALSE;
    }

    @Nullable
    Integer getApplyMaximumValidityValue();

    @Nullable
    MaximumValidityUnitEnum getApplyMaximumValidityUnit();

    @Nullable
    @Value.Default
    default Boolean getDefineMaximumValidity() {
        return FALSE;
    }

    @Nullable
    Integer getDefineMaximumValidityValue();

    @Nullable
    @Value.Default
    default Boolean getUseManagmentValidationForOOQuotation() {
        return FALSE;
    }
}