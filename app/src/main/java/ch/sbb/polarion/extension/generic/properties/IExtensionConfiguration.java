package ch.sbb.polarion.extension.generic.properties;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IExtensionConfiguration {

    @NotNull ConfigurationProperties getProperties();

    @NotNull List<String> getSupportedProperties();

}
