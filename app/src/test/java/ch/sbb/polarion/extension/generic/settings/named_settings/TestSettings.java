package ch.sbb.polarion.extension.generic.settings.named_settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import org.jetbrains.annotations.NotNull;

public class TestSettings extends GenericNamedSettings<TestModel> {
    public TestSettings() {
        super("Test");
    }

    public TestSettings(SettingsService settingsService) {
        super("Test", settingsService);
    }

    @Override
    public @NotNull TestModel defaultValues() {
        TestModel testModel = new TestModel();
        testModel.setName(DEFAULT_NAME);
        return testModel;
    }

    @Override
    public @NotNull String currentBundleTimestamp() {
        return "Something";
    }
}
