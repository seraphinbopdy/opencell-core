/**
 * 
 */
package org.meveo.api.endpoint.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.endpoint.EndpointDto;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.model.endpoint.Endpoint;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;

@Path("/endpoint")
@Api("EndpointRs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface EndpointResource {

    @POST
    @ApiOperation(value = "Create a new endpoint")
    Response create(@Valid @NotNull EndpointDto endpointDto) throws BusinessException;

    @PUT
    @ApiOperation(value = "Update an existing endpoint")
    Response update(@Valid @NotNull EndpointDto endpointDto) throws BusinessException;

    @PUT
    @Path("/createOrUpdate")
    @ApiOperation(value = "Create a new or update an existing endpoint")
    Response createOrUpdate(@Valid @NotNull EndpointDto endpointDto) throws BusinessException;

    /**
     * Delete a {@link Endpoint}
     *
     * @param code Code of the {@link Endpoint} to delete
     */
    @DELETE
    @Path("/{code}")
    @ApiOperation(value = "Delete endpoint")
    Response remove(@PathParam("code") @NotNull @ApiParam("Code of the endpoint") String code) throws BusinessException, EntityDoesNotExistsException;

    /**
     * Find a {@link Endpoint} by code
     *
     * @param code Code of the {@link Endpoint} to find
     */

    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Find endpoint by code")
    Response find(@PathParam("code") @NotNull @ApiParam("Code of the endpoint") String code);

    /**
     * Check exist a {@link Endpoint}
     *
     * @param code Code of the {@link Endpoint} to check
     */
    @HEAD
    @Path("/{code}")
    @ApiOperation(value = "Check exist an endpoint")
    Response exists(@PathParam("code") @NotNull @ApiParam("Code of the endpoint") String code);

    /**
     * List endpoints matching a given criteria
     *
     * @param pagingAndFiltering Pagination and filtering criteria
     * @return List of endpoints
     */
    @POST
    @Path("/filtering")
    @Operation(summary = "List endpoints matching a given criteria")
    Response list(PagingAndFiltering pagingAndFiltering);

    /**
     * Generate open api json of a {@link Endpoint}
     *
     * @param code Code of the {@link Endpoint} to generate open api json
     */
    @GET
    @Path("/openApi/{code}")
    @ApiOperation(value = "Generate open api json of the endpoint")
    Response generateOpenApiJson(@PathParam("code") @NotNull @ApiParam("Code of the endpoint") String code);
}