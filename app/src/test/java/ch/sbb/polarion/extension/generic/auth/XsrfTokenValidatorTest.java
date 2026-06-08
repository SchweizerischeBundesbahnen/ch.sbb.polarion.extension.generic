package ch.sbb.polarion.extension.generic.auth;

import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import com.polarion.core.config.Configuration;
import com.polarion.core.config.IConfiguration;
import com.polarion.core.config.IRestConfiguration;
import com.polarion.core.util.security.PasswordEncryptor;
import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.container.ContainerRequestContext;

import javax.security.auth.Subject;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XsrfTokenValidatorTest {

    private static final String USER_ID = "testUser";
    private static final String VALIDATOR_INPUT = "dummy-value";

    // Fixed epoch-milli bounds so the tests don't depend on the system clock:
    // Long.MAX_VALUE is always in the future, 0 (Instant.EPOCH) is always in the past.
    private static final String FAR_FUTURE_TIMESTAMP = String.valueOf(Long.MAX_VALUE);
    private static final String PAST_TIMESTAMP = "0";

    @Mock
    private ISecurityService securityService;

    @Test
    void testValidationFailsWhenRestApiTokenDisabled() {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class)) {
            mockRestApiTokenEnabled(configurationMockedStatic, false);

            assertThrows(AuthenticationFailedException.class, createValidator()::validate);
        }
    }

    @Test
    void testValidTokenReturnsCurrentSubject() throws AuthenticationFailedException {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class);
             MockedStatic<PasswordEncryptor> passwordEncryptorMockedStatic = mockStatic(PasswordEncryptor.class)) {
            mockRestApiTokenEnabled(configurationMockedStatic, true);
            mockDecryptedToken(passwordEncryptorMockedStatic, FAR_FUTURE_TIMESTAMP + "$" + USER_ID);

            Subject subject = new Subject();
            when(securityService.getCurrentSubject()).thenReturn(subject);
            when(securityService.getSubjectUser(subject)).thenReturn(USER_ID);

            assertSame(subject, createValidator().validate());
        }
    }

    @Test
    void testExpiredTokenRejected() {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class);
             MockedStatic<PasswordEncryptor> passwordEncryptorMockedStatic = mockStatic(PasswordEncryptor.class)) {
            mockRestApiTokenEnabled(configurationMockedStatic, true);
            mockDecryptedToken(passwordEncryptorMockedStatic, PAST_TIMESTAMP + "$" + USER_ID);

            assertThrows(AuthenticationFailedException.class, createValidator()::validate);
        }
    }

    @Test
    void testMalformedTimestampRejected() {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class);
             MockedStatic<PasswordEncryptor> passwordEncryptorMockedStatic = mockStatic(PasswordEncryptor.class)) {
            mockRestApiTokenEnabled(configurationMockedStatic, true);
            mockDecryptedToken(passwordEncryptorMockedStatic, "notANumber$" + USER_ID);

            assertThrows(AuthenticationFailedException.class, createValidator()::validate);
        }
    }

    @Test
    void testTokenWithDifferentUserRejected() {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class);
             MockedStatic<PasswordEncryptor> passwordEncryptorMockedStatic = mockStatic(PasswordEncryptor.class)) {
            mockRestApiTokenEnabled(configurationMockedStatic, true);
            mockDecryptedToken(passwordEncryptorMockedStatic, FAR_FUTURE_TIMESTAMP + "$anotherUser");

            assertThrows(AuthenticationFailedException.class, createValidator()::validate);
        }
    }

    @Test
    void testTokenWithoutSeparatorRejected() {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class);
             MockedStatic<PasswordEncryptor> passwordEncryptorMockedStatic = mockStatic(PasswordEncryptor.class)) {
            mockRestApiTokenEnabled(configurationMockedStatic, true);
            mockDecryptedToken(passwordEncryptorMockedStatic, "tokenWithoutSeparator");

            assertThrows(AuthenticationFailedException.class, createValidator()::validate);
        }
    }

    @Test
    void testUndecryptableTokenRejected() {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class);
             MockedStatic<PasswordEncryptor> passwordEncryptorMockedStatic = mockStatic(PasswordEncryptor.class)) {
            mockRestApiTokenEnabled(configurationMockedStatic, true);
            passwordEncryptorMockedStatic.when(() -> PasswordEncryptor.getInstance(anyString())).thenThrow(new IllegalStateException("decryption failed"));

            assertThrows(AuthenticationFailedException.class, createValidator()::validate);
        }
    }

    @Test
    void testValidTokenButDifferentCurrentUserRejected() {
        try (MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class);
             MockedStatic<PasswordEncryptor> passwordEncryptorMockedStatic = mockStatic(PasswordEncryptor.class)) {
            mockRestApiTokenEnabled(configurationMockedStatic, true);
            mockDecryptedToken(passwordEncryptorMockedStatic, FAR_FUTURE_TIMESTAMP + "$" + USER_ID);

            Subject subject = new Subject();
            when(securityService.getCurrentSubject()).thenReturn(subject);
            when(securityService.getSubjectUser(subject)).thenReturn("anotherUser");

            assertThrows(AuthenticationFailedException.class, createValidator()::validate);
        }
    }

    @Test
    void testUpdateRequestContextSetsUserSubjectAndSkipLogout() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        Subject subject = new Subject();

        createValidator().updateRequestContext(requestContext, subject);

        verify(requestContext).setProperty(AuthenticationFilter.USER_SUBJECT, subject);
        verify(requestContext).setProperty(LogoutFilter.XSRF_SKIP_LOGOUT, Boolean.TRUE);
    }

    private XsrfTokenValidator createValidator() {
        XsrfTokenValidator validator = new XsrfTokenValidator();
        validator.userId(USER_ID).secret(VALIDATOR_INPUT).securityService(securityService);
        return validator;
    }

    private void mockRestApiTokenEnabled(MockedStatic<Configuration> configurationMockedStatic, boolean enabled) {
        IConfiguration configuration = mock(IConfiguration.class);
        IRestConfiguration restConfiguration = mock(IRestConfiguration.class);
        configurationMockedStatic.when(Configuration::getInstance).thenReturn(configuration);
        when(configuration.rest()).thenReturn(restConfiguration);
        when(restConfiguration.restApiTokenEnabled()).thenReturn(enabled);
    }

    private void mockDecryptedToken(MockedStatic<PasswordEncryptor> passwordEncryptorMockedStatic, String decryptedToken) {
        PasswordEncryptor passwordEncryptor = mock(PasswordEncryptor.class);
        passwordEncryptorMockedStatic.when(() -> PasswordEncryptor.getInstance(anyString())).thenReturn(passwordEncryptor);
        when(passwordEncryptor.decrypt(VALIDATOR_INPUT)).thenReturn(decryptedToken);
    }
}
