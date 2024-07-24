package ch.sbb.polarion.extension.generic.auth;

import com.polarion.platform.security.ISecurityService;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractAuthValidator implements AuthValidator {
    protected String userId;
    protected String secret;

    protected ISecurityService securityService;

    @Override
    public @NotNull AuthValidator userId(@NotNull String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public @NotNull AuthValidator secret(@NotNull String secret) {
        this.secret = secret;
        return this;
    }

    @Override
    public @NotNull AuthValidator securityService(@NotNull ISecurityService securityService) {
        this.securityService = securityService;
        return this;
    }
}
