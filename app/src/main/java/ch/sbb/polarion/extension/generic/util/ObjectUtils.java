package ch.sbb.polarion.extension.generic.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@UtilityClass
public class ObjectUtils {

    /**
     * Utility method to ensure that an object is not null.
     * If the object is null, an IllegalStateException is thrown with a default message.
     *
     * @param object the object to check
     * @param <T>    the type of the object
     * @return the non-null object
     * @throws IllegalStateException if the object is null
     */
    public static @NotNull <T> T requireNotNull(@Nullable T object) {
        return requireNotNull(object, "object is null");
    }

    /**
     * Utility method to ensure that an object is not null.
     * If the object is null, an IllegalStateException is thrown with a custom message.
     *
     * @param object  the object to check
     * @param message the message for the exception if the object is null
     * @param <T>     the type of the object
     * @return the non-null object
     * @throws IllegalStateException if the object is null
     */
    public static @NotNull <T> T requireNotNull(@Nullable T object, @NotNull String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        } else {
            return Objects.requireNonNull(object);
        }
    }

}
