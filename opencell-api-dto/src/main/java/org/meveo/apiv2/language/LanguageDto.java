package org.meveo.apiv2.language;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableLanguageDto.class)
public interface LanguageDto extends Resource {

    @Nullable
    @Schema(description = "Code of the language")
    String getCode();

    @Nullable
    @Schema(description = "Description of the language")
    String getDescription();

    @Nullable
    @Schema(description = "Descriptions of the language in different languages")
    List<LanguageDescriptionDto> getLanguageDescriptions();
}
