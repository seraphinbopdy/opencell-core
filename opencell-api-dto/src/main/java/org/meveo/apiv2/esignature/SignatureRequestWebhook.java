package org.meveo.apiv2.esignature;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSignatureRequestWebhook.class)
public interface SignatureRequestWebhook extends Serializable {

    @Nullable
    String getId();

    @Nullable
    String getStatus();

    @Nullable
    String getName();

    @Nullable
    @JsonProperty("delivery_mode")
    String getDeliveryMode();

    @Nullable
    @JsonProperty("created_at")
    Date getCreateAt();

    @Nullable
    String getTimezone();

    @JsonProperty("email_custom_note")
    @Nullable
    String getEmailCustomNote();

    @JsonProperty("expiration_date")
    @Nullable
    Date getExpirationDate();

    @Nullable
    String getSource();

    @JsonProperty("ordered_signers")
    boolean getOrderSigners();

    @JsonProperty("external_id")
    @Nullable
    String getExternalId();

    @Nullable
    List<SignerWebhook> getSigners();

    @Nullable
    List<Map<String, Object>> getApprovers();

    @Nullable
    Object getSender();

    @Nullable
    List<Map<String, Object>> getDocuments();

    @Nullable
    @JsonProperty("reminder_settings")
    Object getReminderSettings();

    @Nullable
    @JsonProperty("workspace_id")
    String getWorkspaceId();

}
