package ch.sbb.polarion.extension.generic.properties;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.context.SystemPropertiesExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class, SystemPropertiesExtension.class})
@CurrentContextConfig(value = "mega-test-extension", extensionConfiguration = ExtensionConfigurationTest.MegaTestExtensionConfiguration.class)
class ExtensionConfigurationTest {

    @Test
    void testGetConfigurationProperties() {
        ConfigurationProperties configurationProperties = MegaTestExtensionConfiguration.getInstance().getConfigurationProperties();

        assertEquals(2, configurationProperties.size());
        assertEquals("testConfigurationPropertyValue", configurationProperties.getProperty("ch.sbb.polarion.extension.mega_test_extension.testConfigurationProperty").value());
        assertEquals("false", configurationProperties.getProperty("ch.sbb.polarion.extension.mega_test_extension.debug").value());
    }

    @Test
    void testGetSupportedProperties() {
        List<String> supportedProperties = MegaTestExtensionConfiguration.getInstance().getSupportedProperties();

        assertEquals(2, supportedProperties.size());
        assertThat(supportedProperties).containsExactlyInAnyOrderElementsOf(List.of("testConfigurationProperty", "debug"));
    }

    @SystemPropertiesExtension.RuntimeProperties({
            "ch.sbb.polarion.extension.mega_test_extension.testConfigurationProperty=configuredConfigurationPropertyValue"
    })
    @Test
    void testConfiguredExtensionProperties() {
        List<String> configuredExtensionProperties = MegaTestExtensionConfiguration.getInstance().getConfiguredExtensionProperties();

        assertEquals(1, configuredExtensionProperties.size());
        assertThat(configuredExtensionProperties).containsExactlyInAnyOrderElementsOf(List.of("testConfigurationProperty"));
    }

    @SystemPropertiesExtension.RuntimeProperties({
            "ch.sbb.polarion.extension.mega_test_extension.obsoleteTestConfigurationProperty=configuredConfigurationPropertyValue"
    })
    @Test
    void testObsoleteProperties() {
        List<String> configuredExtensionProperties = MegaTestExtensionConfiguration.getInstance().getObsoleteProperties();

        assertEquals(1, configuredExtensionProperties.size());
        assertThat(configuredExtensionProperties).containsExactlyInAnyOrderElementsOf(List.of("obsoleteTestConfigurationProperty"));
    }

    @SystemPropertiesExtension.RuntimeProperties({
            "ch.sbb.polarion.extension.mega_test_extension.obsoleteTestConfigurationProperty=configuredConfigurationPropertyValue"
    })
    @Test
    void testObsoleteConfigurationProperties() {
        ConfigurationProperties obsoleteConfigurationProperties = MegaTestExtensionConfiguration.getInstance().getObsoleteConfigurationProperties();

        assertEquals(1, obsoleteConfigurationProperties.size());
        assertEquals("configuredConfigurationPropertyValue", obsoleteConfigurationProperties.getProperty("ch.sbb.polarion.extension.mega_test_extension.obsoleteTestConfigurationProperty").value());
    }

    @SuppressWarnings("unused")
    public static final class MegaTestExtensionConfiguration extends ExtensionConfiguration {

        private static final String TEST_PROPERTY_KEY = "testConfigurationProperty";

        public String getTestConfigurationProperty() {
            return "testConfigurationPropertyValue";
        }

        @Override
        public @NotNull List<String> getSupportedProperties() {
            List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
            supportedProperties.add(TEST_PROPERTY_KEY);
            return supportedProperties;
        }

        public static MegaTestExtensionConfiguration getInstance() {
            return (MegaTestExtensionConfiguration) CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();
        }
    }

}
