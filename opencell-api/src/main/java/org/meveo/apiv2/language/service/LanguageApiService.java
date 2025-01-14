package org.meveo.apiv2.language.service;

import jakarta.inject.Inject;

import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.apiv2.language.LanguageDto;
import org.meveo.apiv2.language.mapper.LanguageMapper;
import org.meveo.model.billing.Language;
import org.meveo.service.admin.impl.LanguageService;

public class LanguageApiService {

    @Inject
    private LanguageService languageService;

    LanguageMapper mapper = new LanguageMapper();

    public Language createLanguage(LanguageDto languageDto) {
        
        if(languageService.findByCode(languageDto.getCode()) != null) {
            throw new EntityAlreadyExistsException(Language.class, languageDto.getCode());
        }
        
        Language entity = mapper.toEntity(languageDto);
        languageService.create(entity);
        return entity;
    }

    public Language updateLanguage(Long id, LanguageDto languageDto) {

        Language existingLanguage = languageService.findById(id);
        if(existingLanguage == null) {
            throw new EntityDoesNotExistsException(Language.class, id);
        }
        
        Language entity = mapper.toEntity(existingLanguage, languageDto);
        languageService.update(entity);
        return entity;
    }

    public Language findLanguage(Long id) {
        return languageService.findById(id);
    }

}
