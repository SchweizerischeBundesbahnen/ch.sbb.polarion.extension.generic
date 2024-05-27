package ch.sbb.polarion.extension.generic.properties;

import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Setter
public class CurrentExtensionConfiguration {

    private ExtensionConfiguration extensionConfiguration;

    public @NotNull ExtensionConfiguration getExtensionConfiguration() {
        if (extensionConfiguration == null) {
            extensionConfiguration = new ExtensionConfiguration();
        }
        return extensionConfiguration;
    }

    public static CurrentExtensionConfiguration getInstance() {
        return ExtensionsConfigurationHolder.INSTANCE;
    }

    private static class ExtensionsConfigurationHolder {
        private static final CurrentExtensionConfiguration INSTANCE = new CurrentExtensionConfiguration();
    }
}
