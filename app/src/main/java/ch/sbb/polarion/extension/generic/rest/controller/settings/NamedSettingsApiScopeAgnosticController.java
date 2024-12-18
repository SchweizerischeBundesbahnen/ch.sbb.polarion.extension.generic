package ch.sbb.polarion.extension.generic.rest.controller.settings;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.settings.Revision;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Collection;
import java.util.List;

import static ch.sbb.polarion.extension.generic.settings.GenericNamedSettings.DEFAULT_SCOPE;

@Secured
@Path("/api")
public class NamedSettingsApiScopeAgnosticController extends NamedSettingsApiController {

    @Override
    @GET
    @Path("/settings/{feature}/names")
    @Operation(summary = "Returns names of specified setting",
            responses = {
                    @ApiResponse(description = "List of setting names",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SettingName.class)
                            )
                    )
            }
    )
    public Collection<SettingName> readSettingNames(@PathParam("feature") String feature, @QueryParam("scope") @DefaultValue("") @Parameter(hidden = true) String scope) {
        return super.readSettingNames(feature, DEFAULT_SCOPE);
    }

    @Override
    @GET
    @Path("/settings/{feature}/names/{name}/content")
    @Operation(summary = "Returns values (content) of specified setting by its id and revision",
            responses = {
                    @ApiResponse(description = "Setting content",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SettingsModel.class)
                            )
                    )
            }
    )
    public SettingsModel readSetting(@PathParam("feature") String feature, @PathParam("name") String name,
                                     @QueryParam("scope") @DefaultValue("") @Parameter(hidden = true) String scope, @QueryParam("revision") String revision) {
        return super.readSetting(feature, name, DEFAULT_SCOPE, revision);
    }

    @Override
    @PUT
    @Path("/settings/{feature}/names/{name}/content")
    @Operation(summary = "Creates or updates named setting. Creation scenario will use default setting value if no body specified in the request.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Setting created/updated successfully")
            }
    )
    public void saveSetting(@PathParam("feature") String feature, @PathParam("name") final String name,
                            @QueryParam("scope") @DefaultValue("") @Parameter(hidden = true) String scope, final String content) {
        super.saveSetting(feature, name, DEFAULT_SCOPE, content);
    }

    @Override
    @POST
    @Path("/settings/{feature}/names/{name}")
    @Operation(summary = "Updates name of specified named setting",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Setting name updated successfully")
            }
    )
    public void renameSetting(@PathParam("feature") String feature, @PathParam("name") final String name,
                              @QueryParam("scope") @DefaultValue("") @Parameter(hidden = true) String scope, final String newName) {
        super.renameSetting(feature, name, DEFAULT_SCOPE, newName);
    }

    @Override
    @DELETE
    @Path("/settings/{feature}/names/{name}")
    @Operation(summary = "Deletes specified setting by id",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Setting deleted successfully")
            }
    )
    public void deleteSetting(@PathParam("feature") String feature, @PathParam("name") String name,
                              @QueryParam("scope") @DefaultValue("") @Parameter(hidden = true) String scope) {
        super.deleteSetting(feature, name, DEFAULT_SCOPE);
    }

    @Override
    @GET
    @Path("/settings/{feature}/names/{name}/revisions")
    @Operation(summary = "Returns revisions history of specified setting with specified id",
            responses = {
                    @ApiResponse(description = "List of revisions",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Revision.class)
                            )
                    )
            }
    )
    public @NotNull List<Revision> readRevisionsList(@PathParam("feature") String feature, @PathParam("name") String name,
                                                     @QueryParam("scope") @DefaultValue("") @Parameter(hidden = true) String scope) {
        return super.readRevisionsList(feature, name, DEFAULT_SCOPE);
    }

}
