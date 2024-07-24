package ch.sbb.polarion.extension.generic.rest.filter;

import ch.sbb.polarion.extension.generic.auth.AuthValidator;
import ch.sbb.polarion.extension.generic.auth.ValidatorFactory;
import ch.sbb.polarion.extension.generic.auth.ValidatorType;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.ISecurityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Secured
@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    public static final String BEARER = "Bearer";
    public static final String USER_SUBJECT = "user_subject";
    public static final String X_POLARION_REST_TOKEN_HEADER = "X-Polarion-REST-Token";

    @Context
    private HttpServletRequest httpServletRequest;

    private final ISecurityService securityService;

    public AuthenticationFilter() {
        this.securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
    }

    public AuthenticationFilter(@NotNull ISecurityService securityService) {
        this.securityService = securityService;
    }

    public AuthenticationFilter(@NotNull ISecurityService securityService, @NotNull HttpServletRequest httpServletRequest) {
        this.securityService = securityService;
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        @Nullable String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        @Nullable String xsrfToken = requestContext.getHeaderString(X_POLARION_REST_TOKEN_HEADER);

        @NotNull AuthValidator authValidator = getAuthValidator(authorizationHeader, xsrfToken);

        try {
            @NotNull Subject subject = authValidator.validate();
            authValidator.updateRequestContext(requestContext, subject);
        } catch (AuthenticationFailedException e) {
            throw new NotAuthorizedException("Authentication failed: " + e.getMessage(),
                    Response.status(Response.Status.UNAUTHORIZED)
                            .build());
        }
    }

    private @NotNull AuthValidator getAuthValidator(@Nullable String authorizationHeader, @Nullable String xsrfToken) {
        if (authorizationHeader != null) {
            return createPersonalAccessTokenValidator(authorizationHeader);
        } else if (xsrfToken != null) {
            return createXsrfTokenValidator(xsrfToken);
        } else {
            throw new NotAuthorizedException("Authorization header must be provided",
                    Response.status(Response.Status.UNAUTHORIZED)
                            .header(HttpHeaders.WWW_AUTHENTICATE, BEARER)
                            .build());
        }
    }

    private @NotNull AuthValidator createPersonalAccessTokenValidator(@NotNull String authorizationHeader) {
        if (!authorizationHeader.startsWith(BEARER)) {
            throw new NotAuthorizedException("Invalid authorization header format",
                    Response.status(Response.Status.UNAUTHORIZED)
                            .header(HttpHeaders.WWW_AUTHENTICATE, BEARER)
                            .build());
        }

        String personalAccessToken = authorizationHeader.substring(BEARER.length()).trim();
        return ValidatorFactory.getValidator(ValidatorType.PERSONAL_ACCESS_TOKEN)
                .secret(personalAccessToken)
                .securityService(securityService);
    }

    private @NotNull AuthValidator createXsrfTokenValidator(@NotNull String xsrfToken) {
        String userId = httpServletRequest.getUserPrincipal().getName();
        return ValidatorFactory.getValidator(ValidatorType.XSRF_TOKEN)
                .userId(userId)
                .secret(xsrfToken)
                .securityService(securityService);
    }
}