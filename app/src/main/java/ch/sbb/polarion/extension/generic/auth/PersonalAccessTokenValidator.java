package ch.sbb.polarion.extension.generic.auth;

import com.polarion.platform.security.AuthenticationFailedException;
import com.polarion.platform.security.login.AccessToken;
import com.polarion.platform.security.login.IToken;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;

public class PersonalAccessTokenValidator extends AbstractAuthValidator {

    @Override
    public @NotNull Subject validate() throws AuthenticationFailedException {
        final IToken<AccessToken> accessToken = AccessToken.token(secret);

        return securityService.login()
                .from("REST")
                .authenticator(AccessToken.id())
                .with(accessToken)
                .perform();
    }
}
