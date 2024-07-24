package ch.sbb.polarion.extension.generic.auth;

import com.polarion.core.util.security.PasswordEncryptor;
import com.polarion.platform.internal.XsrfTokenKeyStorage;
import com.polarion.platform.internal.security.UserPrincipal;
import com.polarion.platform.security.AuthenticationFailedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.util.Date;
import java.util.Objects;

public class XsrfTokenValidator extends AbstractAuthValidator {

    @Override
    public @NotNull Subject validate() throws AuthenticationFailedException {
        if (isXsrfTokenValid(userId, secret)) {
            return createAuthenticatedSubject(userId);
        } else {
            throw new AuthenticationFailedException("Invalid XSRF token");
        }
    }

    private boolean isXsrfTokenValid(@NotNull String userId, @NotNull String encryptedXsrfToken) {
        String token = decryptXsrfToken(encryptedXsrfToken);
        if (token == null) {
            return false;
        }

        String[] parts = token.split("\\$");
        if (parts.length >= 2) {
            Date tokenTimestamp = parseTokenTimestamp(parts[0]);
            String tokenUser = parts[1];
            return isTokenValid(userId, tokenUser, tokenTimestamp);
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

    private boolean isTokenValid(@NotNull String userId, @NotNull String tokenUser, @NotNull Date tokenTimestamp) {
        return Objects.equals(userId, tokenUser) && !tokenTimestamp.before(new Date());
    }

    private Date parseTokenTimestamp(@NotNull String timestampPart) {
        try {
            return new Date(Long.parseLong(timestampPart));
        } catch (NumberFormatException e) {
            return new Date(0);
        }
    }

    private @NotNull Subject createAuthenticatedSubject(@NotNull String userId) {
        Subject subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal(userId));
        subject.setReadOnly();
        return subject;
    }
}