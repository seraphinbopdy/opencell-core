package org.meveo.apiv2.esignature;

import java.util.List;

import org.immutables.value.Value;
import org.meveo.model.esignature.DeliveryMode;
import org.meveo.model.esignature.Operator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSigantureRequest.class)
public interface SigantureRequest {

    @Nullable
    Operator getOperator();

    @Nullable
    String getName();

    @Nullable
    @JsonProperty("delivery_mode")
    DeliveryMode getDeliveryMode();

    @Nullable
    @JsonProperty("custom_experience_id")
    String getCustomExperienceId();

    @Nullable
    @JsonProperty("external_id")
    String getExternalId();

    @Nullable
    List<FilesSignature> getFilesToSign();

    @Nullable
    List<Signers> getSigners();
}
