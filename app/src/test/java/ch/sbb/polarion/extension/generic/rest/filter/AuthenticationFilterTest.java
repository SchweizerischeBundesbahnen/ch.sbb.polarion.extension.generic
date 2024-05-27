package ch.sbb.polarion.extension.generic.rest.filter;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.security.login.AccessToken;
import com.polarion.platform.security.login.ILogin;
import com.polarion.platform.security.login.IToken;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private ISecurityService securityService;
    @Mock
    private ILogin login;
    @Mock
    private ILogin.IBase base;
    @Mock
    private ILogin.IUsingAuthenticator authenticator;
    @Mock
    private ILogin.IFinal iFinal;

    @Test
    void filterRequestWithoutAuthorizationHeader() {

        when(requestContext.getHeaderString("Authorization")).thenReturn(null);
        AuthenticationFilter filter = new AuthenticationFilter(securityService);

        assertThatThrownBy(() -> filter.filter(requestContext))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessageContaining("Authorization header must be provided");
    }

    @Test
    void filterRequestWithoutBaererInAuthorizationHeader() {
        when(requestContext.getHeaderString("Authorization")).thenReturn("wrong token");
        AuthenticationFilter filter = new AuthenticationFilter(securityService);

        assertThatThrownBy(() -> filter.filter(requestContext))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessageContaining("Authorization header must be provided");
    }

    @Test
    void filterRequest() throws IOException, AuthenticationFailedException {
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer token");
        when(securityService.login()).thenReturn(login);
        when(login.from("REST")).thenReturn(base);
        when(base.authenticator(any())).thenReturn(authenticator);
        when(authenticator.with((IToken<AccessToken>) any())).thenReturn(iFinal);

        Subject subject = new Subject();
        when(iFinal.perform()).thenReturn(subject);

        AuthenticationFilter filter = new AuthenticationFilter(securityService);
        filter.filter(requestContext);
        verify(requestContext, times(1)).setProperty("user_subject", subject);
    }


    @Test
    void filterRequestWithFailedAuthentication() throws IOException, AuthenticationFailedException {
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer token");
        when(securityService.login()).thenReturn(login);
        when(login.from("REST")).thenReturn(base);
        when(base.authenticator(any())).thenReturn(authenticator);
        when(authenticator.with((IToken<AccessToken>) any())).thenReturn(iFinal);

        when(iFinal.perform()).thenThrow(new AuthenticationFailedException(""));

        AuthenticationFilter filter = new AuthenticationFilter(securityService);

        filter.filter(requestContext);
        verify(requestContext, times(1)).abortWith(any());
    }
}
