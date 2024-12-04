package org.meveo.apiv2.esignature;

import java.io.Serializable;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSignerWebhook.class)
public interface SignerWebhook extends Serializable {
    @Nullable
    String getId();

    @Nullable
    List<Answers> getAnswers();

    @Nullable
    String getStatus();

    @Nullable
    @JsonProperty("delivery_mode")
    Object getDeliveryMode();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableAnswers.class)
    interface Answers extends Serializable {
        @Nullable
        @JsonProperty("field_id")
        String getFieldId();

        @Nullable
        @JsonProperty("field_type")
        String getFieldType();

        @Nullable
        String getQuestion();

        @Nullable
        String getAnswer();
    }
}
