package ch.sbb.polarion.extension.generic.rest.controller.roles;

import ch.sbb.polarion.extension.generic.rest.model.RolesInfo;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Lists the roles an administration page can offer as checkboxes.
 * <p>
 * Not part of the controllers every extension gets: only the few that store "who may do X" as role
 * names need it, and adding an endpoint to all of them would change their REST surface for nothing. An
 * extension opts in by naming this class (and {@link RolesApiController}) in its REST application's
 * {@code getExtensionControllerClasses()}.
 */
@Singleton
@Tag(name = "Roles")
@Path("/internal")
public class RolesInternalController {

    protected final PolarionService polarionService;

    public RolesInternalController() {
        this(new PolarionService());
    }

    public RolesInternalController(PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    @GET
    @Path("/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the global and project roles available in the specified scope",
            responses = @ApiResponse(responseCode = "200",
                    description = "Successfully retrieved the available roles",
                    content = @Content(schema = @Schema(implementation = RolesInfo.class))))
    public @NotNull RolesInfo getRoles(@Parameter(description = "Scope, e.g. project/<projectId>/ (empty for global scope)") @QueryParam("scope") @Nullable String scope) {
        return new RolesInfo(new ArrayList<>(polarionService.getGlobalRoles()), new ArrayList<>(polarionService.getProjectRoles(scope)));
    }
}
