package ch.sbb.polarion.extension.generic.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedPropertiesTest {

    private ExtendedProperties props;

    @BeforeEach
    void setUp() {
        props = new ExtendedProperties();
    }

    @Test
    void testSetPropertyWithDescriptionAndDefaultValue() {
        props.setProperty("username", "admin", "root", "User for admin tasks");

        assertEquals("admin", props.getProperty("username"));
        assertEquals("User for admin tasks", props.getDescription("username"));
        assertEquals("root", props.getDefaultValue("username"));
    }

    @Test
    void testSetPropertyWithoutDescriptionAndDefaultValue() {
        props.setProperty("language", "EN");

        assertEquals("EN", props.getProperty("language"));
        assertNull(props.getDescription("language"));
        assertNull(props.getDefaultValue("language"));
    }

    @Test
    void testOverridePropertyKeepsNewDescriptionAndDefaultValue() {
        props.setProperty("timeout", "30", "60", "Timeout in seconds");
        props.setProperty("timeout", "45", "90", "Updated timeout description");

        assertEquals("45", props.getProperty("timeout"));
        assertEquals("Updated timeout description", props.getDescription("timeout"));
        assertEquals("90", props.getDefaultValue("timeout"));
    }

    @Test
    void testNullDescriptionAndDefaultValueHandledProperly() {
        props.setProperty("color", "blue", null, null);

        assertEquals("blue", props.getProperty("color"));
        assertNull(props.getDescription("color"));
        assertNull(props.getDefaultValue("color"));
    }

    @Test
    void testPutMethodWithDescriptionAndDefaultValue() {
        props.put("fontSize", "12", "10", "Size of font in points");

        assertEquals("12", props.getProperty("fontSize"));
        assertEquals("Size of font in points", props.getDescription("fontSize"));
        assertEquals("10", props.getDefaultValue("fontSize"));
    }

    @Test
    void testPutMethodWithoutDescriptionAndDefaultValue() {
        props.put("theme", "dark");

        assertEquals("dark", props.getProperty("theme"));
        assertNull(props.getDescription("theme"));
        assertNull(props.getDefaultValue("theme"));
    }

    @Test
    void testOverrideWithNullValues() {
        props.setProperty("path", "/home/user", "File path", "/root");
        props.setProperty("path", "/home/new_user", null, null);

        assertEquals("/home/new_user", props.getProperty("path"));
        assertNull(props.getDescription("path"));
        assertNull(props.getDefaultValue("path"));
    }

}
