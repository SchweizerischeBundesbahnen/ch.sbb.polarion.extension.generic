package ch.sbb.polarion.extension.generic.service;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.function.BiFunction;

/**
 * Calls a {@link Callable} where a checked exception cannot be declared, as inside the executors of this package.
 */
@UtilityClass
class CallableInvoker {

    private static final String ERROR_MESSAGE = "Error during callable execution";

    /**
     * @param toUnchecked makes the exception a checked one is thrown as, of its message and the checked one as its cause
     * @return what the callable returns; an unchecked exception of it is thrown as it is
     */
    <T> T call(@NotNull Callable<T> callable, @NotNull BiFunction<String, Throwable, ? extends RuntimeException> toUnchecked) {
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw toUnchecked.apply(ERROR_MESSAGE, e);
        }
    }

}
