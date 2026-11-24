package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.named_settings.TestModel;
import ch.sbb.polarion.extension.generic.settings.named_settings.TestSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings("rawtypes")
@ExtendWith(CurrentContextExtension.class)
@CurrentContextConfig("registry-test-extension")
class NamedSettingsRegistryTest {

    private final SettingsService settingsService = mock(SettingsService.class);

    @BeforeEach
    @AfterEach
    void clearRegistry() {
        NamedSettingsRegistry.INSTANCE.clear();
    }

    @Test
    void registerKeepsAnEqualInstance() {
        TestSettings first = new TestSettings(settingsService);
        TestSettings second = new TestSettings(settingsService);

        NamedSettingsRegistry.INSTANCE.register(List.of(first)).register(List.of(second));

        assertEquals(1, NamedSettingsRegistry.INSTANCE.getAll().size());
        assertSame(first, NamedSettingsRegistry.INSTANCE.getByFeatureName("Test"));
    }

    @Test
    void replaceAllReplacesAnEqualInstance() {
        TestSettings first = new TestSettings(settingsService);
        TestSettings second = new TestSettings(settingsService);
        NamedSettingsRegistry.INSTANCE.register(List.of(first));

        NamedSettingsRegistry.INSTANCE.replaceAll(List.of(second));

        assertEquals(1, NamedSettingsRegistry.INSTANCE.getAll().size());
        assertSame(second, NamedSettingsRegistry.INSTANCE.getByFeatureName("Test"));
    }

    @Test
    void replaceAllRemovesSettingsMissingInTheNewList() {
        OtherSettings other = new OtherSettings(settingsService);
        NamedSettingsRegistry.INSTANCE.register(List.of(new TestSettings(settingsService)));

        // a list of a concrete settings type must be accepted as well
        List<OtherSettings> replacement = List.of(other);
        NamedSettingsRegistry.INSTANCE.replaceAll(replacement);

        assertSame(other, NamedSettingsRegistry.INSTANCE.getByFeatureName("Other"));
        assertThrows(ObjectNotFoundException.class, () -> NamedSettingsRegistry.INSTANCE.getByFeatureName("Test"));
    }

    @Test
    void clearRemovesAllSettings() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new TestSettings(settingsService), new OtherSettings(settingsService)));
        assertEquals(2, NamedSettingsRegistry.INSTANCE.getAll().size());

        NamedSettingsRegistry.INSTANCE.clear();

        assertTrue(NamedSettingsRegistry.INSTANCE.getAll().isEmpty());
    }

    @Test
    void getAllReturnsReadOnlySet() {
        TestSettings registered = new TestSettings(settingsService);
        OtherSettings other = new OtherSettings(settingsService);
        Set<GenericNamedSettings> emptySet = NamedSettingsRegistry.INSTANCE.getAll();
        Set<GenericNamedSettings> registeredSet = NamedSettingsRegistry.INSTANCE.register(List.of(registered)).getAll();
        Set<GenericNamedSettings> replacedSet = NamedSettingsRegistry.INSTANCE.replaceAll(List.of(registered)).getAll();

        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(other));
        assertThrows(UnsupportedOperationException.class, () -> registeredSet.add(other));
        assertThrows(UnsupportedOperationException.class, () -> replacedSet.add(other));
        assertThrows(UnsupportedOperationException.class, emptySet::clear);
        assertThrows(UnsupportedOperationException.class, registeredSet::clear);
        assertThrows(UnsupportedOperationException.class, replacedSet::clear);
        assertEquals(Set.of(registered), NamedSettingsRegistry.INSTANCE.getAll());
    }

    @Test
    void getAllReturnsSnapshot() {
        TestSettings registered = new TestSettings(settingsService);
        NamedSettingsRegistry.INSTANCE.register(List.of(registered));
        Set<GenericNamedSettings> snapshot = NamedSettingsRegistry.INSTANCE.getAll();

        NamedSettingsRegistry.INSTANCE.register(List.of(new OtherSettings(settingsService)));
        assertEquals(Set.of(registered), snapshot);

        NamedSettingsRegistry.INSTANCE.clear();
        assertEquals(Set.of(registered), snapshot);
    }

    @Test
    void concurrentRegistrationsAreNotLost() throws Exception {
        int threads = 8;
        int registrationsPerThread = 100;
        CyclicBarrier start = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                // create the settings here, as the extension context is available in the test thread only
                List<OtherSettings> threadSettings = new ArrayList<>();
                for (int i = 0; i < registrationsPerThread; i++) {
                    threadSettings.add(new OtherSettings("Other-%d-%d".formatted(t, i), settingsService));
                }
                futures.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    threadSettings.forEach(settings -> NamedSettingsRegistry.INSTANCE.register(List.of(settings)));
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(threads * registrationsPerThread, NamedSettingsRegistry.INSTANCE.getAll().size());
    }

    private static class OtherSettings extends GenericNamedSettings<TestModel> {

        OtherSettings(SettingsService settingsService) {
            this("Other", settingsService);
        }

        OtherSettings(String featureName, SettingsService settingsService) {
            super(featureName, settingsService);
        }

        @Override
        public @NotNull TestModel defaultValues() {
            return new TestModel();
        }

        @Override
        public @NotNull String currentBundleTimestamp() {
            return "Something";
        }
    }
}
