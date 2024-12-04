package org.meveo.apiv2.esignature;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSignatureFields.class)
public interface SignatureFields {

    int getPage();

    int getWidth();

    int getX();

    int getY();
}
