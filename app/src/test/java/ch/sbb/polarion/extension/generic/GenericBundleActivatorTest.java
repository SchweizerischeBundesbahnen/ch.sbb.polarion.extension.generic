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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void runAsyncExecutesTaskOnDaemonThread() throws InterruptedException {
        CountDownLatch ran = new CountDownLatch(1);
        new RealSeamsActivator().runAsync(ran::countDown); // real daemon-thread implementation
        assertTrue(ran.await(5, TimeUnit.SECONDS), "runAsync should have executed the task");
    }

    @Test
    void readinessProbeStartsUninjected() {
        assertNull(new GenericBundleActivator.ReadinessProbe().contributions);
    }

    @Test
    void registrationWaitsUntilGuicePlatformReady() {
        try (MockedStatic<FormExtensionsRegistry> extensionsRegistryStatic = mockStatic(FormExtensionsRegistry.class)) {
            extensionsRegistryStatic.when(FormExtensionsRegistry::getInstance).thenReturn(registry);

            TestBundleActivator activator = new TestBundleActivator(Map.of("only", mock(IFormExtension.class)));
            activator.readyAfterPolls = 2; // not ready for the first two polls, then ready

            activator.registerExtensionsWhenReady(Map.of("only", mock(IFormExtension.class)));

            assertEquals(3, activator.readinessPolls); // polled until ready (2 misses + 1 hit)
            verify(registry, times(1)).registerExtension(eq("only"), any());
        }
    }

    @Test
    void restoresInterruptFlagAndRegistersWhenWaitInterrupted() throws InterruptedException {
        try (MockedStatic<FormExtensionsRegistry> extensionsRegistryStatic = mockStatic(FormExtensionsRegistry.class)) {
            extensionsRegistryStatic.when(FormExtensionsRegistry::getInstance).thenReturn(registry);

            CountDownLatch polling = new CountDownLatch(1);
            TestBundleActivator activator = new TestBundleActivator(Map.of()) {
                @Override
                protected boolean isGuicePlatformReady() {
                    polling.countDown();
                    return false;
                }
            };
            activator.timeoutMs = TimeUnit.MINUTES.toMillis(5);
            activator.pollMs = TimeUnit.MINUTES.toMillis(5); // long sleep so the wait is interrupted mid-flight

            Thread target = Thread.currentThread();
            Thread interrupter = new Thread(() -> {
                try {
                    polling.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                target.interrupt();
            });
            interrupter.setDaemon(true);
            interrupter.start();

            activator.registerExtensionsWhenReady(Map.of("only", mock(IFormExtension.class)));

            assertTrue(Thread.interrupted(), "interrupt flag should have been restored"); // also clears it
            verify(registry, times(1)).registerExtension(eq("only"), any());
        }
    }

    @Test
    void registersAnywayWhenPlatformNeverBecomesReady() {
        try (MockedStatic<FormExtensionsRegistry> extensionsRegistryStatic = mockStatic(FormExtensionsRegistry.class)) {
            extensionsRegistryStatic.when(FormExtensionsRegistry::getInstance).thenReturn(registry);

            TestBundleActivator activator = new TestBundleActivator(Map.of("only", mock(IFormExtension.class)));
            activator.readyAfterPolls = Integer.MAX_VALUE; // never ready
            activator.timeoutMs = 20;
            activator.pollMs = 5;

            activator.registerExtensionsWhenReady(Map.of("only", mock(IFormExtension.class)));

            verify(registry, times(1)).registerExtension(eq("only"), any()); // registered despite timeout
        }
    }

    private static class TestBundleActivator extends GenericBundleActivator {

        final Map<String, IFormExtension> extensions;
        int readyAfterPolls = 0;
        int readinessPolls = 0;
        long timeoutMs = TimeUnit.MINUTES.toMillis(5);
        long pollMs = 1;

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
        protected long getPlatformWaitTimeoutMs() {
            return timeoutMs;
        }

        @Override
        protected long getPlatformPollIntervalMs() {
            return pollMs;
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

    /** Overrides nothing beyond the abstract method, so the real runAsync/isGuicePlatformReady run. */
    private static class RealSeamsActivator extends GenericBundleActivator {

        @Override
        protected Map<String, IFormExtension> getExtensions() {
            return Map.of();
        }
    }
}
