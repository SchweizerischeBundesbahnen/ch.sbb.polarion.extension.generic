package ch.sbb.polarion.extension.generic;

import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.impl.FormExtensionsRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericBundleActivatorTest {

    @Mock
    private FormExtensionsRegistry registry;

    @Test
    void testBundleActivator() {
        try (MockedStatic<FormExtensionsRegistry> extensionsRegistryStatic = mockStatic(FormExtensionsRegistry.class)) {
            extensionsRegistryStatic.when(FormExtensionsRegistry::getInstance).thenReturn(registry);

            BundleContext bundleContext = mock(BundleContext.class);

            new SimplestTestBundleActivator().start(bundleContext); // Force call onStart() in GenericBundleActivator
            verify(registry, times(0)).registerExtension(anyString(), any());

            new TestBundleActivator(Map.of()).start(bundleContext);
            verify(registry, times(0)).registerExtension(anyString(), any());
            verify(bundleContext, times(1)).getProperty("test_property");

            new TestBundleActivator(Map.of("first", mock(IFormExtension.class), "second", mock(IFormExtension.class))).start(bundleContext);
            verify(registry, times(1)).registerExtension(eq("first"), any());
            verify(registry, times(1)).registerExtension(eq("second"), any());
        }
    }

    @Test
    void registrationWaitsUntilGuicePlatformReady() {
        try (MockedStatic<FormExtensionsRegistry> extensionsRegistryStatic = mockStatic(FormExtensionsRegistry.class)) {
            extensionsRegistryStatic.when(FormExtensionsRegistry::getInstance).thenReturn(registry);

            // Reports "not ready" for the first two polls, then ready.
            TestBundleActivator activator = new TestBundleActivator(Map.of("only", mock(IFormExtension.class)));
            activator.readyAfterPolls = 2;

            activator.registerExtensionsWhenReady(Map.of("only", mock(IFormExtension.class)));

            assertEquals(3, activator.readinessPolls); // polled until ready (2 misses + 1 hit)
            verify(registry, times(1)).registerExtension(eq("only"), any());
        }
    }

    private static class TestBundleActivator extends GenericBundleActivator {

        final Map<String, IFormExtension> extensions;
        int readyAfterPolls = 0;
        int readinessPolls = 0;

        public TestBundleActivator(Map<String, IFormExtension> extensions) {
            this.extensions = extensions;
        }

        @Override
        protected Map<String, IFormExtension> getExtensions() {
            return extensions;
        }

        @Override
        public void onStart(BundleContext context) {
            context.getProperty("test_property");
        }

        @Override
        protected boolean isGuicePlatformReady() {
            return readinessPolls++ >= readyAfterPolls;
        }

        @Override
        protected void runAsync(Runnable task) {
            task.run(); // run synchronously in tests
        }
    }

    private static class SimplestTestBundleActivator extends GenericBundleActivator {

        @Override
        protected Map<String, IFormExtension> getExtensions() {
            return Map.of();
        }

        @Override
        protected boolean isGuicePlatformReady() {
            return true;
        }

        @Override
        protected void runAsync(Runnable task) {
            task.run(); // run synchronously in tests
        }
    }
}
