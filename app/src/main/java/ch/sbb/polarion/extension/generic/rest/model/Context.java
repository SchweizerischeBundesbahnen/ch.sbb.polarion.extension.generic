package ch.sbb.polarion.extension.generic.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents the context for building URLs related to Polarion services")
public class Context {

    @Schema(description = "The extension context used as a base for URL construction", example = "pdf-exporter")
    private @NotNull String extensionContext;

    @Schema(description = "Returns the base URL constructed with the extension context", example = "/polarion/pdf-exporter")
    public @NotNull String getBaseUrl() {
        return "/polarion/" + extensionContext;
    }

    @Schema(description = "Returns the REST API URL constructed with the extension context", example = "/polarion/pdf-exporter/rest")
    public @NotNull String getRestUrl() {
        return "/polarion/" + extensionContext + "/rest";
    }

    @Schema(description = "Returns the Swagger UI URL for the REST API")
    public @NotNull String getSwaggerUiUrl() {
        return getRestUrl() + "/swagger";
    }
}
