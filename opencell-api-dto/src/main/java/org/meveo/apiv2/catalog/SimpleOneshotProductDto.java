package org.meveo.apiv2.catalog;

import org.immutables.value.Value;
import org.meveo.model.catalog.OneShotChargeTemplateTypeEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Style(jdkOnly = true, jakarta = true)
@JsonDeserialize(as = ImmutableSimpleOneshotProductDto.class)
public interface SimpleOneshotProductDto extends SimpleChargeProductDto {

    OneShotChargeTemplateTypeEnum getOneShotChargeTemplateType();
    
}
