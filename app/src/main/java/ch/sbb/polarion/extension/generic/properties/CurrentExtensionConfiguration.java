package ch.sbb.polarion.extension.generic.properties;

import ch.sbb.polarion.extension.generic.util.ContextUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CurrentExtensionConfiguration {

    private ExtensionConfiguration extensionConfiguration;

    @SneakyThrows
    public synchronized @NotNull ExtensionConfiguration getExtensionConfiguration() {
        if (extensionConfiguration == null) {
            Set<Class<? extends ExtensionConfiguration>> configurationTypes = ContextUtils.findSubTypes(ExtensionConfiguration.class);
            if (configurationTypes.isEmpty()) {
                extensionConfiguration = new ExtensionConfiguration();
            } else if (configurationTypes.size() == 1) {
                extensionConfiguration = configurationTypes.iterator().next().getConstructor().newInstance();
            } else {
                throw new IllegalStateException("Multiple extension configurations found");
            }
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
