package ch.sbb.polarion.extension.generic.rest.controller.roles;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.rest.model.RolesInfo;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Token-authenticated twin of {@link RolesInternalController}, following the same Api/Internal pairing
 * the settings controllers use: the same endpoint under {@code /api}, executed with elevated rights,
 * because a token caller has no Polarion session to read the security service with.
 */
@Singleton
@Secured
@Path("/api")
public class RolesApiController extends RolesInternalController {

    public RolesApiController() {
        super();
    }

    public RolesApiController(PolarionService polarionService) {
        super(polarionService);
    }

    @Override
    public @NotNull RolesInfo getRoles(@Nullable String scope) {
        return polarionService.callPrivileged(() -> super.getRoles(scope));
    }
}
