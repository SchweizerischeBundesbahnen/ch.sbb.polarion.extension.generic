package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.jaxb.TestObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class SettingsModelDeserializationTest {
    public static final String TEST_MAP = "TEST_MAP";
    public static final String TEST_OBJECT = "TEST_OBJECT";
    public static final String TEST_STRING = "TEST_STRING";

    TestSettingsModel model = new TestSettingsModel();

    @Test
    @SuppressWarnings("rawtypes")
    void testDeserializeMap() {
        String serializedMap = "-----BEGIN TEST_MAP-----" + System.lineSeparator() +
                "{\"key1\":\"value1\",\"key2\":\"value2\"}" + System.lineSeparator() +
                "-----END TEST_MAP-----" + System.lineSeparator();
        Map deserializedMap = model.deserializeEntry(TEST_MAP, serializedMap, Map.class);
        Assertions.assertInstanceOf(Map.class, deserializedMap);
        Assertions.assertEquals(2, deserializedMap.size());
        Assertions.assertEquals("value1", deserializedMap.get("key1"));
        Assertions.assertEquals("value2", deserializedMap.get("key2"));
    }

    @Test
    void testDeserializeObject() {
        String serializedObject = "-----BEGIN TEST_OBJECT-----" + System.lineSeparator() +
                "{\"testKey\":\"testKey\",\"testValue\":1}" + System.lineSeparator() +
                "-----END TEST_OBJECT-----" + System.lineSeparator();
        final TestObject deserializedObject = model.deserializeEntry(TEST_OBJECT, serializedObject, TestObject.class);
        Assertions.assertInstanceOf(TestObject.class, deserializedObject);
        Assertions.assertEquals("testKey", deserializedObject.getTestKey());
        Assertions.assertEquals(1, deserializedObject.getTestValue());
    }

    @Test
    void testDeserializeString() {
        String serializedString = "-----BEGIN TEST_STRING-----" + System.lineSeparator() +
                "key1, value1, key2, value2" + System.lineSeparator() +
                "-----END TEST_STRING-----" + System.lineSeparator();
        final String deserialized = model.deserializeEntry(TEST_STRING, serializedString);
        Assertions.assertInstanceOf(String.class, deserialized);
        Assertions.assertEquals("key1, value1, key2, value2", deserialized);
    }

    @Test
    void testDeserializeNull() {
        Object deserializedObject = model.deserializeEntry(TEST_STRING, null, Object.class);
        Assertions.assertNull(deserializedObject);
    }
}
