package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.jar.Attributes;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

class ContextUtilsTest {

    public static Stream<Arguments> testGetConfigurationPropertiesPrefixProvider() {
        // if expected is null means that an IllegalStateException is expected
        return Stream.of(
                // configurationPropertiesPrefix is not provided
                Arguments.of(null, null, null),
                Arguments.of(null, "", null),
                Arguments.of(null, "extension-name", ContextUtils.CH_SBB_POLARION_EXTENSION + "extension-name."),
                // configurationPropertiesPrefix is empty
                Arguments.of("", null, null),
                Arguments.of("", "", null),
                Arguments.of("", "extension-name", ContextUtils.CH_SBB_POLARION_EXTENSION + "extension-name."),
                // configurationPropertiesPrefix is provided without ending dot
                Arguments.of("com.name.test.without.dot", null, "com.name.test.without.dot."),
                Arguments.of("com.name.test.without.dot", "", "com.name.test.without.dot."),
                Arguments.of("com.name.test.without.dot", "extension-name", "com.name.test.without.dot."),
                // configurationPropertiesPrefix is provided and ends with a dot
                Arguments.of("com.name.test.with.dot.", null, "com.name.test.with.dot."),
                Arguments.of("com.name.test.with.dot.", "", "com.name.test.with.dot."),
                Arguments.of("com.name.test.with.dot.", "extension-name", "com.name.test.with.dot.")
        );
    }

    @ParameterizedTest
    @MethodSource("testGetConfigurationPropertiesPrefixProvider")
    void testGetConfigurationPropertiesPrefix(String configurationPropertiesPrefix, String extensionContext, String expected) {
        try (MockedStatic<ManifestUtils> manifestUtilsMockedStatic = mockStatic(ManifestUtils.class)) {

            Attributes attributes = new Attributes();
            attributes.put(new Attributes.Name(ContextUtils.CONFIGURATION_PROPERTIES_PREFIX), configurationPropertiesPrefix);
            attributes.put(new Attributes.Name(ContextUtils.EXTENSION_CONTEXT), extensionContext);

            manifestUtilsMockedStatic.when(ManifestUtils::getManifestAttributes).thenReturn(attributes);

            if (expected == null) {
                assertThrows(IllegalStateException.class, ContextUtils::getConfigurationPropertiesPrefix);
            } else {
                assertEquals(expected, ContextUtils.getConfigurationPropertiesPrefix());
            }
        }
    }
}
