package ch.sbb.polarion.extension.generic.properties;

import ch.sbb.polarion.extension.generic.util.ContextUtils;
import com.polarion.core.config.impl.SystemValueReader;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Properties;

@Getter
public class ExtensionConfiguration implements IExtensionConfiguration {

    private static final String DEBUG = "debug";

    @NotNull
    protected final String propertyPrefix;

    @Override
    public boolean isDebug() {
        return SystemValueReader.getInstance().readBoolean(propertyPrefix + DEBUG, false);
    }

    protected ExtensionConfiguration() {
        this.propertyPrefix = ContextUtils.getConfigurationPropertiesPrefix();
    }

    @Override
    public final @NotNull Properties getProperties() {
        List<String> supportedProperties = getSupportedProperties();
        Properties properties = new Properties(supportedProperties.size());
        ExtensionConfiguration configuration = CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();

        for (String supportedProperty : supportedProperties) {
            String key = getPropertyPrefix() + supportedProperty;
            String value = GetterFinder.getValue(configuration, supportedProperty);
            if (value != null) {
                properties.setProperty(key, value);
            }
        }

        return properties;
    }

    @Override
    public @NotNull List<String> getSupportedProperties() {
        return List.of(DEBUG);
    }

}
