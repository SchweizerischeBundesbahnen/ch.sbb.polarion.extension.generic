package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.jaxb.TestObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class SettingsModelDeserializationTest {
    public static final String TEST_ARRAY = "TEST_ARRAY";
    public static final String TEST_LIST = "TEST_LIST";
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
    void testDeserializeArrayOfStrings() {
        String serializedArray = "-----BEGIN TEST_ARRAY-----" + System.lineSeparator() +
                "[ \"value1\", \"value2\" ]" + System.lineSeparator() +
                "-----END TEST_ARRAY-----" + System.lineSeparator();
        String[] deserializedArray = model.deserializeEntry(TEST_ARRAY, serializedArray, String[].class);
        Assertions.assertEquals(2, deserializedArray.length);
        Assertions.assertEquals("value1", deserializedArray[0]);
        Assertions.assertEquals("value2", deserializedArray[1]);
    }

    @Test
    void testDeserializeArrayOfInteger() {
        String serializedArray = "-----BEGIN TEST_ARRAY-----" + System.lineSeparator() +
                "[ 1, 2 ]" + System.lineSeparator() +
                "-----END TEST_ARRAY-----" + System.lineSeparator();
        Integer[] deserializedArray = model.deserializeEntry(TEST_ARRAY, serializedArray, Integer[].class);
        Assertions.assertEquals(2, deserializedArray.length);
        Assertions.assertEquals(1, deserializedArray[0]);
        Assertions.assertEquals(2, deserializedArray[1]);
    }

    @Test
    void testDeserializeArrayOfObjects() {
        String serializedList = "-----BEGIN TEST_LIST-----" + System.lineSeparator() +
                "[ " + System.lineSeparator() +
                "{\"testKey\":\"testKey1\",\"testValue\":1}," + System.lineSeparator() +
                "{\"testKey\":\"testKey2\",\"testValue\":2}," + System.lineSeparator() +
                "{\"testKey\":\"testKey3\",\"testValue\":3}" + System.lineSeparator() +
                "]" + System.lineSeparator() +
                "-----END TEST_LIST-----" + System.lineSeparator();
        TestObject[] deserializedArray = model.deserializeEntry(TEST_LIST, serializedList, TestObject[].class);
        Assertions.assertEquals(3, deserializedArray.length);
        Assertions.assertEquals("testKey1", deserializedArray[0].getTestKey());
        Assertions.assertEquals(1, deserializedArray[0].getTestValue());
        Assertions.assertEquals("testKey2", deserializedArray[1].getTestKey());
        Assertions.assertEquals(2, deserializedArray[1].getTestValue());
        Assertions.assertEquals("testKey3", deserializedArray[2].getTestKey());
        Assertions.assertEquals(3, deserializedArray[2].getTestValue());
    }


    @Test
    void testDeserializeListOfStrings() {
        String serializedList = "-----BEGIN TEST_LIST-----" + System.lineSeparator() +
                "[ \"value1\", \"value2\" ]" + System.lineSeparator() +
                "-----END TEST_LIST-----" + System.lineSeparator();
        List<String> deserializedList = model.deserializeListEntry(TEST_LIST, serializedList, String.class);
        Assertions.assertInstanceOf(List.class, deserializedList);
        Assertions.assertEquals(2, deserializedList.size());
        Assertions.assertEquals("value1", deserializedList.get(0));
        Assertions.assertEquals("value2", deserializedList.get(1));
    }

    @Test
    void testDeserializeListOfIntegers() {
        String serializedList = "-----BEGIN TEST_LIST-----" + System.lineSeparator() +
                "[ 1, 2, 3 ]" + System.lineSeparator() +
                "-----END TEST_LIST-----" + System.lineSeparator();
        List<Integer> deserializedList = model.deserializeListEntry(TEST_LIST, serializedList, Integer.class);
        Assertions.assertInstanceOf(List.class, deserializedList);
        Assertions.assertEquals(3, deserializedList.size());
        Assertions.assertEquals(1, deserializedList.get(0));
        Assertions.assertEquals(2, deserializedList.get(1));
        Assertions.assertEquals(3, deserializedList.get(2));
    }

    @Test
    void testDeserializeListOfObjects() {
        String serializedList = "-----BEGIN TEST_LIST-----" + System.lineSeparator() +
                "[ " + System.lineSeparator() +
                "{\"testKey\":\"testKey1\",\"testValue\":1}," + System.lineSeparator() +
                "{\"testKey\":\"testKey2\",\"testValue\":2}," + System.lineSeparator() +
                "{\"testKey\":\"testKey3\",\"testValue\":3}" + System.lineSeparator() +
                "]" + System.lineSeparator() +
                "-----END TEST_LIST-----" + System.lineSeparator();
        List<TestObject> deserializedList = model.deserializeListEntry(TEST_LIST, serializedList, TestObject.class);
        Assertions.assertInstanceOf(List.class, deserializedList);
        for (TestObject testObject : deserializedList) {
            Assertions.assertInstanceOf(TestObject.class, testObject);
        }
        Assertions.assertEquals(3, deserializedList.size());
        Assertions.assertEquals("testKey1", deserializedList.get(0).getTestKey());
        Assertions.assertEquals(1, deserializedList.get(0).getTestValue());
        Assertions.assertEquals("testKey2", deserializedList.get(1).getTestKey());
        Assertions.assertEquals(2, deserializedList.get(1).getTestValue());
        Assertions.assertEquals("testKey3", deserializedList.get(2).getTestKey());
        Assertions.assertEquals(3, deserializedList.get(2).getTestValue());
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
    void testMixedLineSeparators() {
        String serializedString = "-----BEGIN TEST_STRING-----\r\nsomeValue\n-----END TEST_STRING-----";
        final String deserialized = model.deserializeEntry(TEST_STRING, serializedString);
        Assertions.assertInstanceOf(String.class, deserialized);
        Assertions.assertEquals("someValue", deserialized);
    }

    @Test
    void testDeserializeNull() {
        Object deserializedObject = model.deserializeEntry(TEST_STRING, null, Object.class);
        Assertions.assertNull(deserializedObject);
    }

    @Test
    void testDeserializeEmptyString() {
        Object deserializedObject = model.deserializeEntry(TEST_STRING, "", Object.class);
        Assertions.assertNull(deserializedObject);
    }

    @Test
    void testDeserializeEmptyEntryWithDefaultStringValue() {
        String deserializedString = model.deserializeEntry(TEST_OBJECT, null, String.class, "defaultValue");
        Assertions.assertEquals("defaultValue", deserializedString);
    }


    @Test
    void testDeserializeEmptyEntryWithDefaultObjectValue() {
        TestObject defaultValue = new TestObject("testKey", 1);
        final TestObject deserializedObject = model.deserializeEntry(TEST_OBJECT, null, TestObject.class, defaultValue);
        Assertions.assertEquals(defaultValue, deserializedObject);
    }
}
