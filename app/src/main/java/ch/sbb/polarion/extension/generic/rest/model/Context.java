package ch.sbb.polarion.extension.generic.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Schema(description = "Represents the context for building URLs related to Polarion services")
public class Context {

    @Schema(description = "The extension context used as a base for URL construction")
    private final @NotNull String extensionContext;

    @Schema(description = "Returns the base URL constructed with the extension context")
    public @NotNull String getBaseUrl() {
        return "/polarion/" + extensionContext;
    }

    @Schema(description = "Returns the REST API URL constructed with the extension context")
    public @NotNull String getRestUrl() {
        return "/polarion/" + extensionContext + "/rest";
    }

    @Schema(description = "Returns the Swagger UI URL for the REST API")
    public @NotNull String getSwaggerUiUrl() {
        return getRestUrl() + "/swagger";
    }

    public Context(@Nullable String extensionContext) {
        if (extensionContext == null || extensionContext.isBlank()) {
            throw new IllegalArgumentException("Extension context must be provided");
        }
        this.extensionContext = extensionContext;
    }
}
