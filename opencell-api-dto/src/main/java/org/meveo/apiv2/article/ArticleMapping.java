package org.meveo.apiv2.article;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableArticleMapping.class)
public interface ArticleMapping extends Resource {

	@Schema(description = "code of article mapping")
    String getCode();

	@Schema(description = "description of article mapping")
    String getDescription();

	@Schema(description = "mapping script associated to this article mapping")
    @Nullable
    Resource getMappingScript();
}
