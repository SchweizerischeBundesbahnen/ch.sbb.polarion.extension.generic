package ch.sbb.polarion.extension.generic.auth;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import com.polarion.core.config.Configuration;
import com.polarion.core.util.security.PasswordEncryptor;
import com.polarion.platform.internal.XsrfTokenKeyStorage;
import com.polarion.platform.security.AuthenticationFailedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.time.Instant;
import java.util.Objects;

public class XsrfTokenValidator extends AbstractAuthValidator {

    @Override
    public @NotNull Subject validate() throws AuthenticationFailedException {
        if (!Configuration.getInstance().rest().restApiTokenEnabled()) {
            throw new AuthenticationFailedException("REST API token is disabled");
        }

        if (isXsrfTokenValid(userId, secret)) {
            return getUserSubject(userId);
        } else {
            throw new AuthenticationFailedException("Invalid XSRF token");
        }
    }

    @Override
    public void updateRequestContext(@NotNull ContainerRequestContext requestContext, @NotNull Subject subject) {
        super.updateRequestContext(requestContext, subject);
        requestContext.setProperty(LogoutFilter.XSRF_SKIP_LOGOUT, Boolean.TRUE);
    }

    private boolean isXsrfTokenValid(@NotNull String userId, @NotNull String encryptedXsrfToken) {
        String token = decryptXsrfToken(encryptedXsrfToken);
        if (token == null) {
            return false;
        }

        // Polarion 2606 token layout (see com.polarion.alm.server.xsrf.XsrfTokenServiceImpl):
        // <salt>$<expiration>$<userId>$<sessionId> - parts[0] (random salt) is ignored.
        String[] parts = token.split("\\$");
        if (parts.length >= 4) {
            Instant tokenTimestamp = parseTokenTimestamp(parts[1]);
            String tokenUser = parts[2];
            String tokenSessionId = parts[3];
            return isTokenValid(userId, tokenUser, tokenTimestamp, tokenSessionId);
        }

        return false;
    }

    private @Nullable String decryptXsrfToken(@NotNull String encryptedToken) {
        try {
            String tokenKey = XsrfTokenKeyStorage.INSTANCE.getTokenKey();
            return PasswordEncryptor.getInstance(tokenKey).decrypt(encryptedToken);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isTokenValid(@NotNull String userId, @NotNull String tokenUser, @NotNull Instant tokenTimestamp, @NotNull String tokenSessionId) {
        return Objects.equals(userId, tokenUser)
                && !tokenTimestamp.isBefore(Instant.now())
                && !tokenSessionId.isBlank()
                && Objects.equals(sessionId, tokenSessionId);
    }

    private Instant parseTokenTimestamp(@NotNull String timestampPart) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(timestampPart));
        } catch (NumberFormatException e) {
            return Instant.EPOCH;
        }
    }

    private @NotNull Subject getUserSubject(@NotNull String userId) throws AuthenticationFailedException {
        Subject currentSubject = securityService.getCurrentSubject();
        String currentUserId = securityService.getSubjectUser(currentSubject);
        if (Objects.equals(currentUserId, userId)) {
            return currentSubject;
        } else {
            throw new AuthenticationFailedException("Invalid user ID");
        }
    }
}
