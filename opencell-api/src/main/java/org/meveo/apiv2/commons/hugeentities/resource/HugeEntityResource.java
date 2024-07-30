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
package org.meveo.apiv2.commons.hugeentities.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.apiv2.common.HugeEntity;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * A definition of huge entity resource.
 *
 * @author Abdellatif BARI
 * @since 15.1.0
 */
@Path("/v2/hugeEntity")
@Produces({APPLICATION_JSON})
@Consumes({APPLICATION_JSON})
public interface HugeEntityResource {

    /**
     * Update Huge entity
     */
    @PUT
    @Path("/update")
    @Operation(
            summary = "Update huge entity",
            description = "Update huge entity",
            operationId = "POST_HugeEntity_updateHugeEntity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The is successfully updated", content = @Content(schema = @Schema(implementation = ActionStatusEnum.class))),
                    @ApiResponse(responseCode = "202", description = "The modification is received and a batch entity is created", content = @Content(schema = @Schema(implementation = ActionStatusEnum.class))),
                    @ApiResponse(responseCode = "400", description = "Entity class name is missing", content = @Content(schema = @Schema(implementation = BusinessException.class))),
                    @ApiResponse(responseCode = "404", description = "The entity name not found", content = @Content(schema = @Schema(implementation = NotFoundException.class))),
                    @ApiResponse(responseCode = "404", description = "The job instance doesn't exist", content = @Content(schema = @Schema(implementation = EntityDoesNotExistsException.class))),
                    @ApiResponse(responseCode = "412", description = "The filters are missing", content = @Content(schema = @Schema(implementation = MissingParameterException.class))),
                    @ApiResponse(responseCode = "412", description = "Fields to update are missing", content = @Content(schema = @Schema(implementation = MissingParameterException.class)))

            })
    Response update(@Parameter(description = "Advanced filter of huge entity for updating", required = true) HugeEntity hugeEntity);
}