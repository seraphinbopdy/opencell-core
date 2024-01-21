package org.meveo.apiv2.catalog;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;
import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.model.DatePeriod;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.OneShotChargeTemplateTypeEnum;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableSimpleOneshotProductDto.class)
public interface SimpleOneshotProductDto extends SimpleChargeProductDto {

    OneShotChargeTemplateTypeEnum getOneShotChargeTemplateType();
    
}
