package org.meveo.apiv2.esignature;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableWebhookData.class)
public interface WebhookData {
    @Nullable
    @JsonProperty("signature_request")
    SignatureRequestWebhook getSignatureRequestWebhook();
}
