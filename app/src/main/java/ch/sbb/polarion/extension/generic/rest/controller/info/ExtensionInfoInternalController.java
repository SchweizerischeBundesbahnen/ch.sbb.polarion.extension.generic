package ch.sbb.polarion.extension.generic.rest.controller.info;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.properties.ConfigurationProperties;
import ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration;
import ch.sbb.polarion.extension.generic.properties.ExtensionConfiguration;
import ch.sbb.polarion.extension.generic.rest.model.ConfigurationPropertiesModel;
import ch.sbb.polarion.extension.generic.rest.model.ConfigurationPropertyModel;
import ch.sbb.polarion.extension.generic.rest.model.Context;
import ch.sbb.polarion.extension.generic.rest.model.Version;
import ch.sbb.polarion.extension.generic.util.ExtensionInfo;
import com.polarion.core.util.logging.Logger;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
@Hidden
@Tag(name = "Extension Information")
@Path("/internal")
public class ExtensionInfoInternalController {

    private final Logger logger = Logger.getLogger(ExtensionInfoInternalController.class);

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

    @GET
    @Path("/configuration-properties")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the extension configuration properties (active and obsolete)",
            responses = @ApiResponse(description = "Configuration properties",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ConfigurationPropertiesModel.class))))
    public ConfigurationPropertiesModel getConfigurationProperties() {
        ExtensionConfiguration configuration = CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();
        return ConfigurationPropertiesModel.builder()
                .properties(toModels(configuration.getConfigurationProperties(), true))
                .obsoleteProperties(toModels(configuration.getObsoleteConfigurationProperties(), false))
                .build();
    }

    @GET
    @Path("/configuration-status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the extension configuration status for the given scope",
            responses = @ApiResponse(description = "Configuration status entries",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = ConfigurationStatus.class)))))
    public List<ConfigurationStatus> getConfigurationStatus(
            @Parameter(description = "Scope to evaluate the status for, e.g. 'project/elibrary/'") @QueryParam("scope") String scope) {
        return new ArrayList<>(ConfigurationStatusProvider.getAllStatuses(scope));
    }

    @GET
    @Path("/readme")
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "Returns the build-generated README help article shown on the About page",
            responses = @ApiResponse(description = "HTML help article"))
    public String getDocumentation() {
        return readHtmlFile("about.html");
    }

    @GET
    @Path("/user-guide")
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "Returns the build-generated USER_GUIDE help article shown on the User Guide page",
            responses = @ApiResponse(description = "HTML help article"))
    public String getUserGuide() {
        return readHtmlFile("user-guide.html");
    }

    /**
     * Reads a build-generated HTML file from the classpath under
     * {@code /webapp/{extensionContext}-admin/html/}. Returns an empty string when the file is absent
     * (the markdown sources are not packaged into the jar, so there is nothing to fall back to).
     */
    private String readHtmlFile(@NotNull String htmlFileName) {
        String extensionContext = ExtensionInfo.getInstance().getContext().getExtensionContext();
        String resourcePath = "/webapp/" + extensionContext + "-admin/html/" + htmlFileName;
        try (InputStream inputStream = ExtensionInfo.class.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logger.error("Could not read the HTML file from " + resourcePath, e);
        }
        return "";
    }

    private static @NotNull List<ConfigurationPropertyModel> toModels(@NotNull ConfigurationProperties properties, boolean withDefaultsAndDescription) {
        List<String> keys = new ArrayList<>(properties.keySet());
        Collections.sort(keys);
        List<ConfigurationPropertyModel> result = new ArrayList<>();
        for (String key : keys) {
            ConfigurationProperties.Value value = properties.getProperty(key);
            ConfigurationPropertyModel.ConfigurationPropertyModelBuilder builder = ConfigurationPropertyModel.builder()
                    .key(key)
                    .value(value.value());
            if (withDefaultsAndDescription) {
                builder.defaultValue(value.defaultValue()).description(value.description());
            }
            result.add(builder.build());
        }
        return result;
    }
}
