package ch.sbb.polarion.extension.generic.rest.controller;

import ch.sbb.polarion.extension.generic.rest.model.Context;
import ch.sbb.polarion.extension.generic.rest.model.Version;
import ch.sbb.polarion.extension.generic.util.ExtensionInfo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Hidden
@Tag(name = "Extension Information")
@Path("/internal")
public class ExtensionInfoInternalController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/context")
    @Operation(
            summary = "Returns basic context information of Polarion's extension",
            responses = {
                    @ApiResponse(
                            description = "Context information",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Context.class)))
            }
    )
    public Context getContext() {
        return ExtensionInfo.getInstance().getContext();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/version")
    @Operation(
            summary = "Returns version of Polarion's extension",
            responses = {
                    @ApiResponse(description = "Version information",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Version.class))
                    )
            }
    )
    public Version getVersion() {
        return ExtensionInfo.getInstance().getVersion();
    }
}
