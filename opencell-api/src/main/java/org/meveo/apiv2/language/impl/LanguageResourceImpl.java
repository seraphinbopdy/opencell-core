package org.meveo.apiv2.language.impl;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.core.Response;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.apiv2.language.LanguageDto;
import org.meveo.apiv2.language.resource.LanguageResource;
import org.meveo.apiv2.language.service.LanguageApiService;
import org.meveo.model.billing.Language;


@Interceptors({ WsRestApiInterceptor.class })
public class LanguageResourceImpl extends BaseRs implements LanguageResource {

    @Inject
    private LanguageApiService languageApiService;

    @Override
    public Response createLanguage(LanguageDto languageDto) {

        try {
            Language language =  languageApiService.createLanguage(languageDto);

            ActionStatus responseStatus = new ActionStatus();
            responseStatus.setStatus(ActionStatusEnum.SUCCESS);
            responseStatus.setEntityId(language.getId());
            responseStatus.setEntityCode(languageDto.getCode());

            return Response.ok(responseStatus).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, new ActionStatus());
        }
    }


    @Override
    public Response updateLanguage(Long id, LanguageDto languageDto) {
        try {
            languageApiService.updateLanguage(id, languageDto);

            ActionStatus responseStatus = new ActionStatus();
            responseStatus.setStatus(ActionStatusEnum.SUCCESS);

            return Response.ok(responseStatus).build();
        } catch (MeveoApiException e) {
            return errorResponse(e, new ActionStatus());
        }
    }
}
