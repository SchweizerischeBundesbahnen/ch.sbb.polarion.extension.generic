package ch.sbb.polarion.extension.generic.rest.filter;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;

@Secured
@Provider
public class LogoutFilter implements ContainerResponseFilter {
    // This request property should be set by async processing to prevent logout in response filter before finishing the process
    // In this case async process itself is responsible for logout after the completion
    public static final String ASYNC_SKIP_LOGOUT = "async.skip.logout";

    private final ISecurityService securityService;

    public LogoutFilter(ISecurityService securityService) {
        this.securityService = securityService;
    }

    @SuppressWarnings("unused")
    public LogoutFilter() {
        this.securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        Subject subject = (Subject) requestContext.getProperty(AuthenticationFilter.USER_SUBJECT);
        if ((requestContext.getProperty(ASYNC_SKIP_LOGOUT) != Boolean.TRUE) && (subject != null)) {
            securityService.logout(subject);
        }
    }
}
