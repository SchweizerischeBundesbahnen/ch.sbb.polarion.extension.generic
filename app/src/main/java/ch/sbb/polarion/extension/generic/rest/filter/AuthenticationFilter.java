package ch.sbb.polarion.extension.generic.rest.filter;

import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.security.login.AccessToken;
import com.polarion.platform.security.login.IToken;

import javax.security.auth.Subject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Secured
@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final Logger logger = Logger.getLogger(AuthenticationFilter.class);

    public static final String BEARER = "Bearer";
    public static final String USER_SUBJECT = "user_subject";
    private final ISecurityService securityService;

    public AuthenticationFilter(ISecurityService securityService) {
        this.securityService = securityService;
    }

    public AuthenticationFilter() {
        this.securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
            logger.error("Missing authorization header in request");
            throw new NotAuthorizedException("Authorization header must be provided", Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", BEARER).build());
        }

        final String token = authorizationHeader.substring(BEARER.length()).trim();

        try {
            Subject subject = validateToken(token);
            requestContext.setProperty(USER_SUBJECT, subject);
        } catch (AuthenticationFailedException e) {
            logger.error("Authentication failed: " + e.getMessage(), e);
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private Subject validateToken(String token) throws AuthenticationFailedException {
        final IToken<AccessToken> accessToken = AccessToken.token(token);

        return securityService.login()
                .from("REST")
                .authenticator(AccessToken.id())
                .with(accessToken)
                .perform();
    }
}
