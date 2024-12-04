package org.meveo.apiv2.esignature;

import java.util.List;

import org.immutables.value.Value;
import org.meveo.model.esignature.SigantureAuthentificationMode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSigners.class)
public interface Signers {

    @Nullable
    InfoSigner getInfo();

    @Nullable
    @JsonProperty("signature_authentication_mode")
    SigantureAuthentificationMode getSignatureAuthenticationMode();

    @Nullable
    List<SignatureFields> getFields();

}
