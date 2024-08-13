package org.meveo.apiv2.media.file.upload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.meveo.apiv2.media.MediaFile;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/media/files")
public interface FileUploadResource {

    @POST
    @Path("/upload")
    @Operation(summary = "Upload a media file",
            tags = {"Media"},
            description = "Upload a media file",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "success"),
                    @ApiResponse(responseCode = "404",
                            description = "Entity does not exist"),
                    @ApiResponse(responseCode = "412",
                            description = "Missing parameters"),
                    @ApiResponse(responseCode = "400",
                            description = "media file upload failed")
            })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Response uploadFile(@MultipartForm MediaFile file);
}
