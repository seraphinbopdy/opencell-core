package org.meveo.apiv2.audit.resource;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/v2/auditDataLog")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AuditDataLogResource {

    @GET
    @Path("/{entityClass}/{entityId}")
    @Operation(summary = "This endpoint allows to find audit data logs for a given entity", tags = { "AuditDataLog" }, description = "find audit data logs for a given entity", responses = {
            @ApiResponse(responseCode = "200", description = "the data auditing logs successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "bad request when data auditing log information contains an error") })
    Response listByEntity(@Parameter(description = "entity class", required = true) @PathParam("entityClass") String entityClass,
            @Parameter(description = "entity id", required = true) @PathParam("entityId") Long entityId, @Parameter(description = "limit to field change") @QueryParam("field") String field, @Context Request request);

    @GET
    @Path("/")
    @Operation(summary = "This endpoint allows to find a list of audit data logs", tags = { "AuditDataLog" }, description = "find audit data logs", responses = {
            @ApiResponse(responseCode = "200", description = "the data auditing logs successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "bad request when data auditing log information contains an error") })
    Response list(@Parameter(description = "The offset of the list") @DefaultValue("0") @QueryParam("offset") Long offset,
            @Parameter(description = "The limit element per page") @DefaultValue("50") @QueryParam("limit") Long limit, @Parameter(description = "The sort by field") @QueryParam("sort") String sort,
            @Parameter(description = "The ordering by field") @QueryParam("orderBy") String orderBy, @Parameter(description = "Map of filters") Map<String, Object> filter, @Context Request request);
}