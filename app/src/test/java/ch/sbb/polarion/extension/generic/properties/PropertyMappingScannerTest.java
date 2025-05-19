package ch.sbb.polarion.extension.generic.properties;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMapping;
import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMappingDefaultValue;
import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMappingDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
class PropertyMappingScannerTest {
    private static PropertyMappingScanner<?> scanner;

    @BeforeEach
    void setUp() {
        scanner = new PropertyMappingScanner<>(new TestConfig());
    }

    private static Stream<Arguments> existingPropertyValues() {
        return Stream.of(
                Arguments.of("test.mapping.key", "mapping value", (Function<String, String>) key -> scanner.getValue(key)),
                Arguments.of("test.mapping.key", "default test value", (Function<String, String>) key -> scanner.getDefaultValue(key)),
                Arguments.of("test.mapping.key", "test description", (Function<String, String>) key -> scanner.getDescription(key)),
                Arguments.of("test.mapping.key.parent", "true", (Function<String, String>) key -> scanner.getValue(key)),
                Arguments.of("test.mapping.key.parent", "false", (Function<String, String>) key -> scanner.getDefaultValue(key)),
                Arguments.of("test.mapping.key.parent", "test parent description", (Function<String, String>) key -> scanner.getDescription(key)),
                Arguments.of("testKey", "method convention value", (Function<String, String>) key -> scanner.getValue(key)),
                Arguments.of("testKey", "method convention default value", (Function<String, String>) key -> scanner.getDefaultValue(key)),
                Arguments.of("testKey", "method convention description", (Function<String, String>) key -> scanner.getDescription(key)),
                Arguments.of("testParentKey", "parent annotation convention value", (Function<String, String>) key -> scanner.getValue(key)),
                Arguments.of("testParentKey", "parent method convention default value", (Function<String, String>) key -> scanner.getDefaultValue(key)),
                Arguments.of("testParentKey", "parent method convention description", (Function<String, String>) key -> scanner.getDescription(key))
        );
    }

    @ParameterizedTest
    @MethodSource("existingPropertyValues")
    void shouldRetrieveAnnotationValues(String key, String expectedValue, Function<String, String> getMappingValue) {
        assertThat(getMappingValue.apply(key)).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> nonExistentPropertyKeys() {
        return Stream.of(
                Arguments.of("non.existent.key", (Function<String, String>) key -> scanner.getValue(key)),
                Arguments.of("non.existent.key", (Function<String, String>) key -> scanner.getDefaultValue(key)),
                Arguments.of("non.existent.key", (Function<String, String>) key -> scanner.getDescription(key))
        );
    }

    @ParameterizedTest
    @MethodSource("nonExistentPropertyKeys")
    void shouldReturnNullForNonExistentValues(String key, Function<String, String> getMappingValue) {
        assertThat(getMappingValue.apply(key)).isNull();
    }

    @Test
    void shouldHandleBlankKey() {
        TestConfigBlankKey testConfigBlankKey = new TestConfigBlankKey();
        assertThatThrownBy(() -> new PropertyMappingScanner<>(testConfigBlankKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property key cannot be empty");
    }

    @SuppressWarnings("unused")
    private static class TestConfig extends TestConfigParent {
        @PropertyMapping("test.mapping.key")
        public String anyMethodNameValue() {
            return "mapping value";
        }

        @PropertyMappingDefaultValue("test.mapping.key")
        public String anyMethodNameDefaultValue() {
            return "default test value";
        }

        @PropertyMappingDescription("test.mapping.key")
        public String anyMethodNameDescription() {
            return "test description";
        }

        // old method naming convention
        public String getTestKey() {
            return "method convention value";
        }

        public String getTestKeyDefaultValue() {
            return "method convention default value";
        }

        public String getTestKeyDescription() {
            return "method convention description";
        }
    }

    @SuppressWarnings("unused")
    private static class TestConfigParent extends ExtensionConfiguration {
        @PropertyMapping("test.mapping.key.parent")
        public boolean anyMethodNameValueParent() {
            return true;
        }

        @PropertyMappingDefaultValue("test.mapping.key.parent")
        public boolean anyMethodNameDefaultValueParent() {
            return false;
        }

        @PropertyMappingDescription("test.mapping.key.parent")
        public String anyMethodNameDescriptionParent() {
            return "test parent description";
        }

        // mixed new annotation and old method naming convention
        @PropertyMapping("testParentKey")
        public String anyMethodNameInParent() {
            return "parent annotation convention value";
        }

        public String getTestParentKeyDefaultValue() {
            return "parent method convention default value";
        }

        public String getTestParentKeyDescription() {
            return "parent method convention description";
        }
    }

    @SuppressWarnings("unused")
    private static class TestConfigBlankKey extends ExtensionConfiguration {
        @PropertyMapping("")
        public String getMappingValue() {
            return "mapping value";
        }
    }
}
