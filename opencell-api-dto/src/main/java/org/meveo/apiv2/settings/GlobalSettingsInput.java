package org.meveo.apiv2.settings;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableGlobalSettingsInput.class)
public interface GlobalSettingsInput extends Resource {

    @NotNull
    QuoteSettings getQuoteSettings();

    @Nullable
    Dunning getDunning();
}
