package org.meveo.apiv2.generic;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableLanguageDescription.class)
public interface LanguageDescription extends Resource {

    String getLanguageDescriptionCode();

    String getDescription();
}
