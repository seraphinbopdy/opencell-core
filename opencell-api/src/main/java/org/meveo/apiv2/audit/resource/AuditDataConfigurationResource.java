package org.meveo.apiv2.audit.resource;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import org.meveo.apiv2.audit.AuditDataConfigurationDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/auditDataConfiguration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AuditDataConfigurationResource {

    @POST
    @Path("/")
    @Operation(summary = "This endpoint allows to create data auditing configuration resource", tags = { "AuditDataConfiguration" }, description = "create new data audit configuration", responses = {
            @ApiResponse(responseCode = "200", description = "the data audit configuration successfully created, and the id is returned in the response"),
            @ApiResponse(responseCode = "400", description = "bad request when data audit configuration information contains an error") })
    Response createAuditDataConfiguration(@Parameter(description = "the data audit configuration", required = true) AuditDataConfigurationDto auditDataConfiguration);

    @PUT
    @Path("/{id}")
    @Operation(summary = "This endpoint allows to update an existing data auditing configuration resource", tags = {
            "AuditDataConfiguration" }, description = "update an existing data auditing configuration", responses = {
                    @ApiResponse(responseCode = "200", description = "the data auditing configuration successfully updated, and the id is returned in the response"),
                    @ApiResponse(responseCode = "400", description = "bad request when data auditing configuration information contains an error") })
    Response updateAuditDataConfiguration(@Parameter(description = "id of data auditing configuration", required = true) @PathParam("id") Long id,
            @Parameter(description = "the data auditing configuration", required = true) AuditDataConfigurationDto auditDataConfiguration);

    @GET
    @Path("/{entityClass}")
    @Operation(summary = "This endpoint allows to find an existing data auditing configuration resource", tags = { "AuditDataConfiguration" }, description = "find an existing data auditing configuration", responses = {
            @ApiResponse(responseCode = "200", description = "the data auditing configuration successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "bad request when data auditing configuration information contains an error") })
    Response find(@Parameter(description = "entity class", required = true) @PathParam("entityClass") String entityClass, @Context Request request);

    @DELETE
    @Path("/{entityClass}")
    @Operation(summary = "This endpoint allows to delete an existing data auditing configuration resource", tags = {
            "AuditDataConfiguration" }, description = "delete an existing data auditing configuration", responses = {
                    @ApiResponse(responseCode = "200", description = "the data auditing configuration successfully deleted"),
                    @ApiResponse(responseCode = "400", description = "bad request when data auditing configuration is not found") })
    Response delete(@Parameter(description = "entity class", required = true) @PathParam("entityClass") String entityClass, @Context Request request);

    @GET
    @Path("/")
    @Operation(summary = "This endpoint allows to find list of data auditing configuration resource", tags = {
            "AuditDataConfiguration" }, description = "find a list of an existing data auditing configuration", responses = {
                    @ApiResponse(responseCode = "200", description = "return list of data auditing configuration"),
                    @ApiResponse(responseCode = "400", description = "bad request when data auditing configuration information contains an error") })
    Response list(@Parameter(description = "The offset of the list") @DefaultValue("0") @QueryParam("offset") Long offset, 
    		@Parameter(description = "The limit element per page") @DefaultValue("50") @QueryParam("limit") Long limit,
    		@Parameter(description = "The sort by field") @QueryParam("sort") String sort, 
    		@Parameter(description = "The ordering by field") @QueryParam("orderBy") String orderBy, 
    		@Parameter(description = "Map of filters") Map<String, Object> filter,
            @Context Request request);
    }