package ch.sbb.polarion.extension.generic.auth;

import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.ISecurityService;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;

public interface AuthValidator {
    @NotNull Subject validate() throws AuthenticationFailedException;

    @NotNull AuthValidator withUserId(@NotNull String userId);
    @NotNull AuthValidator withSecret(@NotNull String secret);

    @NotNull AuthValidator withSecurityService(@NotNull ISecurityService securityService);
}
