package org.meveo.apiv2.settings;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunning.class)
public interface Dunning {
    @NotNull
    Boolean getActivateDunning();
}
