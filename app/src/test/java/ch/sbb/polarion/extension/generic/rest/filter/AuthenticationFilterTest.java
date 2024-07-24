package ch.sbb.polarion.extension.generic.rest.filter;

import ch.sbb.polarion.extension.generic.auth.AuthValidator;
import ch.sbb.polarion.extension.generic.auth.ValidatorFactory;
import ch.sbb.polarion.extension.generic.auth.ValidatorType;
import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.security.login.AccessToken;
import com.polarion.platform.security.login.ILogin;
import com.polarion.platform.security.login.IToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private ILogin.IFinal loginFinal;
    @Mock
    private HttpServletRequest httpServletRequest;

    @Test
    void filterRequestWithoutAuthorizationHeaderAndXsrfHeader() {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(requestContext.getHeaderString(AuthenticationFilter.X_POLARION_REST_TOKEN_HEADER)).thenReturn(null);

        AuthenticationFilter filter = new AuthenticationFilter(securityService);

        assertThatThrownBy(() -> filter.filter(requestContext))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessageContaining("Authorization header must be provided");
    }

    @Test
    void filterRequestWithoutBearerInAuthorizationHeader() {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("wrong token");
        when(requestContext.getHeaderString(AuthenticationFilter.X_POLARION_REST_TOKEN_HEADER)).thenReturn(null);

        AuthenticationFilter filter = new AuthenticationFilter(securityService);

        assertThatThrownBy(() -> filter.filter(requestContext))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessageContaining("Invalid authorization header format");
    }

    @Test
    void filterRequestWithValidBearerToken() throws IOException, AuthenticationFailedException {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token");
        when(requestContext.getHeaderString(AuthenticationFilter.X_POLARION_REST_TOKEN_HEADER)).thenReturn(null);

        when(securityService.login()).thenReturn(login);
        when(login.from("REST")).thenReturn(base);
        when(base.authenticator(any())).thenReturn(authenticator);
        when(authenticator.with((IToken<AccessToken>) any())).thenReturn(loginFinal);

        Subject subject = new Subject();
        when(loginFinal.perform()).thenReturn(subject);

        AuthenticationFilter filter = new AuthenticationFilter(securityService);
        filter.filter(requestContext);
        verify(requestContext, times(1)).setProperty("user_subject", subject);
    }


    @Test
    void filterRequestWithFailedAuthentication() throws IOException, AuthenticationFailedException {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer failed_token");
        when(requestContext.getHeaderString(AuthenticationFilter.X_POLARION_REST_TOKEN_HEADER)).thenReturn(null);

        when(securityService.login()).thenReturn(login);
        when(login.from("REST")).thenReturn(base);
        when(base.authenticator(any())).thenReturn(authenticator);
        when(authenticator.with((IToken<AccessToken>) any())).thenReturn(loginFinal);

        when(loginFinal.perform()).thenThrow(new AuthenticationFailedException("Something went wrong"));

        AuthenticationFilter filter = new AuthenticationFilter(securityService);

        assertThatThrownBy(() -> filter.filter(requestContext))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessageContaining("Something went wrong");
    }

    @Test
    void filterRequestWithValidXsrfToken() throws IOException, AuthenticationFailedException {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(requestContext.getHeaderString(AuthenticationFilter.X_POLARION_REST_TOKEN_HEADER)).thenReturn("validXsrfToken");

        Principal userPrincipal = mock(Principal.class);
        when(httpServletRequest.getUserPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getName()).thenReturn("user");

        AuthValidator xsrfValidator = mock(AuthValidator.class);
        when(xsrfValidator.validate()).thenReturn(new Subject());
        when(xsrfValidator.userId(anyString())).thenReturn(xsrfValidator);
        when(xsrfValidator.secret(anyString())).thenReturn(xsrfValidator);
        when(xsrfValidator.securityService(securityService)).thenReturn(xsrfValidator);
        doCallRealMethod().when(xsrfValidator).updateRequestContext(any(), any());
        try (MockedStatic<ValidatorFactory> validatorFactoryMockedStatic = mockStatic(ValidatorFactory.class)) {
            when(ValidatorFactory.getValidator(ValidatorType.XSRF_TOKEN)).thenReturn(xsrfValidator);

            AuthenticationFilter filter = new AuthenticationFilter(securityService, httpServletRequest);
            filter.filter(requestContext);
            verify(requestContext, times(1)).setProperty(eq("user_subject"), any(Subject.class));
        }
    }

    @Test
    void filterRequestWithInvalidXsrfToken() {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(requestContext.getHeaderString(AuthenticationFilter.X_POLARION_REST_TOKEN_HEADER)).thenReturn("invalid_xsrf_token");

        Principal userPrincipal = mock(Principal.class);
        when(httpServletRequest.getUserPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getName()).thenReturn("user");

        AuthenticationFilter filter = new AuthenticationFilter(securityService, httpServletRequest);

        assertThatThrownBy(() -> filter.filter(requestContext))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessageContaining("Invalid XSRF token");
    }

    @Test
    void filterRequestWithXsrfTokenForDifferentUser() {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(requestContext.getHeaderString(AuthenticationFilter.X_POLARION_REST_TOKEN_HEADER)).thenReturn("xsrf_token_for_different_user");

        Principal userPrincipal = mock(Principal.class);
        when(httpServletRequest.getUserPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getName()).thenReturn("different_user");

        AuthenticationFilter filter = new AuthenticationFilter(securityService, httpServletRequest);

        assertThatThrownBy(() -> filter.filter(requestContext))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessageContaining("Invalid XSRF token");
    }
}
