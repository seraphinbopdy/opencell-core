package org.meveo.apiv2.dunning.impl;


import org.meveo.api.dto.LanguageDescriptionDto;
import org.meveo.apiv2.dunning.ImmutableDunningPauseReason;
import org.meveo.apiv2.generic.ResourceMapper;
import org.meveo.model.AuditableEntity;
import org.meveo.model.dunning.DunningPauseReason;
import org.meveo.model.dunning.DunningSettings;
import org.meveo.service.billing.impl.TradingLanguageService;

import static org.meveo.commons.utils.EjbUtils.getServiceInterface;

public class DunningPauseReasonsMapper extends ResourceMapper<org.meveo.apiv2.dunning.DunningPauseReason, DunningPauseReason> {

    private TradingLanguageService tradingLanguageService = (TradingLanguageService) getServiceInterface(TradingLanguageService.class.getSimpleName());;

    @Override
    protected org.meveo.apiv2.dunning.DunningPauseReason toResource(DunningPauseReason entity) {
        return ImmutableDunningPauseReason.builder()
                .id(entity.getId())
                .pauseReason(entity.getPauseReason())
                .description(entity.getDescription())
                .descriptionI18n(LanguageDescriptionDto.convertMultiLanguageFromMapOfValues(entity.getDescriptionI18n()))
                .dunningSettings(createResource((AuditableEntity) entity.getDunningSettings())).build();
    }

    @Override
    protected DunningPauseReason toEntity(org.meveo.apiv2.dunning.DunningPauseReason resource) {
        var entity = new DunningPauseReason();
        entity.setId(resource.getId());
        var dunningSettings = new DunningSettings();
        dunningSettings.setId(resource.getDunningSettings().getId());
        dunningSettings.setCode(resource.getDunningSettings().getCode());
        entity.setDunningSettings(dunningSettings);
        entity.setDescription(resource.getDescription());
        entity.setPauseReason(resource.getPauseReason());
        entity.setDescriptionI18n(LanguageDescriptionDto.convertMultiLanguageToMapOfValues(resource.getDescriptionI18n(), null, tradingLanguageService.listLanguageCodes()));
        return entity;
    }
}
