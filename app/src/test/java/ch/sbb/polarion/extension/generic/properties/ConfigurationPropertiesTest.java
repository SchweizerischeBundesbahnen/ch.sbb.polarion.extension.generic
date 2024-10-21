package ch.sbb.polarion.extension.generic.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigurationPropertiesTest {

    private ConfigurationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ConfigurationProperties();
    }

    @Test
    void testSetPropertyWithDescriptionAndDefaultValue() {
        properties.setProperty("username", new ConfigurationProperties.Value("admin", "root", "User for admin tasks"));

        ConfigurationProperties.Value value = properties.getProperty("username");
        assertNotNull(value);
        assertEquals("admin", value.value());
        assertEquals("root", value.defaultValue());
        assertEquals("User for admin tasks", value.description());
    }

    @Test
    void testSetPropertyWithoutDescriptionAndDefaultValue() {
        properties.setProperty("language", new ConfigurationProperties.Value("EN", null, null));

        ConfigurationProperties.Value value = properties.getProperty("language");
        assertNotNull(value);
        assertEquals("EN", value.value());
        assertNull(value.defaultValue());
        assertNull(value.description());
    }

    @Test
    void testOverridePropertyKeepsNewDescriptionAndDefaultValue() {
        properties.setProperty("timeout", new ConfigurationProperties.Value("30", "60", "Timeout in seconds"));
        properties.setProperty("timeout", new ConfigurationProperties.Value("45", "90", "Updated timeout description"));

        ConfigurationProperties.Value value = properties.getProperty("timeout");
        assertNotNull(value);
        assertEquals("45", value.value());
        assertEquals("90", value.defaultValue());
        assertEquals("Updated timeout description", value.description());
    }

    @Test
    void testNullDescriptionAndDefaultValueHandledProperly() {
        properties.setProperty("color", new ConfigurationProperties.Value("blue", null, null));

        ConfigurationProperties.Value value = properties.getProperty("color");
        assertNotNull(value);
        assertEquals("blue", value.value());
        assertNull(value.defaultValue());
        assertNull(value.description());
    }

    @Test
    void testOverrideWithNullValues() {
        properties.setProperty("path", new ConfigurationProperties.Value("/home/user", "File path", "/root"));
        properties.setProperty("path", new ConfigurationProperties.Value("/home/new_user", null, null));

        ConfigurationProperties.Value value = properties.getProperty("path");
        assertNotNull(value);
        assertEquals("/home/new_user", value.value());
        assertNull(value.defaultValue());
        assertNull(value.description());
    }

}
