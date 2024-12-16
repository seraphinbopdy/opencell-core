package org.meveo.apiv2.catalog;

import java.util.Set;

import org.immutables.value.Value;
import org.meveo.api.dto.cpq.ProductVersionAttributeDTO;
import org.meveo.model.catalog.OneShotChargeTemplateTypeEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSimpleOneshotProductDto.class)
public interface SimpleOneshotProductDto extends SimpleChargeProductDto {

	@Nullable
    @Schema(description = "Code of the price plan to be associated")
    String getPricePlanCode();
    
    @Nullable
    @Schema(description = "Flag to control price publication", defaultValue = "true")
    Boolean getPublishPrice();
    
    @Nullable
    @Schema(description = "Set of product attributes")
    Set<ProductVersionAttributeDTO> getAttributes();
    
}
