package ch.sbb.polarion.extension.generic.properties;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Properties;

public interface IExtensionConfiguration {

    boolean isDebug();

    @NotNull Properties getProperties();

    @NotNull List<String> getSupportedProperties();

}
