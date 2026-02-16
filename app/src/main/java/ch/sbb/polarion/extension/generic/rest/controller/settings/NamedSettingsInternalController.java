package ch.sbb.polarion.extension.generic.rest.controller.settings;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.Revision;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.polarion.core.util.StringUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Hidden
@Tag(name = "Settings")
@Path("/internal")
@SuppressWarnings("unchecked")
public class NamedSettingsInternalController {

    protected final PolarionService polarionService;

    public NamedSettingsInternalController() {
        this(new PolarionService());
    }

    public NamedSettingsInternalController(PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    @GET
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the complete list of all supported features",
            responses = {
                    @ApiResponse(description = "List of supported features",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = String.class)
                            )
                    )
            }
    )
    public Collection<String> readFeaturesList() {
        return NamedSettingsRegistry.INSTANCE.getAll().stream().map(GenericNamedSettings::getFeatureName).toList();
    }

    @GET
    @Path("/settings/{feature}/names")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns names of specified setting in specified scope (global or certain project)",
            responses = {
                    @ApiResponse(description = "List of setting names",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SettingName.class)
                            )
                    )

            }
    )
    public Collection<SettingName> readSettingNames(@PathParam("feature") String feature, @QueryParam("scope") @DefaultValue("") String scope) {
        return NamedSettingsRegistry.INSTANCE.getByFeatureName(feature).readNames(scope);
    }

    @POST
    @Path("/settings/{feature}/names/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Updates name of specified named setting in specified scope (global or certain project)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Setting name updated successfully")
            }
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void renameSetting(@PathParam("feature") String feature, @PathParam("name") final String name,
                              @QueryParam("scope") @DefaultValue("") String scope, final String newName) {
        Collection<SettingName> settingNames = readSettingNames(feature, scope);
        SettingId id = SettingId.fromId(getIdByName(feature, scope, true, name));
        GenericNamedSettings settings = NamedSettingsRegistry.INSTANCE.getByFeatureName(feature);
        SettingsModel existingSetting = settings.read(scope, id, null);
        validateSettingName(newName);
        if (settingNames.stream().anyMatch(n -> Objects.equals(n.getScope(), scope) && Objects.equals(n.getName(), newName) && !Objects.equals(n.getId(), id.getIdentifier()))) {
            throw new IllegalArgumentException("Setting with the specified name already exists");
        }
        existingSetting.setName(newName);
        settings.save(scope, id, existingSetting);
    }

    @GET
    @Path("/settings/{feature}/names/{name}/revisions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns revisions history of specified setting with specified id and in specified scope (global or certain project)",
            responses = {
                    @ApiResponse(description = "List of revisions",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Revision.class)
                            )
                    )
            }
    )
    public @NotNull List<Revision> readRevisionsList(@PathParam("feature") String feature, @PathParam("name") String name,
                                                     @QueryParam("scope") @DefaultValue("") String scope) {
        GenericNamedSettings<?> settings = NamedSettingsRegistry.INSTANCE.getByFeatureName(feature);
        return settings.listRevisions(scope, SettingId.fromName(name));
    }

    @DELETE
    @Path("/settings/{feature}/names/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Deletes specified setting by id and scope (global or certain project)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Setting deleted successfully")
            }
    )
    public void deleteSetting(@PathParam("feature") String feature, @PathParam("name") String name,
                              @QueryParam("scope") @DefaultValue("") String scope) {
        GenericNamedSettings<?> settings = NamedSettingsRegistry.INSTANCE.getByFeatureName(feature);
        settings.delete(scope, SettingId.fromName(name));
    }

    @GET
    @Path("/settings/{feature}/names/{name}/content")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns values (content) of specified setting by its id, scope (global or certain project) and revision",
            responses = {
                    @ApiResponse(description = "Setting content",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SettingsModel.class)
                            )
                    )
            }
    )
    public SettingsModel readSetting(@PathParam("feature") String feature, @PathParam("name") String name,
                                     @QueryParam("scope") @DefaultValue("") String scope, @QueryParam("revision") String revision) {
        GenericNamedSettings<?> settings = NamedSettingsRegistry.INSTANCE.getByFeatureName(feature);
        return settings.read(scope, SettingId.fromName(name), revision);
    }

    @PUT
    @Path("/settings/{feature}/names/{name}/content")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Creates or updates named setting in specified scope (global or certain project). Creation scenario will use default setting value if no body specified in the request.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Setting created/updated successfully")
            }
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void saveSetting(@PathParam("feature") String feature, @PathParam("name") final String name,
                            @QueryParam("scope") @DefaultValue("") String scope, final String content) {
        GenericNamedSettings settings = NamedSettingsRegistry.INSTANCE.getByFeatureName(feature);
        String id = settings.getIdByName(scope, true, name);
        SettingsModel model;
        if (StringUtils.isEmpty(id)) {
            validateSettingName(name);
            if (readSettingNames(feature, scope).stream().anyMatch(n -> Objects.equals(n.getScope(), scope) && Objects.equals(n.getName(), name))) {
                throw new IllegalArgumentException("Setting with the specified name already exists");
            }
            id = UUID.randomUUID().toString();
            model = StringUtils.isEmpty(content) ? getDefaultValues(feature) : settings.fromJson(content);
        } else {
            model = settings.fromJson(content);
        }
        model.setName(name);
        settings.save(scope, SettingId.fromId(id), model);
    }

    @GET
    @Path("/settings/{feature}/default-content")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns default values of specified setting",
            responses = {
                    @ApiResponse(description = "Default values",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SettingsModel.class)
                            )
                    )
            }
    )
    public SettingsModel getDefaultValues(@PathParam("feature") String feature) {
        GenericNamedSettings<?> settings = NamedSettingsRegistry.INSTANCE.getByFeatureName(feature);
        return settings.defaultValues();
    }

    private String getIdByName(String feature, String scope, boolean limitToScope, String settingName) {
        return Optional.ofNullable(NamedSettingsRegistry.INSTANCE.getByFeatureName(feature).getIdByName(scope, limitToScope, settingName))
                .orElseThrow(() -> new IllegalArgumentException("No setting with the specified name found"));
    }

    private void validateSettingName(String settingName) {
        if (StringUtils.isEmpty(settingName)) {
            throw new IllegalArgumentException("Setting name required");
        } else if (!settingName.matches("[a-zA-Z0-9\\-_ ]+")) {
            throw new IllegalArgumentException("Setting name: only alphanumeric characters, hyphens and spaces are allowed");
        }
    }
}
