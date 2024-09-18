package org.meveo.apiv2.esignature;

import org.immutables.value.Value;
import org.meveo.model.esignature.NatureDocument;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableFilesSignature.class)
public interface FilesSignature {

    @Nullable
    String getFilePath();

    @Nullable
    NatureDocument getNature();

    @JsonProperty("parse_anchors")
    boolean getParseAnchors();
}
