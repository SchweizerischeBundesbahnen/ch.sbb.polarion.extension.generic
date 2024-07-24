package ch.sbb.polarion.extension.generic.auth;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class ValidatorFactory {

    public static @NotNull AuthValidator getValidator(@NotNull ValidatorType type) {
        return switch (type) {
            case PERSONAL_ACCESS_TOKEN -> new PersonalAccessTokenValidator();
            case XSRF_TOKEN -> new XsrfTokenValidator();
        };
    }
}
