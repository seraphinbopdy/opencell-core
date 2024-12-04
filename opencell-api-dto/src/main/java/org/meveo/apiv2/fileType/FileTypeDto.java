package org.meveo.apiv2.fileType;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableFileTypeDto.class)
public interface FileTypeDto extends Resource{
	
	@Nullable
    @Schema(description = "code of the file type")
	String getCode();

}
