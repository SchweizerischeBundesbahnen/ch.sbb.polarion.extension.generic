package ch.sbb.polarion.extension.generic.auth;

import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.ISecurityService;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import javax.ws.rs.container.ContainerRequestContext;

public interface AuthValidator {
    @NotNull Subject validate() throws AuthenticationFailedException;

    @NotNull AuthValidator userId(@NotNull String userId);
    @NotNull AuthValidator secret(@NotNull String secret);

    @NotNull AuthValidator securityService(@NotNull ISecurityService securityService);

    default void updateRequestContext(@NotNull ContainerRequestContext requestContext, @NotNull Subject subject) {
        requestContext.setProperty(AuthenticationFilter.USER_SUBJECT, subject);
    }
}
