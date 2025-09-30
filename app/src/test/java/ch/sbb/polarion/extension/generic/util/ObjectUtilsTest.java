package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilsTest {

    @Test
    void requireNotNull_withNonNullObject_shouldReturnObject() {
        // Arrange
        String expected = "test";

        // Act
        String result = ObjectUtils.requireNotNull(expected, "Message");

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void requireNotNull_withNullObject_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> ObjectUtils.requireNotNull(null, "Message"));
    }

    @Test
    void serialize_withValidObject_shouldReturnByteArray() throws Exception {
        // Arrange
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", 42);

        // Act
        byte[] result = ObjectUtils.serialize(testMap);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void serialize_withString_shouldReturnByteArray() throws Exception {
        // Arrange
        String testString = "Hello World";

        // Act
        byte[] result = ObjectUtils.serialize(testString);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void serialize_withSerializableObject_shouldReturnByteArray() throws Exception {
        // Arrange
        TestSerializableObject testObject = new TestSerializableObject("test", 123);

        // Act
        byte[] result = ObjectUtils.serialize(testObject);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void deserialize_withValidByteArray_shouldReturnMap() throws Exception {
        // Arrange
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("key1", "value1");
        originalMap.put("key2", 42);
        originalMap.put("key3", true);
        byte[] serializedData = ObjectUtils.serialize(originalMap);

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ObjectUtils.deserialize(serializedData);

        // Assert
        assertNotNull(result);
        assertEquals(originalMap.size(), result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals(42, result.get("key2"));
        assertEquals(true, result.get("key3"));
    }

    @Test
    void deserialize_withEmptyMap_shouldReturnEmptyMap() throws Exception {
        // Arrange
        Map<String, Object> emptyMap = new HashMap<>();
        byte[] serializedData = ObjectUtils.serialize(emptyMap);

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ObjectUtils.deserialize(serializedData);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void serialize_deserialize_roundTrip_shouldPreserveData() throws Exception {
        // Arrange
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("string", "test value");
        originalMap.put("integer", 12345);
        originalMap.put("boolean", false);
        originalMap.put("double", 3.14159);

        // Act
        byte[] serialized = ObjectUtils.serialize(originalMap);
        @SuppressWarnings("unchecked")
        Map<String, Object> deserialized = (Map<String, Object>) ObjectUtils.deserialize(serialized);

        // Assert
        assertEquals(originalMap, deserialized);
    }

    private record TestSerializableObject(String name, int value) implements Serializable {

        @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (obj == null || getClass() != obj.getClass()) return false;
                TestSerializableObject that = (TestSerializableObject) obj;
                return value == that.value && name.equals(that.name);
            }

    }
}
