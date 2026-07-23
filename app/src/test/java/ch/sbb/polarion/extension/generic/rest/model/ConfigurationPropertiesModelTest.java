package ch.sbb.polarion.extension.generic.rest.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationPropertiesModelTest {

    private ConfigurationPropertyModel property(String key) {
        return ConfigurationPropertyModel.builder().key(key).build();
    }

    @Test
    void testNoArgsConstructorAndSetters() {
        ConfigurationPropertiesModel model = new ConfigurationPropertiesModel();
        assertNull(model.getProperties());
        assertNull(model.getObsoleteProperties());

        List<ConfigurationPropertyModel> properties = List.of(property("a"));
        List<ConfigurationPropertyModel> obsolete = List.of(property("b"));
        model.setProperties(properties);
        model.setObsoleteProperties(obsolete);

        assertEquals(properties, model.getProperties());
        assertEquals(obsolete, model.getObsoleteProperties());
    }

    @Test
    void testAllArgsConstructor() {
        List<ConfigurationPropertyModel> properties = List.of(property("a"));
        List<ConfigurationPropertyModel> obsolete = List.of(property("b"));

        ConfigurationPropertiesModel model = new ConfigurationPropertiesModel(properties, obsolete);

        assertEquals(properties, model.getProperties());
        assertEquals(obsolete, model.getObsoleteProperties());
    }

    @Test
    void testBuilder() {
        List<ConfigurationPropertyModel> properties = List.of(property("a"));
        List<ConfigurationPropertyModel> obsolete = List.of(property("b"));

        ConfigurationPropertiesModel model = ConfigurationPropertiesModel.builder()
                .properties(properties)
                .obsoleteProperties(obsolete)
                .build();

        assertEquals(properties, model.getProperties());
        assertEquals(obsolete, model.getObsoleteProperties());
    }

    @Test
    void testEqualsAndHashCode() {
        List<ConfigurationPropertyModel> properties = List.of(property("a"));
        List<ConfigurationPropertyModel> obsolete = List.of(property("b"));

        ConfigurationPropertiesModel first = new ConfigurationPropertiesModel(properties, obsolete);
        ConfigurationPropertiesModel second = new ConfigurationPropertiesModel(properties, obsolete);
        ConfigurationPropertiesModel different = new ConfigurationPropertiesModel(properties, null);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

        assertNotEquals(first, different);
        assertNotEquals(null, first);
        assertNotEquals(new Object(), first);
    }

    @Test
    void testToString() {
        ConfigurationPropertiesModel model = ConfigurationPropertiesModel.builder()
                .properties(List.of(property("active.key")))
                .obsoleteProperties(List.of(property("obsolete.key")))
                .build();

        String toString = model.toString();
        assertTrue(toString.contains("active.key"));
        assertTrue(toString.contains("obsolete.key"));
    }
}
