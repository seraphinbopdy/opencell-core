package org.meveo.apiv2.language.service;

import jakarta.inject.Inject;

import jakarta.ws.rs.core.Response;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.apiv2.language.LanguageDto;
import org.meveo.apiv2.language.mapper.LanguageMapper;
import org.meveo.model.billing.Language;
import org.meveo.service.admin.impl.LanguageService;

import java.util.ArrayList;
import java.util.List;

public class LanguageApiService {

    @Inject
    private LanguageService languageService;

    LanguageMapper mapper = new LanguageMapper();

    public Language createLanguage(LanguageDto languageDto) {
        List<String> missingParameters = new ArrayList<>();
        if(languageDto.getCode() == null) {
            missingParameters.add("code");
        }
        if(languageDto.getDescription() == null) {
            missingParameters.add("description");
        }
        if(!missingParameters.isEmpty()) {
            throw new MissingParameterException(missingParameters);
        }
        
        if(languageService.findByCode(languageDto.getCode()) != null) {
            throw new EntityAlreadyExistsException(Language.class, languageDto.getCode());
        }
        
        if(languageService.findByDescription(languageDto.getDescription()) != null) {
            throw new BusinessException("Language with description " + languageDto.getDescription() + " already exists");
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

        if (id == null) {
            throw new MissingParameterException("id");
        }
        
        Language entity = mapper.toEntity(existingLanguage, languageDto);
        languageService.update(entity);
        return entity;
    }

    public Language findLanguage(Long id) {
        return languageService.findById(id);
    }

}
