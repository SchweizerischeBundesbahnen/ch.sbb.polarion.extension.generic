package ch.sbb.polarion.extension.generic.test_extensions;

import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
public class BundleJarsPrioritizingRunnableMockExtension implements BeforeEachCallback, AfterEachCallback {

    private MockedStatic<BundleJarsPrioritizingRunnable> prioritizingRunnableMockedStatic;

    @Override
    @SuppressWarnings("unchecked")
    public void beforeEach(@NonNull ExtensionContext context) {
        prioritizingRunnableMockedStatic = mockStatic(BundleJarsPrioritizingRunnable.class, RETURNS_DEEP_STUBS);
        when(BundleJarsPrioritizingRunnable.execute(any(), any()))
                .thenAnswer(invocation -> (((Class<? extends BundleJarsPrioritizingRunnable>) invocation.getArgument(0)).getDeclaredConstructor().newInstance()).run(invocation.getArgument(1)));
        when(BundleJarsPrioritizingRunnable.execute(any(), any(), anyBoolean()))
                .thenAnswer(invocation -> (((Class<? extends BundleJarsPrioritizingRunnable>) invocation.getArgument(0)).getDeclaredConstructor().newInstance()).run(invocation.getArgument(1)));
        when(BundleJarsPrioritizingRunnable.executeCached(any(), any()))
                .thenAnswer(invocation -> (((Class<? extends BundleJarsPrioritizingRunnable>) invocation.getArgument(0)).getDeclaredConstructor().newInstance()).run(invocation.getArgument(1)));
        when(BundleJarsPrioritizingRunnable.executeCached(any(), any(), anyBoolean()))
                .thenAnswer(invocation -> (((Class<? extends BundleJarsPrioritizingRunnable>) invocation.getArgument(0)).getDeclaredConstructor().newInstance()).run(invocation.getArgument(1)));
    }

    @Override
    public void afterEach(@NonNull ExtensionContext context) {
        prioritizingRunnableMockedStatic.close();
    }

}
