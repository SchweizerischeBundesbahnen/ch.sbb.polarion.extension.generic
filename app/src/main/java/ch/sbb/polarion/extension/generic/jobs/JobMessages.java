package ch.sbb.polarion.extension.generic.jobs;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * The texts a job reports as its error message when it did not end with a result of its own.
 */
@UtilityClass
public class JobMessages {

    public static final String CANCELLED_BY_USER = "Cancelled by user";
    public static final String STOPPED = "Stopped because the extension stopped";
    public static final String NO_RESULT = "The job produced no result";

    public static @NotNull String timeout(int timeoutInMinutes) {
        return "Timeout after %d min".formatted(timeoutInMinutes);
    }
}
