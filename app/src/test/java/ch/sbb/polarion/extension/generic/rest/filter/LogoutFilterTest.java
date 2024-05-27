package ch.sbb.polarion.extension.generic.rest.filter;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.polarion.platform.security.ISecurityService;

import static ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter.ASYNC_SKIP_LOGOUT;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutFilterTest {

    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private ISecurityService securityService;

    @Test
    void successfulLogoutCallIfSubjectExists() throws IOException {
        Subject subject = new Subject();
        when(requestContext.getProperty(AuthenticationFilter.USER_SUBJECT)).thenReturn(subject);
        LogoutFilter filter = new LogoutFilter(securityService);
        filter.filter(requestContext, null);
        verify(securityService, times(1)).logout(subject);
    }

    @Test
    void doNotCallLogoutIfThereIsNoSubject() throws IOException {
        when(requestContext.getProperty(AuthenticationFilter.USER_SUBJECT)).thenReturn(null);
        LogoutFilter filter = new LogoutFilter(securityService);
        filter.filter(requestContext, null);
        verify(securityService, times(0)).logout(any());
    }

    @Test
    void doNotCallLogoutIfAsyncSkipPropertyIsSet() throws IOException {
        Subject subject = new Subject();
        when(requestContext.getProperty(AuthenticationFilter.USER_SUBJECT)).thenReturn(subject);
        when(requestContext.getProperty(ASYNC_SKIP_LOGOUT)).thenReturn(Boolean.TRUE);
        LogoutFilter filter = new LogoutFilter(securityService);
        filter.filter(requestContext, null);
        verify(securityService, times(0)).logout(any());
    }
}
