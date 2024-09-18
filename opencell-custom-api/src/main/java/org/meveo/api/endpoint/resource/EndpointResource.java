/**
 * 
 */
package org.meveo.api.endpoint.resource;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.endpoint.EndpointDto;
import org.meveo.api.dto.response.PagingAndFiltering;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.model.endpoint.Endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/endpoint")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface EndpointResource {

    @POST
    @Operation(summary = "Create a new endpoint")
    Response create(@Valid @NotNull EndpointDto endpointDto) throws BusinessException;

    @PUT
    @Operation(summary = "Update an existing endpoint")
    Response update(@Valid @NotNull EndpointDto endpointDto) throws BusinessException;

    @PUT
    @Path("/createOrUpdate")
    @Operation(summary = "Create a new or update an existing endpoint")
    Response createOrUpdate(@Valid @NotNull EndpointDto endpointDto) throws BusinessException;

    /**
     * Delete a {@link Endpoint}
     *
     * @param code Code of the {@link Endpoint} to delete
     */
    @DELETE
    @Path("/{code}")
    @Operation(summary = "Delete endpoint")
    Response remove(@PathParam("code") @NotNull @Parameter(description = "Code of the endpoint") String code) throws BusinessException, EntityDoesNotExistsException;

    /**
     * Find a {@link Endpoint} by code
     *
     * @param code Code of the {@link Endpoint} to find
     */

    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Find endpoint by code")
    Response find(@PathParam("code") @NotNull @Parameter(description = "Code of the endpoint") String code);

    /**
     * Check exist a {@link Endpoint}
     *
     * @param code Code of the {@link Endpoint} to check
     */
    @HEAD
    @Path("/{code}")
    @Operation(summary = "Check exist an endpoint")
    Response exists(@PathParam("code") @NotNull @Parameter(description = "Code of the endpoint") String code);

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
    @Operation(summary = "Generate open api json of the endpoint")
    Response generateOpenApiJson(@PathParam("code") @NotNull @Parameter(description = "Code of the endpoint") String code);
}