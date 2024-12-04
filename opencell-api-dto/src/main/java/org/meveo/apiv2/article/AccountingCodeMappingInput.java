package org.meveo.apiv2.article;

import java.util.List;

import jakarta.annotation.Nullable;

import org.immutables.value.Value;
import org.meveo.apiv2.models.Resource;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableAccountingCodeMappingInput.class)
public interface AccountingCodeMappingInput extends Resource {

    @Schema(description = "Accounting article code")
    @Nullable
    String getAccountingArticleCode();

    @Schema(description = "Accounting code mapping list")
    @Nullable
    List<AccountingCodeMapping> getAccountingCodeMappings();
}