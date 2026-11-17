package ch.sbb.polarion.extension.generic.service;

import ch.sbb.polarion.extension.generic.exception.BaselineExecutionException;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

@UtilityClass
public class PolarionBaselineExecutor {

    public <T> T executeInBaseline(@Nullable String baselineRevision, @NotNull ReadOnlyTransaction transaction, @NotNull Callable<T> callable) {
        if (baselineRevision == null) {
            return CallableInvoker.call(callable, BaselineExecutionException::new);
        } else {
            return transaction.utils().executeInBaseline(baselineRevision, () -> CallableInvoker.call(callable, BaselineExecutionException::new));
        }
    }

}
