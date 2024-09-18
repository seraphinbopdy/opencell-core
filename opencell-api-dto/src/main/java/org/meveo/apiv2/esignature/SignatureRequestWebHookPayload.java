package org.meveo.apiv2.esignature;

import java.io.Serializable;
import java.util.Date;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSignatureRequestWebHookPayload.class)
public interface SignatureRequestWebHookPayload extends Serializable {

    @Nullable
    @JsonProperty("event_id")
    String getEventId();

    @Nullable
    @JsonProperty("event_name")
    String getEventName();

    @Nullable
    @JsonProperty("event_time")
    Date getEventTime();

    @Nullable
    @JsonProperty("subscription_id")
    String getSubscriptionId();

    @Nullable
    @JsonProperty("subscription_description")
    String getSubscriptionDescription();

    boolean getSandbox();

    @Nullable
    WebhookData getData();

}
