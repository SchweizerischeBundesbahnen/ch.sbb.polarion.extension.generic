package ch.sbb.polarion.extension.generic.jaxb;

import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class JAXBUtilsTest {

    private final String TEST_OBJECT = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + System.lineSeparator() +
            "<TestObject>" + System.lineSeparator() +
            "    <testKey>key</testKey>" + System.lineSeparator() +
            "    <testValue>100</testValue>" + System.lineSeparator() +
            "</TestObject>" + System.lineSeparator() +
            "";

    @Test
    void deserialize() throws JAXBException {
        TestObject testObject = JAXBUtils.deserialize(TestObject.class, TEST_OBJECT);

        assertEquals("key", testObject.getTestKey());
        assertEquals(100, testObject.getTestValue());
        assertNull(JAXBUtils.deserialize(TestObject.class, (String) null));
        assertNull(JAXBUtils.deserialize(TestObject.class, (Reader) null));
        assertNull(JAXBUtils.deserialize(null, mock(Reader.class)));
    }

    @Test
    void serialize() throws JAXBException, IOException {
        TestObject testObject = new TestObject("key", 100);
        String serialized = JAXBUtils.serialize(testObject);

        assertEquals(TEST_OBJECT.replace(System.lineSeparator(), "\n"), serialized);
    }
}