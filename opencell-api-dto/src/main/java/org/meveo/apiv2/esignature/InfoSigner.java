package org.meveo.apiv2.esignature;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableInfoSigner.class)
public interface InfoSigner {

    @Nullable
    @JsonProperty("first_name")
    String getFirstName();

    @Nullable
    @JsonProperty("last_name")
    String getLastName();

    @Nullable
    String getEmail();

    @Nullable
    @JsonProperty("phone_number")
    String getPhoneNumber();

    @Nullable
    String getLocale();
}
