package ch.sbb.polarion.extension.generic.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@UtilityClass
public class ObjectUtils {

    @NotNull
    public static <T> T requireNotNull(@Nullable T object) {
        return requireNotNull(object, "object is null");
    }

    @NotNull
    public static <T> T requireNotNull(@Nullable T object, @NotNull String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        } else {
            return Objects.requireNonNull(object);
        }
    }
}
