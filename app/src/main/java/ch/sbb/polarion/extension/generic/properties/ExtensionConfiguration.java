package ch.sbb.polarion.extension.generic.properties;

import ch.sbb.polarion.extension.generic.util.ContextUtils;
import com.polarion.core.config.impl.SystemValueReader;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class ExtensionConfiguration implements IExtensionConfiguration {

    private static final String DEBUG = "debug";
    public static final boolean DEBUG_DEFAULT_VALUE = false;
    private static final String DEBUG_DESCRIPTION = "Enable debug mode for current extension";

    @NotNull
    protected final String propertyPrefix;

    @SuppressWarnings("unused")
    public boolean isDebug() {
        return SystemValueReader.getInstance().readBoolean(propertyPrefix + DEBUG, DEBUG_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    public String getDebugDescription() {
        return DEBUG_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    public String getDebugDefaultValue() {
        return String.valueOf(DEBUG_DEFAULT_VALUE);
    }

    protected ExtensionConfiguration() {
        this.propertyPrefix = ContextUtils.getConfigurationPropertiesPrefix();
    }

    @Override
    public final @NotNull ConfigurationProperties getProperties() {
        List<String> supportedProperties = getSupportedProperties();
        ConfigurationProperties configurationProperties = new ConfigurationProperties(supportedProperties.size());
        ExtensionConfiguration extensionConfiguration = CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();

        for (String supportedProperty : supportedProperties) {
            @NotNull String key = getPropertyPrefix() + supportedProperty;
            @Nullable String value = GetterFinder.getValue(extensionConfiguration, supportedProperty);
            @Nullable String defaultValue = GetterFinder.getDefaultValue(extensionConfiguration, supportedProperty);
            @Nullable String description = GetterFinder.getDescription(extensionConfiguration, supportedProperty);
            if (value != null) {
                configurationProperties.setProperty(key, new ConfigurationProperties.Value(value, defaultValue, description));
            }
        }

        return configurationProperties;
    }

    @Override
    public @NotNull List<String> getSupportedProperties() {
        return List.of(DEBUG);
    }

}
