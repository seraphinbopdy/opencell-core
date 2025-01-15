package org.meveo.apiv2.language.mapper;

import org.meveo.apiv2.language.LanguageDto;
import org.meveo.apiv2.ordering.ResourceMapper;
import org.meveo.model.billing.Language;

import java.util.HashMap;
import java.util.Map;

public class LanguageMapper extends ResourceMapper<LanguageDto, Language> {

    @Override
    public LanguageDto toResource(Language entity) {
        return null;
    }

    @Override
    public Language toEntity(LanguageDto resource) {
        var language = new Language();
        return toEntity(language, resource);
    }
    
    public Language toEntity(Language entity, LanguageDto resource) {

        entity.setLanguageCode(resource.getCode());
        entity.setDescriptionEn(resource.getDescription());
        if(resource.getLanguageDescriptions()!= null) {
            Map<String, String> languageDescriptions = new HashMap<>();
            resource.getLanguageDescriptions().forEach(languageDescription -> {
                languageDescriptions.put(languageDescription.getLanguageCode(), languageDescription.getDescription());
            });
            entity.setDescriptionI18n(languageDescriptions);
        }

        return entity;
    }
}
