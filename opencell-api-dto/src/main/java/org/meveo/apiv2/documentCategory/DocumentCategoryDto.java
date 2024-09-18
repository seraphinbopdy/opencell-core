package org.meveo.apiv2.documentCategory;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableDocumentCategoryDto.class)
public interface DocumentCategoryDto extends Resource{
	
	@Nullable
    @Schema(description = "code of the document category")
	String getCode();
	
	@Nullable
    @Schema(description = "relative path")
	String getRelativePath();

}
