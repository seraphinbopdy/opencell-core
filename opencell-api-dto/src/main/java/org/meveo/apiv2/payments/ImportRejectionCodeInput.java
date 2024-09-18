package org.meveo.apiv2.payments;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableImportRejectionCodeInput.class)
public interface ImportRejectionCodeInput {

    @Nullable
    @Value.Default
    default RejectionCodeImportMode getMode() {
        return RejectionCodeImportMode.UPDATE;
    }

    @Nullable
    @Value.Default
    default Boolean getIgnoreLanguageErrors() {
        return Boolean.TRUE;
    }

    @Nullable
    String getBase64csv();
}
