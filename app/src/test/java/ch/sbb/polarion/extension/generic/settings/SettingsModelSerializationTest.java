package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.jaxb.TestObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class SettingsModelSerializationTest {

    public static final String TEST_MAP = "TEST_MAP";
    public static final String TEST_OBJECT = "TEST_OBJECT";
    public static final String TEST_STRING = "TEST_STRING";
    TestSettingsModel model = new TestSettingsModel();

    @Test
    void testSerializeMap() {
        Map<String, String> testMap = Map.of("key1", "value1", "key2", "value2");
        final String serialized = model.serializeEntry(TEST_MAP, testMap);
        Assertions.assertInstanceOf(String.class, serialized);
        Assertions.assertTrue(serialized.contains("key1"));
        Assertions.assertTrue(serialized.contains("key2"));
        Assertions.assertTrue(serialized.contains("value1"));
        Assertions.assertTrue(serialized.contains("value2"));
    }

    @Test
    void testSerializeObject() {
        TestObject testObject = new TestObject("testKey", 1);
        final String serialized = model.serializeEntry(TEST_OBJECT, testObject);
        Assertions.assertInstanceOf(String.class, serialized);
        Assertions.assertTrue(serialized.contains("testKey"));
        Assertions.assertTrue(serialized.contains("testValue"));
        Assertions.assertTrue(serialized.contains("1"));
    }

    @Test
    void testSerializeString() {
        String testString = "key1, value1, key2, value2";
        final String serialized = model.serializeEntry(TEST_STRING, testString);
        Assertions.assertInstanceOf(String.class, serialized);
        Assertions.assertTrue(serialized.contains(testString));
    }

    @Test
    void testSerializeNull() {
        final String serialized = model.serializeEntry(TEST_STRING, null);
        Assertions.assertInstanceOf(String.class, serialized);
        Assertions.assertTrue(serialized.isEmpty());
    }

}
