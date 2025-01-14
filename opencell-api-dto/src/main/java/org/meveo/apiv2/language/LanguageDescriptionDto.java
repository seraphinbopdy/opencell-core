package org.meveo.apiv2.language;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableLanguageDescriptionDto.class)
public interface LanguageDescriptionDto extends Resource {

    String getLanguageCode();

    String getDescription();
}
