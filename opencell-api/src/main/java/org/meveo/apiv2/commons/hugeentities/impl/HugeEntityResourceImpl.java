/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */
package org.meveo.apiv2.commons.hugeentities.impl;

import org.meveo.admin.job.UpdateHugeEntityJob;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.apiv2.common.HugeEntity;
import org.meveo.apiv2.commons.hugeentities.resource.HugeEntityResource;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.jpa.EntityManagerWrapper;
import org.meveo.jpa.MeveoJpa;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.BatchEntityService;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.securityDeposit.impl.FinanceSettingsService;

import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * An implementation of huge entity resource.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Interceptors({WsRestApiInterceptor.class})
public class HugeEntityResourceImpl implements HugeEntityResource {

    @Inject
    @MeveoJpa
    private EntityManagerWrapper entityManagerWrapper;

    @Inject
    private FinanceSettingsService financeSettingsService;

    @Inject
    private JobInstanceService jobInstanceService;

    @Inject
    protected CustomFieldInstanceService customFieldInstanceService;

    @Inject
    private BatchEntityService batchEntityService;

    @Override
    public Response update(HugeEntity hugeEntity) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        //Filters
        if (hugeEntity.getFilters().isEmpty()) {
            throw new MissingParameterException("filters");
        }

        //targetJob
        String targetJob = hugeEntity.getTargetJob();
        JobInstance jobInstance = ofNullable(jobInstanceService.findByCode(targetJob))
                .orElseThrow(() -> new EntityDoesNotExistsException(JobInstance.class, targetJob));

        //targetEntity
        String targetEntity = (String) customFieldInstanceService.getCFValue(jobInstance, UpdateHugeEntityJob.CF_ENTITY_ClASS_NAME);
        Class hugeEntityClass = batchEntityService.getHugeEntityClass(targetEntity);
        String hugeEntityClassName = hugeEntityClass.getSimpleName();

        boolean isEntityWithHugeVolume = financeSettingsService.isEntityWithHugeVolume(hugeEntityClassName);
        if (isEntityWithHugeVolume) {
            batchEntityService.create(hugeEntity, hugeEntity.getFilters(), hugeEntityClassName);
            result.setMessage("Entity " + hugeEntityClassName + " is marked as \"huge\". " +
                    "Your filter is recorded as a batch and will be processed later by a " + targetJob + " job. " +
                    "You will receive a notification email when the batch has been processed.");
            return Response.status(Response.Status.ACCEPTED).entity(result).build();
        } else {
            String defaultFilter = (String) customFieldInstanceService.getCFValue(jobInstance, UpdateHugeEntityJob.CF_DEFAULT_FILTER);
            String selectQuery = batchEntityService.getSelectQuery(hugeEntityClass, hugeEntity.getFilters(), defaultFilter, false);
            List<Long> ids = entityManagerWrapper.getEntityManager().createQuery(selectQuery).getResultList();

            StringBuilder updateQuery = new StringBuilder("UPDATE ").append(hugeEntityClassName).append(" SET ")
                    .append("updated=").append(QueryBuilder.paramToString(new Date()));

            String fieldsToUpdate = (String) customFieldInstanceService.getCFValue(jobInstance, UpdateHugeEntityJob.CF_FIELDS_TO_UPDATE);
            if (StringUtils.isBlank(hugeEntityClassName)) {
                throw new MissingParameterException("Fields to update are missing on the job : " + targetJob);
            }
            updateQuery.append(", ").append(fieldsToUpdate);

            int updated = batchEntityService.update(updateQuery, ids);
            if (updated > 0) {
                result.setMessage(updated + " elements updated");
            } else {
                result.setMessage("No element found to update");
            }
            return Response.status(Response.Status.OK).entity(result).build();
        }
    }
}