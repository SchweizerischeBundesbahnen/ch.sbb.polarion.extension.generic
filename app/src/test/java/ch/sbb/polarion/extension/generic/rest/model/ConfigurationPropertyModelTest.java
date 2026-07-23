package ch.sbb.polarion.extension.generic.rest.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationPropertyModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testNoArgsConstructorAndSetters() {
        ConfigurationPropertyModel model = new ConfigurationPropertyModel();
        assertNull(model.getKey());
        assertNull(model.getValue());
        assertNull(model.getDefaultValue());
        assertNull(model.getDescription());

        model.setKey("some.key");
        model.setValue("value");
        model.setDefaultValue("default");
        model.setDescription("description");

        assertEquals("some.key", model.getKey());
        assertEquals("value", model.getValue());
        assertEquals("default", model.getDefaultValue());
        assertEquals("description", model.getDescription());
    }

    @Test
    void testAllArgsConstructor() {
        ConfigurationPropertyModel model = new ConfigurationPropertyModel("some.key", "value", "default", "description");

        assertEquals("some.key", model.getKey());
        assertEquals("value", model.getValue());
        assertEquals("default", model.getDefaultValue());
        assertEquals("description", model.getDescription());
    }

    @Test
    void testBuilder() {
        ConfigurationPropertyModel model = ConfigurationPropertyModel.builder()
                .key("some.key")
                .value("value")
                .defaultValue("default")
                .description("description")
                .build();

        assertEquals("some.key", model.getKey());
        assertEquals("value", model.getValue());
        assertEquals("default", model.getDefaultValue());
        assertEquals("description", model.getDescription());
    }

    @Test
    void testEqualsAndHashCode() {
        ConfigurationPropertyModel first = ConfigurationPropertyModel.builder()
                .key("some.key")
                .value("value")
                .defaultValue("default")
                .description("description")
                .build();
        ConfigurationPropertyModel second = ConfigurationPropertyModel.builder()
                .key("some.key")
                .value("value")
                .defaultValue("default")
                .description("description")
                .build();
        ConfigurationPropertyModel different = ConfigurationPropertyModel.builder()
                .key("other.key")
                .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

        assertNotEquals(first, different);
        assertNotEquals(null, first);
        assertNotEquals(new Object(), first);
    }

    @Test
    void testToString() {
        ConfigurationPropertyModel model = ConfigurationPropertyModel.builder()
                .key("some.key")
                .value("value")
                .defaultValue("default")
                .description("description")
                .build();

        String toString = model.toString();
        assertTrue(toString.contains("some.key"));
        assertTrue(toString.contains("value"));
        assertTrue(toString.contains("default"));
        assertTrue(toString.contains("description"));
    }

    @Test
    void testJsonSerializationOmitsNullValues() throws Exception {
        ConfigurationPropertyModel model = ConfigurationPropertyModel.builder()
                .key("some.key")
                .value("value")
                .build();

        String json = objectMapper.writeValueAsString(model);

        assertTrue(json.contains("\"key\":\"some.key\""));
        assertTrue(json.contains("\"value\":\"value\""));
        assertFalse(json.contains("defaultValue"));
        assertFalse(json.contains("description"));
    }

    @Test
    void testJsonSerializationIncludesNonNullValues() throws Exception {
        ConfigurationPropertyModel model = ConfigurationPropertyModel.builder()
                .key("some.key")
                .value("value")
                .defaultValue("default")
                .description("description")
                .build();

        String json = objectMapper.writeValueAsString(model);

        assertTrue(json.contains("\"defaultValue\":\"default\""));
        assertTrue(json.contains("\"description\":\"description\""));
    }
}
