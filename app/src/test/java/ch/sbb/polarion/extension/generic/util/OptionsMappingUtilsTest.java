package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OptionsMappingUtilsTest {

    @Test
    void testGetMappingForFieldId() {
        assertEquals(new HashMap<>(), OptionsMappingUtils.getMappingForFieldId("fieldId", null));
        assertEquals(new HashMap<>(), OptionsMappingUtils.getMappingForFieldId("fieldId", Map.of()));
        assertEquals(new HashMap<>(), OptionsMappingUtils.getMappingForFieldId("fieldId", Map.of("fieldId", new HashMap<>())));
        assertEquals(Map.of("key3", "someValue"), OptionsMappingUtils.getMappingForFieldId("fieldId", Map.of("fieldId", Map.of("key1", "", "key2", "  ", "key3", "someValue"))));
    }

    @Test
    void testGetMappedOptionKey() {
        assertNull(OptionsMappingUtils.getMappedOptionKey("field", null, null));
        assertNull(OptionsMappingUtils.getMappedOptionKey("field", Boolean.TRUE, null));
        assertNull(OptionsMappingUtils.getMappedOptionKey("field", 42, Map.of("field", Map.of("someKey", "42"))));
        assertNull(OptionsMappingUtils.getMappedOptionKey("field", "", null));
        assertNull(OptionsMappingUtils.getMappedOptionKey("field", " ", null));
        assertNull(OptionsMappingUtils.getMappedOptionKey("field", " ", Map.of("field", Map.of())));
        assertNull(OptionsMappingUtils.getMappedOptionKey("field", null, Map.of("field", Map.of("someKey", "someValue"))));
        assertEquals("someKey", OptionsMappingUtils.getMappedOptionKey("field", "someValue", Map.of("field", Map.of("someKey", "someValue"))));
        assertEquals("someKey", OptionsMappingUtils.getMappedOptionKey("field", " someValue ", Map.of("field", Map.of("someKey", "someValue"))));
        assertEquals("someKey", OptionsMappingUtils.getMappedOptionKey("field", "someValue", Map.of("field", Map.of("someKey", " someValue"))));
        assertNull(OptionsMappingUtils.getMappedOptionKey("field", "", Map.of("field", Map.of("someKey", ""))));
        assertEquals("someKey", OptionsMappingUtils.getMappedOptionKey("field", "", Map.of("field", Map.of("someKey", "(empty),someValue"))));
        assertEquals("someKey", OptionsMappingUtils.getMappedOptionKey("field", "   ", Map.of("field", Map.of("someKey", "(empty) ,someValue"))));
        assertEquals("someKey", OptionsMappingUtils.getMappedOptionKey("field", null, Map.of("field", Map.of("someKey", "(empty) ,someValue"))));
    }

}
