package org.meveo.apiv2.settings;

import jakarta.annotation.Nonnull;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDunning.class)
public interface Dunning {
    @Nonnull
    Boolean getActivateDunning();
}
