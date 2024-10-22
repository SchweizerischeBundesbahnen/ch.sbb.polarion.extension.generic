package ch.sbb.polarion.extension.generic.properties;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IExtensionConfiguration {

    @NotNull ConfigurationProperties getConfigurationProperties();

    @NotNull List<String> getSupportedProperties();

    @NotNull List<String> getObsoleteProperties();

    @NotNull ConfigurationProperties getObsoleteConfigurationProperties();
}
