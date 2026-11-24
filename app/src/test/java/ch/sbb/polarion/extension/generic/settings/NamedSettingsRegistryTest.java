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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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

    private static class OtherSettings extends GenericNamedSettings<TestModel> {

        OtherSettings(SettingsService settingsService) {
            super("Other", settingsService);
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
