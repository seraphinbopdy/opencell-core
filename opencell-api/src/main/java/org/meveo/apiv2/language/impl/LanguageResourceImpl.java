package org.meveo.apiv2.language.impl;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.core.Response;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.language.LanguageDto;
import org.meveo.apiv2.language.resource.LanguageResource;
import org.meveo.apiv2.language.service.LanguageApiService;
import org.meveo.model.billing.Language;


@Interceptors({ WsRestApiInterceptor.class })
public class LanguageResourceImpl implements LanguageResource {

    @Inject
    private LanguageApiService languageApiService;

    @Override
    public Response createLanguage(LanguageDto languageDto) {
        if (languageDto.getCode() == null || languageDto.getDescription() == null) {
            return Response.status(412).entity("Missing parameters: 'code' and 'description' are required").build();
        }

        try {
            Language language =  languageApiService.createLanguage(languageDto);

            ActionStatus responseStatus = new ActionStatus();
            responseStatus.setStatus(ActionStatusEnum.SUCCESS);
            responseStatus.setEntityId(language.getId());
            responseStatus.setEntityCode(languageDto.getCode());

            return Response.ok(responseStatus).build();
        } catch (Exception e) {
            return Response.status(400).entity("Language creation failed: " + e.getMessage()).build();
        }
    }


    @Override
    public Response updateLanguage(Long id, LanguageDto languageDto) {
        if (id == null) {
            return Response.status(412).entity("Missing ID").build();
        }
        if (languageDto == null) {
            return Response.status(412).entity("Missing request body").build();
        }

        try {
            languageApiService.updateLanguage(id, languageDto);

            ActionStatus responseStatus = new ActionStatus();
            responseStatus.setStatus(ActionStatusEnum.SUCCESS);

            return Response.ok(responseStatus).build();
        } catch (Exception e) {
            return Response.status(400).entity("Language creation failed: " + e.getMessage()).build();
        }
    }
}
