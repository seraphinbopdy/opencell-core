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
public interface SimpleOneshotProductDto {
    
    @Nullable
    String getChargeCode();
    
    String getProductCode();
    
    String getLabel();
    
    BigDecimal getPrice();

    OneShotChargeTemplateTypeEnum getOneShotChargeTemplateType();
    
    @Nullable
    DatePeriod getValidity();

    @Schema(description = "Description of Parameter 1")
    @Nullable
    String getParameter1Description();

    @Schema(description = "Translated descriptions of Parameter 1")
    @Nullable
    List<LanguageDescriptionDto> getParameter1TranslatedDescriptions();

    @Schema(description = "Translated long descriptions of Parameter 1")
    @Nullable
    List<LanguageDescriptionDto> getParameter1TranslatedLongDescriptions();

    @Schema(description = "Format of Parameter 1")
    @Nullable
    ChargeTemplate.ParameterFormat getParameter1Format();

    @Schema(description = "Is Parameter 1 Mandatory?")
    @Nullable
    Boolean getParameter1IsMandatory();

    @Schema(description = "Is Parameter 1 Hidden?")
    @Nullable
    Boolean getParameter1IsHidden();

    // Parameter 2
    @Schema(description = "Description of Parameter 2")
    @Nullable
    String getParameter2Description();

    @Schema(description = "Translated descriptions of Parameter 2")
    @Nullable
    List<LanguageDescriptionDto> getParameter2TranslatedDescriptions();

    @Schema(description = "Translated long descriptions of Parameter 2")
    @Nullable
    List<LanguageDescriptionDto> getParameter2TranslatedLongDescriptions();

    @Schema(description = "Format of Parameter 2")
    @Nullable
    ChargeTemplate.ParameterFormat getParameter2Format();

    @Schema(description = "Is Parameter 2 Mandatory?")
    @Nullable
    Boolean getParameter2IsMandatory();

    @Schema(description = "Is Parameter 2 Hidden?")
    @Nullable
    Boolean getParameter2IsHidden();

    // Parameter 3
    @Schema(description = "Description of Parameter 3")
    @Nullable
    String getParameter3Description();

    @Schema(description = "Translated descriptions of Parameter 3")
    @Nullable
    List<LanguageDescriptionDto> getParameter3TranslatedDescriptions();

    @Schema(description = "Translated long descriptions of Parameter 3")
    @Nullable
    List<LanguageDescriptionDto> getParameter3TranslatedLongDescriptions();

    @Schema(description = "Format of Parameter 3")
    @Nullable
    ChargeTemplate.ParameterFormat getParameter3Format();

    @Schema(description = "Is Parameter 3 Mandatory?")
    @Nullable
    Boolean getParameter3IsMandatory();

    @Schema(description = "Is Parameter 3 Hidden?")
    @Nullable
    Boolean getParameter3IsHidden();

    // Parameter Extra
    @Schema(description = "Description of Extra Parameter")
    @Nullable
    String getParameterExtraDescription();

    @Schema(description = "Translated descriptions of Extra Parameter")
    @Nullable
    List<LanguageDescriptionDto> getParameterExtraTranslatedDescriptions();

    @Schema(description = "Translated long descriptions of Extra Parameter")
    @Nullable
    List<LanguageDescriptionDto> getParameterExtraTranslatedLongDescriptions();

    @Schema(description = "Format of Extra Parameter")
    @Nullable
    ChargeTemplate.ParameterFormat getParameterExtraFormat();

    @Schema(description = "Is Extra Parameter Mandatory?")
    @Nullable
    Boolean getParameterExtraIsMandatory();

    @Schema(description = "Is Extra Parameter Hidden?")
    @Nullable
    Boolean getParameterExtraIsHidden();
    
}
