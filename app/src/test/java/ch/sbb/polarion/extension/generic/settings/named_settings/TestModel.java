package ch.sbb.polarion.extension.generic.settings.named_settings;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;

public class TestModel extends SettingsModel {

    @Override
    protected String serializeModelData() {
        return "Dummy implementation";
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        // Dummy implementation
    }
}
