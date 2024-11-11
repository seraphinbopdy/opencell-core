package org.meveo.apiv2.dunning.impl;


import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.apiv2.dunning.ImmutableDunningStopReason;
import org.meveo.apiv2.generic.ResourceMapper;
import org.meveo.model.AuditableEntity;
import org.meveo.model.billing.TradingLanguage;
import org.meveo.model.dunning.DunningSettings;
import org.meveo.model.dunning.DunningStopReason;
import org.meveo.service.billing.impl.TradingLanguageService;

import static org.meveo.commons.utils.EjbUtils.getServiceInterface;

public class DunningStopReasonsMapper extends ResourceMapper<org.meveo.apiv2.dunning.DunningStopReason, DunningStopReason> {

    private TradingLanguageService tradingLanguageService = (TradingLanguageService) getServiceInterface(TradingLanguageService.class.getSimpleName());;

    @Override
    protected org.meveo.apiv2.dunning.DunningStopReason toResource(DunningStopReason entity) {
        return ImmutableDunningStopReason.builder()
                .id(entity.getId())
                .stopReason(entity.getStopReason())
                .description(entity.getDescription())
                .descriptionI18n(LanguageDescriptionDto.convertMultiLanguageFromMapOfValues(entity.getDescriptionI18n()))
                .dunningSettings(createResource((AuditableEntity) entity.getDunningSettings())).build();
    }

    @Override
    protected DunningStopReason toEntity(org.meveo.apiv2.dunning.DunningStopReason resource) {
        var entity = new DunningStopReason();
        entity.setId(resource.getId());
        resource.getDunningSettings();
        var dunningSettings = new DunningSettings();
        dunningSettings.setId(resource.getDunningSettings().getId());
        dunningSettings.setCode(resource.getDunningSettings().getCode());
        entity.setDunningSettings(dunningSettings);
        entity.setDescription(resource.getDescription());
        entity.setDescriptionI18n(LanguageDescriptionDto.convertMultiLanguageToMapOfValues(resource.getDescriptionI18n(), null, tradingLanguageService.listLanguageCodes()));
        entity.setStopReason(resource.getStopReason());
        return entity;
    }
}
